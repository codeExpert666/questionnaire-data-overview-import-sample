package com.acme.questionnaire.excel;

import com.acme.questionnaire.ref.FeatureRef;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 问卷导入模板的表头规范。
 *
 * <p>模板由固定列和 pq_feature 动态评分列组成。固定列描述问卷、观点和观点所属特性；
 * 动态评分列描述该问卷对每个启用特性的打分。动态列格式固定为
 * “特性评分[featureCode]featureName”，导入时会同时校验编码存在、特性仍启用、
 * 名称未变化且列未重复。</p>
 */
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

    /**
     * 构造单个 pq_feature 的动态评分列表头。
     *
     * <p>方括号中的 featureCode 用于机器解析，尾部 featureName 用于人工识别和旧模板检测。</p>
     */
    public static String featureScoreHeader(FeatureRef feature) {
        return "特性评分[" + feature.getFeatureCode() + "]" + feature.getFeatureName();
    }

    /**
     * 构造数据工作表表头。
     *
     * <p>所有启用特性都会进入模板，不按产品裁剪；具体某个产品是否可以填写某个特性评分，
     * 在导入时通过 pq_product_feature 校验。</p>
     */
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

    /**
     * 解析动态评分列表头。
     *
     * <p>只负责拆分编码和名称；编码是否存在、特性是否启用、名称是否仍匹配由导入监听器结合
     * 当前 pq_feature 引用数据判断。</p>
     */
    public static ParsedFeatureHeader parseFeatureScoreHeader(String header) {
        Matcher matcher = FEATURE_SCORE_HEADER_PATTERN.matcher(normalizeHeader(header));
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "特性评分列格式错误，应为：特性评分[特性编码]特性名称");
        }
        return new ParsedFeatureHeader(matcher.group(1), matcher.group(2).trim());
    }

    /**
     * 规范化用户可能手工调整过的表头文本。
     *
     * <p>去掉换行和首尾空白，避免 Excel 自动换行影响固定列和动态列匹配。</p>
     */
    public static String normalizeHeader(String value) {
        return value == null ? "" : value.replace("\r", "")
                .replace("\n", "")
                .trim();
    }

    /**
     * 从动态评分列表头中解析出的 pq_feature 标识。
     *
     * @param featureCode 方括号中的稳定编码
     * @param featureName 编码后的展示名称
     */
    public record ParsedFeatureHeader(String featureCode, String featureName) {
    }
}
