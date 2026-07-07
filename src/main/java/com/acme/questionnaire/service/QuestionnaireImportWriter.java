package com.acme.questionnaire.service;

import com.acme.questionnaire.exception.QuestionnaireImportException;
import com.acme.questionnaire.mapper.AnswerMapper;
import com.acme.questionnaire.mapper.FeatureScoreMapper;
import com.acme.questionnaire.mapper.OpinionMapper;
import com.acme.questionnaire.model.AnswerAggregate;
import com.acme.questionnaire.model.AnswerSnapshot;
import com.acme.questionnaire.model.OpinionSnapshot;
import com.acme.questionnaire.param.AnswerIdRow;
import com.acme.questionnaire.param.AnswerUpsertParam;
import com.acme.questionnaire.param.FeatureScoreInsertParam;
import com.acme.questionnaire.param.OpinionInsertParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 问卷导入写库组件。
 *
 * <p>导入粒度是 questionnaire_id。再次导入相同问卷时，先 upsert 问卷主表，再整体删除该答卷旧的
 * pq_answer_feature_score 和 pq_opinion 明细，最后写入本次文件解析出的特性评分和观点。</p>
 */
@Component
@RequiredArgsConstructor
public class QuestionnaireImportWriter {
    /**
     * Excel 导入写入 pq_answer.source_system 的固定来源。
     *
     * <p>同一个外部 questionnaire_id 可能来自不同渠道；导入覆盖和主键回查必须同时带上来源，避免
     * 覆盖其他来源的数据。</p>
     */
    public static final String SOURCE_SYSTEM = "EXCEL";
    /** 特性评分单次批量插入大小，控制 SQL 参数数量和单条语句长度。 */
    private static final int SCORE_INSERT_CHUNK_SIZE = 1_000;
    /** 观点文本字段可能较长，使用较小批量避免单条 INSERT 过大。 */
    private static final int OPINION_INSERT_CHUNK_SIZE = 200;

    private final AnswerMapper answerMapper;
    private final FeatureScoreMapper featureScoreMapper;
    private final OpinionMapper opinionMapper;

    /**
     * 批量保存问卷聚合数据。
     *
     * <p>特性评分已经在解析阶段完成 pq_feature 启用状态和产品适用性校验，
     * 这里只负责把 featureId 和 score 写入 pq_answer_feature_score。</p>
     */
    public BatchWriteCount saveBatch(List<AnswerAggregate> aggregates) {
        if (aggregates == null || aggregates.isEmpty()) {
            return new BatchWriteCount(0, 0, 0);
        }

        List<AnswerUpsertParam> answerParams = aggregates.stream()
                .map(AnswerAggregate::getAnswer)
                .map(this::toAnswerParam)
                .toList();
        // 先写入或更新问卷主表，确保后续能用 source_system + questionnaire_id 回查到 answer_id。
        answerMapper.batchUpsert(answerParams);

        List<String> questionnaireIds = answerParams.stream()
                .map(AnswerUpsertParam::getQuestionnaireId)
                .toList();
        List<AnswerIdRow> idRows = answerMapper.selectIdsByQuestionnaireIds(
                SOURCE_SYSTEM, questionnaireIds);
        if (idRows.size() != questionnaireIds.size()) {
            throw new QuestionnaireImportException("答卷写入后回查主键数量不一致");
        }

        // 回查主键后才能整体替换明细；明细表都通过 answer_id 外键关联，不直接保存 questionnaire_id。
        Map<String, Long> answerIdByQuestionnaireId = new HashMap<>(idRows.size() * 2);
        for (AnswerIdRow row : idRows) {
            answerIdByQuestionnaireId.put(row.getQuestionnaireId(), row.getId());
        }

        List<Long> answerIds = idRows.stream().map(AnswerIdRow::getId).toList();
        // 覆盖语义：本次 Excel 是该问卷的完整快照，因此先删除旧评分和旧观点，再插入新明细。
        featureScoreMapper.deleteByAnswerIds(answerIds);
        opinionMapper.deleteByAnswerIds(answerIds);

        List<FeatureScoreInsertParam> scoreParams = new ArrayList<>();
        List<OpinionInsertParam> opinionParams = new ArrayList<>();
        for (AnswerAggregate aggregate : aggregates) {
            Long answerId = answerIdByQuestionnaireId.get(
                    aggregate.getAnswer().getQuestionnaireId());
            if (answerId == null) {
                throw new QuestionnaireImportException(
                        "未找到问卷主键：" + aggregate.getAnswer().getQuestionnaireId());
            }

            aggregate.getAnswer().getFeatureScores().forEach((featureId, score) ->
                    scoreParams.add(new FeatureScoreInsertParam(answerId, featureId, score)));

            int seq = 1;
            for (OpinionSnapshot opinion : aggregate.getOpinions()) {
                opinionParams.add(OpinionInsertParam.builder()
                        .answerId(answerId)
                        .opinionSeq(seq++)
                        .sentimentCode(opinion.getSentiment().getCode())
                        .featureCategoryName(opinion.getFeatureCategoryName())
                        .feedbackContent1(opinion.getFeedbackContent1())
                        .feedbackContent2(opinion.getFeedbackContent2())
                        .build());
            }
        }

        batchInsertScores(scoreParams);
        batchInsertOpinions(opinionParams);
        return new BatchWriteCount(aggregates.size(), opinionParams.size(), scoreParams.size());
    }

    /**
     * 将解析阶段的问卷快照转换为 pq_answer upsert 参数。
     *
     * <p>Excel 中的 productCode/productModel 只用于解析和校验，不直接写入 pq_answer；数据库保存
     * product_id 外键，展示时再关联产品字典。</p>
     */
    private AnswerUpsertParam toAnswerParam(AnswerSnapshot answer) {
        return AnswerUpsertParam.builder()
                .sourceSystem(SOURCE_SYSTEM)
                .questionnaireId(answer.getQuestionnaireId())
                .productId(answer.getProductId())
                .answerTime(answer.getAnswerTime())
                .romVersion(answer.getRomVersion())
                .appVersion(answer.getAppVersion())
                .feedbackText(answer.getFeedbackText())
                .scoreReason(answer.getScoreReason())
                .recommendScore(answer.getRecommendScore())
                .userCategory(answer.getUserCategory().getCode())
                .build();
    }

    /**
     * 分片插入特性评分。
     *
     * <p>空列表表示本批问卷没有任何适用特性的非空评分，允许直接跳过。</p>
     */
    private void batchInsertScores(List<FeatureScoreInsertParam> list) {
        for (int from = 0; from < list.size(); from += SCORE_INSERT_CHUNK_SIZE) {
            int to = Math.min(from + SCORE_INSERT_CHUNK_SIZE, list.size());
            featureScoreMapper.batchInsert(list.subList(from, to));
        }
    }

    /**
     * 分片插入观点明细。
     *
     * <p>opinion_seq 在同一问卷内从 1 开始递增，保持 Excel 连续观点行的展示顺序。</p>
     */
    private void batchInsertOpinions(List<OpinionInsertParam> list) {
        for (int from = 0; from < list.size(); from += OPINION_INSERT_CHUNK_SIZE) {
            int to = Math.min(from + OPINION_INSERT_CHUNK_SIZE, list.size());
            opinionMapper.batchInsert(list.subList(from, to));
        }
    }
}
