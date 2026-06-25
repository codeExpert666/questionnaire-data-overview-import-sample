package com.acme.questionnaire.excel;

import com.acme.questionnaire.config.QuestionnaireImportProperties;
import com.acme.questionnaire.dto.ExcelImportError;
import com.acme.questionnaire.dto.QuestionnaireImportResult;
import com.acme.questionnaire.exception.ExcelImportValidationException;
import com.acme.questionnaire.model.AnswerAggregate;
import com.acme.questionnaire.model.AnswerSnapshot;
import com.acme.questionnaire.model.OpinionSnapshot;
import com.acme.questionnaire.model.Sentiment;
import com.acme.questionnaire.model.UserCategory;
import com.acme.questionnaire.ref.FeatureRef;
import com.acme.questionnaire.ref.ImportReferenceData;
import com.acme.questionnaire.ref.ProductRef;
import com.acme.questionnaire.service.BatchWriteCount;
import com.acme.questionnaire.service.QuestionnaireImportWriter;
import com.acme.questionnaire.service.RecommendCategoryResolver;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 问卷观点 Excel 导入监听器。
 *
 * <p>该监听器按行解析固定问卷字段、观点字段和动态特性评分列。动态评分列由当前启用 pq_feature
 * 决定，具体某个产品是否允许填写该列由 pq_product_feature 决定；观点“特性分类名称”也使用
 * 同一套产品-特性适用关系校验。</p>
 */
