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

@Component
@RequiredArgsConstructor
public class QuestionnaireImportWriter {
    public static final String SOURCE_SYSTEM = "EXCEL";
    private static final int SCORE_INSERT_CHUNK_SIZE = 1_000;
    private static final int OPINION_INSERT_CHUNK_SIZE = 200;

    private final AnswerMapper answerMapper;
    private final FeatureScoreMapper featureScoreMapper;
    private final OpinionMapper opinionMapper;

    public BatchWriteCount saveBatch(List<AnswerAggregate> aggregates) {
        if (aggregates == null || aggregates.isEmpty()) {
            return new BatchWriteCount(0, 0, 0);
        }

        List<AnswerUpsertParam> answerParams = aggregates.stream()
                .map(AnswerAggregate::getAnswer)
                .map(this::toAnswerParam)
                .toList();
        answerMapper.batchUpsert(answerParams);

        List<String> questionnaireIds = answerParams.stream()
                .map(AnswerUpsertParam::getQuestionnaireId)
                .toList();
        List<AnswerIdRow> idRows = answerMapper.selectIdsByQuestionnaireIds(
                SOURCE_SYSTEM, questionnaireIds);
        if (idRows.size() != questionnaireIds.size()) {
            throw new QuestionnaireImportException("答卷写入后回查主键数量不一致");
        }

        Map<String, Long> answerIdByQuestionnaireId = new HashMap<>(idRows.size() * 2);
        for (AnswerIdRow row : idRows) {
            answerIdByQuestionnaireId.put(row.getQuestionnaireId(), row.getId());
        }

        List<Long> answerIds = idRows.stream().map(AnswerIdRow::getId).toList();
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
                        .featureId(opinion.getFeatureId())
                        .feedbackContent1(opinion.getFeedbackContent1())
                        .feedbackContent2(opinion.getFeedbackContent2())
                        .build());
            }
        }

        batchInsertScores(scoreParams);
        batchInsertOpinions(opinionParams);
        return new BatchWriteCount(aggregates.size(), opinionParams.size(), scoreParams.size());
    }

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

    private void batchInsertScores(List<FeatureScoreInsertParam> list) {
        for (int from = 0; from < list.size(); from += SCORE_INSERT_CHUNK_SIZE) {
            int to = Math.min(from + SCORE_INSERT_CHUNK_SIZE, list.size());
            featureScoreMapper.batchInsert(list.subList(from, to));
        }
    }

    private void batchInsertOpinions(List<OpinionInsertParam> list) {
        for (int from = 0; from < list.size(); from += OPINION_INSERT_CHUNK_SIZE) {
            int to = Math.min(from + OPINION_INSERT_CHUNK_SIZE, list.size());
            opinionMapper.batchInsert(list.subList(from, to));
        }
    }
}
