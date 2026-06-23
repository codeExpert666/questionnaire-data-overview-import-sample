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
import java.util.stream.Collectors;

public class QuestionnaireOpinionImportListener
        extends AnalysisEventListener<Map<Integer, String>> {

    private static final int MAX_QUESTIONNAIRE_ID_LENGTH = 128;
    private static final int MAX_PRODUCT_MODEL_LENGTH = 128;
    private static final int MAX_PRODUCT_CODE_LENGTH = 64;
    private static final int MAX_VERSION_LENGTH = 64;
    private static final int MAX_LONG_TEXT_LENGTH = 10_000;
    private static final int MAX_OPINION_TEXT_LENGTH = 5_000;

    private final ImportReferenceData referenceData;
    private final QuestionnaireImportWriter importWriter;
    private final RecommendCategoryResolver categoryResolver;
    private final QuestionnaireImportProperties properties;

    private final Map<Integer, FeatureRef> scoreFeatureByColumn = new LinkedHashMap<>();
    private final Set<String> closedQuestionnaireIds = new HashSet<>();
    private final List<AnswerAggregate> pendingAggregates = new ArrayList<>();
    private final List<ExcelImportError> errors = new ArrayList<>();

    private boolean headerValidated;
    private int dataRowCount;
    private int questionnaireCount;
    private int opinionCount;
    private int featureScoreCount;

    private String currentQuestionnaireId;
    private AnswerAggregate currentAggregate;
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
        flushPendingAggregates();
    }

    public QuestionnaireImportResult getResult() {
        return new QuestionnaireImportResult(
                dataRowCount,
                questionnaireCount,
                opinionCount,
                featureScoreCount);
    }

    private void validateHeader(Map<Integer, String> headMap) {
        List<ExcelImportError> headerErrors = new ArrayList<>();
        int fixedCount = QuestionnaireExcelHeaders.fixedHeaderCount();
        int expectedColumnCount = fixedCount + referenceData.getEnabledFeatures().size();
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

        Set<String> headerFeatureCodes = new HashSet<>();
        for (int index = fixedCount; index < actualColumnCount; index++) {
            String rawHeader = QuestionnaireExcelHeaders.normalizeHeader(headMap.get(index));
            try {
                QuestionnaireExcelHeaders.ParsedFeatureHeader parsed =
                        QuestionnaireExcelHeaders.parseFeatureScoreHeader(rawHeader);
                FeatureRef feature = referenceData.findFeatureByCode(parsed.featureCode());
                if (feature == null) {
                    headerErrors.add(new ExcelImportError(
                            1,
                            rawHeader,
                            "特性编码不存在或已停用：" + parsed.featureCode()));
                    continue;
                }
                if (!feature.getFeatureName().equals(parsed.featureName())) {
                    headerErrors.add(new ExcelImportError(
                            1,
                            rawHeader,
                            "特性名称已变化，当前名称为“" + feature.getFeatureName()
                                    + "”，请重新下载模板"));
                    continue;
                }
                if (!headerFeatureCodes.add(parsed.featureCode())) {
                    headerErrors.add(new ExcelImportError(
                            1,
                            rawHeader,
                            "特性评分列重复：" + parsed.featureCode()));
                    continue;
                }
                scoreFeatureByColumn.put(index, feature);
            } catch (IllegalArgumentException ex) {
                headerErrors.add(new ExcelImportError(1, rawHeader, ex.getMessage()));
            }
        }

        Set<String> enabledCodes = referenceData.getEnabledFeatures().stream()
                .map(FeatureRef::getFeatureCode)
                .collect(Collectors.toSet());
        Set<String> missingCodes = new HashSet<>(enabledCodes);
        missingCodes.removeAll(headerFeatureCodes);
        if (!missingCodes.isEmpty()) {
            headerErrors.add(new ExcelImportError(
                    1,
                    null,
                    "模板缺少启用特性评分列：" + String.join(",", missingCodes)
                            + "，请重新下载模板"));
        }

        if (!headerErrors.isEmpty()) {
            throw new ExcelImportValidationException("导入模板不正确", headerErrors);
        }
    }

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

        String opinionFeatureCode = field("特性分类编码", () -> ExcelCellParser.nullableText(
                row,
                QuestionnaireExcelHeaders.OPINION_FEATURE_CODE,
                "特性分类编码",
                MAX_PRODUCT_CODE_LENGTH));
        Long opinionFeatureId = null;
        if (opinionFeatureCode != null) {
            FeatureRef opinionFeature = referenceData.findFeatureByCode(opinionFeatureCode);
            if (opinionFeature == null) {
                throw new RowFieldValidationException(
                        "特性分类编码",
                        "特性编码不存在或已停用：" + opinionFeatureCode);
            }
            if (!referenceData.productSupportsFeature(product.getId(), opinionFeature.getId())) {
                throw new RowFieldValidationException(
                        "特性分类编码",
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

    private <T> T field(String columnName, Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RowFieldValidationException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            throw new RowFieldValidationException(columnName, ex.getMessage());
        }
    }

    private void addError(Integer rowNumber, String columnName, String message) {
        errors.add(new ExcelImportError(rowNumber, columnName, message));
        if (errors.size() >= properties.getMaxErrors()) {
            throw new ExcelImportValidationException(
                    "导入错误已达到上限" + properties.getMaxErrors() + "条",
                    errors);
        }
    }

    private record ParsedQuestionnaireRow(AnswerSnapshot answer, OpinionSnapshot opinion) {
    }

    private static class RowFieldValidationException extends IllegalArgumentException {
        private final String columnName;

        private RowFieldValidationException(String columnName, String message) {
            super(message);
            this.columnName = columnName;
        }
    }
}