public class QuestionnaireOpinionImportListener
        extends AnalysisEventListener<Map<Integer, String>> {

    private static final int MAX_QUESTIONNAIRE_ID_LENGTH = 128;
    private static final int MAX_PRODUCT_MODEL_LENGTH = 128;
    private static final int MAX_PRODUCT_CODE_LENGTH = 64;
    private static final int MAX_FEATURE_NAME_LENGTH = 128;
    private static final int MAX_VERSION_LENGTH = 64;
    private static final int MAX_LONG_TEXT_LENGTH = 10_000;
    private static final int MAX_OPINION_TEXT_LENGTH = 5_000;

    private final ImportReferenceData referenceData;
    private final QuestionnaireImportWriter importWriter;
    private final RecommendCategoryResolver categoryResolver;
    private final QuestionnaireImportProperties properties;

    /**
     * 当前文件中动态评分列到 pq_feature 的映射。
     *
     * <p>导入前先通过表头校验建立该映射，后续逐行读取评分时直接按列定位 featureId。
     * 该映射只说明“列对应哪个启用特性”，不说明产品是否可填；产品适用性在逐行解析时通过
     * pq_product_feature 快照判断。</p>
     */
    private final Map<Integer, FeatureRef> scoreFeatureByColumn = new LinkedHashMap<>();
    /**
     * 已完成读取的问卷 ID 集合。
     *
     * <p>同一问卷允许多行观点，但必须连续出现。关闭一个问卷分组后再遇到相同 ID，说明文件排序
     * 破坏了“按问卷整体覆盖”的导入粒度，需要按行报错。</p>
     */
    private final Set<String> closedQuestionnaireIds = new HashSet<>();
    /**
     * 等待写库的问卷聚合。
     *
     * <p>每个聚合对应一个 questionnaire_id，里面包含一份问卷级快照和多条观点明细。达到批量阈值后
     * 交给写库组件；如果已经出现校验错误，则丢弃暂存数据，等待最终统一抛出异常并回滚事务。</p>
     */
    private final List<AnswerAggregate> pendingAggregates = new ArrayList<>();
    /** 累积的导入错误明细；达到 maxErrors 后会提前中断解析。 */
    private final List<ExcelImportError> errors = new ArrayList<>();

    private boolean headerValidated;
    /** 实际读取到的数据行数，不包含表头和 EasyExcel 忽略的空行。 */
    private int dataRowCount;
    /** 成功写入的问卷数，只有 flushPendingAggregates 成功后才递增。 */
    private int questionnaireCount;
    /** 成功写入的观点明细数。 */
    private int opinionCount;
    /** 成功写入的非空特性评分数。 */
    private int featureScoreCount;

    /** 当前正在聚合的问卷 ID。 */
    private String currentQuestionnaireId;
    /** 当前问卷的聚合对象；第一条有效行会创建问卷级快照，后续行只能追加观点。 */
    private AnswerAggregate currentAggregate;
    /** 当前问卷分组是否已经出现错误；为 true 时关闭分组时不会进入待写队列。 */
    private boolean currentQuestionnaireInvalid;

    public QuestionnaireOpinionImportListener(ImportReferenceData referenceData,
                                              QuestionnaireImportWriter importWriter,
                                              RecommendCategoryResolver categoryResolver,
                                              QuestionnaireImportProperties properties) {
        this.referenceData = referenceData;
        this.importWriter = importWriter;
        this.categoryResolver = categoryResolver;
        this.properties = properties;
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        // 表头校验必须先完成，后续行解析才知道每个动态评分列对应哪个 pq_feature。
        validateHeader(headMap);
        headerValidated = true;
    }

    @Override
    public void invoke(Map<Integer, String> row, AnalysisContext context) {
        int rowNumber = context.readRowHolder().getRowIndex() + 1;
        dataRowCount++;
        if (dataRowCount > properties.getMaxDataRows()) {
            throw ExcelImportValidationException.single(
                    rowNumber,
                    null,
                    "数据行数不能超过" + properties.getMaxDataRows());
        }

        String questionnaireId = ExcelCellParser.cell(
                row, QuestionnaireExcelHeaders.QUESTIONNAIRE_ID);
        if (questionnaireId == null || questionnaireId.isBlank()) {
            addError(rowNumber, "问卷ID", "问卷ID不能为空");
            return;
        }
        // 问卷 ID 变化时关闭前一组；同一问卷的多行观点必须连续，才能做整包覆盖写入。
        switchQuestionnaireIfNecessary(questionnaireId, rowNumber);

        try {
            ParsedQuestionnaireRow parsedRow = parseRow(row, rowNumber);
            if (currentAggregate == null) {
                currentAggregate = new AnswerAggregate(parsedRow.answer());
            } else {
                verifyRepeatedAnswerFields(currentAggregate.getAnswer(), parsedRow.answer());
            }
            currentAggregate.addOpinion(parsedRow.opinion());
        } catch (RowFieldValidationException ex) {
            currentQuestionnaireInvalid = true;
            addError(rowNumber, ex.columnName, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            currentQuestionnaireInvalid = true;
            addError(rowNumber, null, ex.getMessage());
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        if (!headerValidated) {
            addError(1, null, "未读取到有效表头");
        }
        closeCurrentQuestionnaire();
        if (dataRowCount == 0) {
            addError(2, null, "模板中没有可导入的数据");
        }
        if (!errors.isEmpty()) {
            throw new ExcelImportValidationException(
                    "问卷观点表导入失败，共发现" + errors.size() + "个问题",
                    errors);
        }
        // 只有所有行都通过校验时才写入最后一批，保证“任一行失败则整个文件不入库”。
        flushPendingAggregates();
    }

    /**
     * 返回导入结果统计。
     *
     * <p>统计值来自写库组件返回值，而不是单纯读取行数；如果文件存在校验错误，异常会在返回前抛出。</p>
     */
    public QuestionnaireImportResult getResult() {
        return new QuestionnaireImportResult(
                dataRowCount,
                questionnaireCount,
                opinionCount,
                featureScoreCount);
    }

    /**
     * 校验模板表头并建立特性评分列映射。
     *
     * <p>表头必须与当前启用 pq_feature 完全一致：列数一致、固定列表头一致、
     * 动态列按当前启用特性顺序排列，且每个动态列表头为“特性名称体验”。这样可以避免
     * 使用旧模板导入到已变更的特性字典。</p>
     */
    private void validateHeader(Map<Integer, String> headMap) {
        List<ExcelImportError> headerErrors = new ArrayList<>();
        int fixedCount = QuestionnaireExcelHeaders.fixedHeaderCount();
        List<FeatureRef> enabledFeatures = referenceData.getEnabledFeatures();
        int expectedColumnCount = fixedCount + enabledFeatures.size();
        int actualColumnCount = headMap.keySet().stream().max(Integer::compareTo)
                .map(index -> index + 1)
                .orElse(0);

        if (actualColumnCount != expectedColumnCount) {
            headerErrors.add(new ExcelImportError(
                    1,
                    null,
                    "模板列数不匹配，当前应为" + expectedColumnCount
                            + "列，实际为" + actualColumnCount + "列，请重新下载模板"));
        }

        for (int index = 0; index < fixedCount; index++) {
            String expected = QuestionnaireExcelHeaders.FIXED_HEADERS.get(index);
            String actual = QuestionnaireExcelHeaders.normalizeHeader(headMap.get(index));
            if (!expected.equals(actual)) {
                headerErrors.add(new ExcelImportError(
                        1,
                        expected,
                        "第" + (index + 1) + "列表头应为“" + expected
                                + "”，实际为“" + actual + "”"));
            }
        }

        int featureColumnCount = Math.max(actualColumnCount - fixedCount, 0);
        int columnsToValidate = Math.min(featureColumnCount, enabledFeatures.size());
        for (int offset = 0; offset < columnsToValidate; offset++) {
            int index = fixedCount + offset;
            String rawHeader = QuestionnaireExcelHeaders.normalizeHeader(headMap.get(index));
            FeatureRef expectedFeature = enabledFeatures.get(offset);
            String expectedHeader = QuestionnaireExcelHeaders.featureScoreHeader(expectedFeature);
            try {
                QuestionnaireExcelHeaders.ParsedFeatureHeader parsed =
                        QuestionnaireExcelHeaders.parseFeatureScoreHeader(rawHeader);
                if (!expectedFeature.getFeatureName().equals(parsed.featureName())) {
                    headerErrors.add(new ExcelImportError(
                            1,
                            rawHeader,
                            "第" + (index + 1) + "列表头应为“" + expectedHeader
                                    + "”，实际为“" + rawHeader + "”，请重新下载模板"));
                    continue;
                }
                scoreFeatureByColumn.put(index, expectedFeature);
            } catch (IllegalArgumentException ex) {
                headerErrors.add(new ExcelImportError(1, rawHeader, ex.getMessage()));
            }
        }

        if (featureColumnCount < enabledFeatures.size()) {
            List<String> missingHeaders = enabledFeatures.subList(
                            featureColumnCount,
                            enabledFeatures.size())
                    .stream()
                    .map(QuestionnaireExcelHeaders::featureScoreHeader)
                    .toList();
            headerErrors.add(new ExcelImportError(
                    1,
                    null,
                    "模板缺少启用特性评分列：" + String.join(",", missingHeaders)
                            + "，请重新下载模板"));
        }

        if (!headerErrors.isEmpty()) {
            throw new ExcelImportValidationException("导入模板不正确", headerErrors);
        }
    }

    /**
     * 解析一行问卷观点数据。
     *
     * <p>观点所属特性使用固定列“特性分类名称”，可为空；填写时必须是启用特性，
     * 且该产品在 pq_product_feature 中已启用该特性。问卷级特性评分来自动态列，
     * 同样要求产品支持对应特性。</p>
     *
     * <p>产品解析以“产品编码”为稳定键，只接受当前启用的 pq_product；“产品型号”用于
     * 校验用户没有把编码和展示名填串。停用产品不会出现在引用快照中，因此会按编码不存在处理。</p>
     */
    private ParsedQuestionnaireRow parseRow(Map<Integer, String> row, int rowNumber) {
        String questionnaireId = field("问卷ID", () -> ExcelCellParser.requiredText(
                row,
                QuestionnaireExcelHeaders.QUESTIONNAIRE_ID,
                "问卷ID",
                MAX_QUESTIONNAIRE_ID_LENGTH));
        String productModel = field("产品型号", () -> ExcelCellParser.requiredText(
                row,
                QuestionnaireExcelHeaders.PRODUCT_MODEL,
                "产品型号",
                MAX_PRODUCT_MODEL_LENGTH));
        String productCode = field("产品编码", () -> ExcelCellParser.requiredText(
                row,
                QuestionnaireExcelHeaders.PRODUCT_CODE,
                "产品编码",
                MAX_PRODUCT_CODE_LENGTH));

        ProductRef product = referenceData.findProductByCode(productCode);
        if (product == null) {
            throw new RowFieldValidationException("产品编码", "产品编码不存在：" + productCode);
        }
        if (!Objects.equals(product.getProductModel(), productModel)) {
            throw new RowFieldValidationException(
                    "产品型号",
                    "产品型号与产品编码不匹配，编码“" + productCode
                            + "”对应型号为“" + product.getProductModel() + "”");
        }

        LocalDateTime answerTime = field("答卷时间", () -> ExcelCellParser.requiredDateTime(
                row,
                QuestionnaireExcelHeaders.ANSWER_TIME,
                "答卷时间"));
        String romVersion = field("ROM版本", () -> ExcelCellParser.nullableText(
                row,
                QuestionnaireExcelHeaders.ROM_VERSION,
                "ROM版本",
                MAX_VERSION_LENGTH));
        String appVersion = field("App版本", () -> ExcelCellParser.nullableText(
                row,
                QuestionnaireExcelHeaders.APP_VERSION,
                "App版本",
                MAX_VERSION_LENGTH));
        String feedbackText = field("用户反馈与建议", () -> ExcelCellParser.nullableText(
                row,
                QuestionnaireExcelHeaders.FEEDBACK_TEXT,
                "用户反馈与建议",
                MAX_LONG_TEXT_LENGTH));
        String scoreReason = field("打分原因", () -> ExcelCellParser.nullableText(
                row,
                QuestionnaireExcelHeaders.SCORE_REASON,
                "打分原因",
                MAX_LONG_TEXT_LENGTH));
        Integer recommendScore = field("推荐意愿", () -> ExcelCellParser.requiredScore(
                row,
                QuestionnaireExcelHeaders.RECOMMEND_SCORE,
                "推荐意愿"));

        UserCategory expectedCategory = categoryResolver.resolve(recommendScore);
        String categoryText = ExcelCellParser.cell(row, QuestionnaireExcelHeaders.USER_CATEGORY);
        UserCategory importedCategory = field("用户归类", () -> UserCategory.fromText(categoryText));
        if (importedCategory != null && importedCategory != expectedCategory) {
            throw new RowFieldValidationException(
                    "用户归类",
                    "推荐意愿为" + recommendScore + "时，用户归类应为“"
                            + expectedCategory.getDisplayName() + "”");
        }

        Sentiment sentiment = field("情感观点", () -> Sentiment.fromText(
                ExcelCellParser.cell(row, QuestionnaireExcelHeaders.SENTIMENT)));

        String opinionFeatureName = field("特性分类名称", () -> ExcelCellParser.nullableText(
                row,
                QuestionnaireExcelHeaders.OPINION_FEATURE_NAME,
                "特性分类名称",
                MAX_FEATURE_NAME_LENGTH));
        Long opinionFeatureId = null;
        if (opinionFeatureName != null) {
            FeatureRef opinionFeature = referenceData.findFeatureByName(opinionFeatureName);
            if (opinionFeature == null) {
                throw new RowFieldValidationException(
                        "特性分类名称",
                        "特性名称不存在或已停用：" + opinionFeatureName);
            }
            // 观点分类也必须落在当前产品启用的 pq_product_feature 白名单内。
            if (!referenceData.productSupportsFeature(product.getId(), opinionFeature.getId())) {
                throw new RowFieldValidationException(
                        "特性分类名称",
                        "产品“" + productCode + "”未配置特性“"
                                + opinionFeature.getFeatureName() + "”");
            }
            opinionFeatureId = opinionFeature.getId();
        }

        String feedbackContent1 = field("特性具体反馈内容1", () -> ExcelCellParser.nullableText(
                row,
                QuestionnaireExcelHeaders.FEEDBACK_CONTENT_1,
                "特性具体反馈内容1",
                MAX_OPINION_TEXT_LENGTH));
        String feedbackContent2 = field("特性具体反馈内容2", () -> ExcelCellParser.nullableText(
                row,
                QuestionnaireExcelHeaders.FEEDBACK_CONTENT_2,
                "特性具体反馈内容2",
                MAX_OPINION_TEXT_LENGTH));

        Map<Long, Integer> featureScores = parseFeatureScores(row, product);
        AnswerSnapshot answer = AnswerSnapshot.builder()
                .firstRowNumber(rowNumber)
                .questionnaireId(questionnaireId)
                .productId(product.getId())
                .productCode(productCode)
                .productModel(productModel)
                .answerTime(answerTime)
                .romVersion(romVersion == null ? "" : romVersion)
                .appVersion(appVersion == null ? "" : appVersion)
                .feedbackText(feedbackText)
                .scoreReason(scoreReason)
                .recommendScore(recommendScore)
                .userCategory(expectedCategory)
                .featureScores(Map.copyOf(featureScores))
                .build();
        OpinionSnapshot opinion = OpinionSnapshot.builder()
                .rowNumber(rowNumber)
                .sentiment(sentiment)
                .featureId(opinionFeatureId)
                .feedbackContent1(feedbackContent1)
                .feedbackContent2(feedbackContent2)
                .build();
        return new ParsedQuestionnaireRow(answer, opinion);
    }

    /**
     * 解析动态评分列。
     *
     * <p>模板包含所有启用特性，但不同产品只允许填写自身配置的特性评分。
     * 对产品不适用的特性，单元格必须留空；非空值会被拒绝，避免写入无业务含义的
     * pq_answer_feature_score 记录。</p>
     */
    private Map<Long, Integer> parseFeatureScores(Map<Integer, String> row, ProductRef product) {
        Map<Long, Integer> scores = new HashMap<>();
        for (Map.Entry<Integer, FeatureRef> entry : scoreFeatureByColumn.entrySet()) {
            int columnIndex = entry.getKey();
            FeatureRef feature = entry.getValue();
            String columnName = QuestionnaireExcelHeaders.featureScoreHeader(feature);
            Integer score = field(columnName, () -> ExcelCellParser.nullableScore(
                    row, columnIndex, columnName));
            if (score == null) {
                continue;
            }
            // 模板列是全量启用特性；非适用产品只能留空，不能写入无业务含义的评分。
            if (!referenceData.productSupportsFeature(product.getId(), feature.getId())) {
                throw new RowFieldValidationException(
                        columnName,
                        "产品“" + product.getProductCode() + "”不涉及特性“"
                                + feature.getFeatureName() + "”，该列应留空");
            }
            scores.put(feature.getId(), score);
        }
        return scores;
    }

    /**
     * 校验同一问卷多行观点中的问卷级字段一致。
     *
     * <p>特性评分属于问卷级数据，不能在同一 questionnaire_id 的多条观点行中变化。
     * 导入写库时会按问卷整体覆盖旧评分，因此这里提前拒绝同一文件内的冲突数据。</p>
     */
    private void verifyRepeatedAnswerFields(AnswerSnapshot first, AnswerSnapshot current) {
        same("产品编码", first.getProductCode(), current.getProductCode());
        same("产品型号", first.getProductModel(), current.getProductModel());
        same("答卷时间", first.getAnswerTime(), current.getAnswerTime());
        same("ROM版本", first.getRomVersion(), current.getRomVersion());
        same("App版本", first.getAppVersion(), current.getAppVersion());
        same("用户反馈与建议", first.getFeedbackText(), current.getFeedbackText());
        same("打分原因", first.getScoreReason(), current.getScoreReason());
        same("推荐意愿", first.getRecommendScore(), current.getRecommendScore());
        same("用户归类", first.getUserCategory(), current.getUserCategory());

        Set<Long> featureIds = new HashSet<>(first.getFeatureScores().keySet());
        featureIds.addAll(current.getFeatureScores().keySet());
        for (Long featureId : featureIds) {
            Integer firstScore = first.getFeatureScores().get(featureId);
            Integer currentScore = current.getFeatureScores().get(featureId);
            if (!Objects.equals(firstScore, currentScore)) {
                FeatureRef feature = referenceData.findFeatureById(featureId);
                String columnName = feature == null
                        ? "特性评分"
                        : QuestionnaireExcelHeaders.featureScoreHeader(feature);
                throw new RowFieldValidationException(
                        columnName,
                        "同一问卷的重复行中，该特性评分不一致；首次出现在第"
                                + first.getFirstRowNumber() + "行");
            }
        }
    }

    private void same(String columnName, Object first, Object current) {
        if (!Objects.equals(first, current)) {
            throw new RowFieldValidationException(
                    columnName,
                    "同一问卷的重复行中，该字段值不一致");
        }
    }

    /**
     * 根据问卷 ID 切换当前聚合分组。
     *
     * <p>导入文件按行流式读取，不能等全部读取完再排序，因此要求相同 questionnaire_id 连续出现。
     * 该约束让监听器可以在内存中只保留当前分组和有限批量。</p>
     */
    private void switchQuestionnaireIfNecessary(String questionnaireId, int rowNumber) {
        if (currentQuestionnaireId == null) {
            beginQuestionnaire(questionnaireId, rowNumber);
            return;
        }
        if (currentQuestionnaireId.equals(questionnaireId)) {
            return;
        }
        closeCurrentQuestionnaire();
        beginQuestionnaire(questionnaireId, rowNumber);
    }

    /**
     * 开始一个新的问卷聚合分组。
     *
     * <p>如果该问卷 ID 已经关闭过，说明同一问卷被拆散到文件不同位置；继续读取可收集更多错误，
     * 但该分组不会写入数据库。</p>
     */
    private void beginQuestionnaire(String questionnaireId, int rowNumber) {
        currentQuestionnaireId = questionnaireId;
        currentAggregate = null;
        currentQuestionnaireInvalid = false;
        if (closedQuestionnaireIds.contains(questionnaireId)) {
            currentQuestionnaireInvalid = true;
            addError(
                    rowNumber,
                    "问卷ID",
                    "同一问卷的多条观点必须连续排列，问卷ID“"
                            + questionnaireId + "”此前已出现");
        }
    }

    /**
     * 关闭当前问卷分组并决定是否进入待写队列。
     *
     * <p>只有分组内存在有效聚合且未出现错误时才允许写入。批量阈值用于控制内存占用；
     * 写库仍处在 importExcel 外层事务中，后续如果抛出校验或系统异常，已 flush 的批次也会回滚。</p>
     */
    private void closeCurrentQuestionnaire() {
        if (currentQuestionnaireId == null) {
            return;
        }
        closedQuestionnaireIds.add(currentQuestionnaireId);
        if (currentAggregate != null && !currentQuestionnaireInvalid) {
            pendingAggregates.add(currentAggregate);
            if (pendingAggregates.size() >= properties.getAnswerBatchSize()) {
                if (errors.isEmpty()) {
                    flushPendingAggregates();
                } else {
                    pendingAggregates.clear();
                }
            }
        }
        currentQuestionnaireId = null;
        currentAggregate = null;
        currentQuestionnaireInvalid = false;
    }

    /**
     * 将暂存的问卷聚合批量写入数据库。
     *
     * <p>写库组件负责 upsert 主表、清理旧明细并插入本次评分和观点。监听器只合并统计值，
     * 不直接操作 Mapper，保持解析校验与持久化边界清晰。</p>
     */
    private void flushPendingAggregates() {
        if (pendingAggregates.isEmpty()) {
            return;
        }
        BatchWriteCount count = importWriter.saveBatch(List.copyOf(pendingAggregates));
        questionnaireCount += count.questionnaireCount();
        opinionCount += count.opinionCount();
        featureScoreCount += count.featureScoreCount();
        pendingAggregates.clear();
    }

    /**
     * 为单个字段解析补充列名上下文。
     *
     * <p>ExcelCellParser 和枚举转换方法只知道错误原因，不知道业务列名；这里把异常转换为
     * RowFieldValidationException，后续统一写入 ExcelImportError。</p>
     */
    private <T> T field(String columnName, Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RowFieldValidationException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            throw new RowFieldValidationException(columnName, ex.getMessage());
        }
    }

    /**
     * 记录导入错误。
     *
     * <p>达到 maxErrors 后立即抛出业务异常，避免大文件在明显不可导入时继续解析。已经暂存或写入
     * 的批次仍受外层事务保护，最终会随异常整体回滚。</p>
     */
    private void addError(Integer rowNumber, String columnName, String message) {
        errors.add(new ExcelImportError(rowNumber, columnName, message));
        if (errors.size() >= properties.getMaxErrors()) {
            throw new ExcelImportValidationException(
                    "导入错误已达到上限" + properties.getMaxErrors() + "条",
                    errors);
        }
    }

    /**
     * 一行 Excel 数据解析后的结果。
     *
     * <p>answer 是问卷级快照，opinion 是该行观点明细；同一问卷的多行会复用第一行 answer 并追加
     * 多个 opinion。</p>
     */
    private record ParsedQuestionnaireRow(AnswerSnapshot answer, OpinionSnapshot opinion) {
    }

    /**
     * 带列名的行级校验异常。
     *
     * <p>该异常只在监听器内部流转，最终会被转换成 ExcelImportError 暴露给接口调用方。</p>
     */
    private static class RowFieldValidationException extends IllegalArgumentException {
        private final String columnName;

        private RowFieldValidationException(String columnName, String message) {
            super(message);
            this.columnName = columnName;
        }
    }
}
