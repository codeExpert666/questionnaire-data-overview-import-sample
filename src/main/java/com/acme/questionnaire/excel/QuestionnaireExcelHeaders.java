package com.acme.questionnaire.excel;

import com.acme.questionnaire.ref.FeatureRef;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QuestionnaireExcelHeaders {
    public static final String DATA_SHEET_NAME = "问卷观点导入";
    public static final String INSTRUCTION_SHEET_NAME = "填写说明";
    public static final String FEATURE_DICTIONARY_SHEET_NAME = "特性字典";
    public static final int EXCEL_MAX_COLUMN_COUNT = 16_384;

    public static final int QUESTIONNAIRE_ID = 0;
    public static final int PRODUCT_MODEL = 1;
    public static final int PRODUCT_CODE = 2;
    public static final int ANSWER_TIME = 3;
    public static final int ROM_VERSION = 4;
    public static final int APP_VERSION = 5;
    public static final int FEEDBACK_TEXT = 6;
    public static final int SCORE_REASON = 7;
    public static final int RECOMMEND_SCORE = 8;
    public static final int USER_CATEGORY = 9;
    public static final int SENTIMENT = 10;
    public static final int OPINION_FEATURE_CODE = 11;
    public static final int FEEDBACK_CONTENT_1 = 12;
    public static final int FEEDBACK_CONTENT_2 = 13;

    public static final List<String> FIXED_HEADERS = List.of(
            "问卷ID",
            "产品型号",
            "产品编码",
            "答卷时间",
            "ROM版本",
            "App版本",
            "用户反馈与建议",
            "打分原因",
            "推荐意愿",
            "用户归类",
            "情感观点",
            "特性分类编码",
            "特性具体反馈内容1",
            "特性具体反馈内容2"
    );

    private static final Pattern FEATURE_SCORE_HEADER_PATTERN =
            Pattern.compile("^特性评分\\[([A-Za-z0-9_.-]{1,64})](.+)$");

    private QuestionnaireExcelHeaders() {
    }

    public static int fixedHeaderCount() {
        return FIXED_HEADERS.size();
    }

    public static String featureScoreHeader(FeatureRef feature) {
        return "特性评分[" + feature.getFeatureCode() + "]" + feature.getFeatureName();
    }

    public static List<List<String>> buildHead(List<FeatureRef> features) {
        if (FIXED_HEADERS.size() + features.size() > EXCEL_MAX_COLUMN_COUNT) {
            throw new IllegalStateException("启用特性数量超过 Excel 最大列数限制");
        }
        List<List<String>> head = new ArrayList<>(FIXED_HEADERS.size() + features.size());
        for (String fixedHeader : FIXED_HEADERS) {
            head.add(List.of(fixedHeader));
        }
        for (FeatureRef feature : features) {
            head.add(List.of(featureScoreHeader(feature)));
        }
        return head;
    }

    public static ParsedFeatureHeader parseFeatureScoreHeader(String header) {
        Matcher matcher = FEATURE_SCORE_HEADER_PATTERN.matcher(normalizeHeader(header));
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "特性评分列格式错误，应为：特性评分[特性编码]特性名称");
        }
        return new ParsedFeatureHeader(matcher.group(1), matcher.group(2).trim());
    }

    public static String normalizeHeader(String value) {
        return value == null ? "" : value.replace("\r", "")
                .replace("\n", "")
                .trim();
    }

    public record ParsedFeatureHeader(String featureCode, String featureName) {
    }
}
