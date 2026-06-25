package com.acme.questionnaire.excel;

import com.acme.questionnaire.ref.FeatureRef;

import java.util.ArrayList;
import java.util.List;

/**
 * 问卷导入模板的表头规范。
 *
 * <p>模板由固定列和 pq_feature 动态评分列组成。固定列描述问卷、观点和观点所属特性；
 * 动态评分列描述该问卷对每个启用特性的打分。动态列格式固定为“特性名称体验”，
 * 例如“音质音效体验”；导入时会按当前启用特性的顺序和名称校验模板是否仍匹配。</p>
 *
 * <p>该类是模板下载和导入表头校验共同依赖的单一规范来源。新增、删除或调整固定列时，
 * 必须同步更新列索引常量、FIXED_HEADERS 顺序、模板样式配置和导入监听器中的行级解析逻辑。
 * 动态评分列只能追加在固定列之后，不能插入固定列中间。</p>
 */
public final class QuestionnaireExcelHeaders {
    /** 导入数据页名称；导入流程仍按 sheet index=0 读取，名称用于用户识别。 */
    public static final String DATA_SHEET_NAME = "问卷观点导入";
    /** 填表规则说明页名称；该页不参与程序解析。 */
    public static final String INSTRUCTION_SHEET_NAME = "填写说明";
    /** 当前启用产品字典页名称；供用户复制产品编码和型号。 */
    public static final String PRODUCT_DICTIONARY_SHEET_NAME = "产品字典";
    /** 当前启用特性字典页名称；供用户复制特性分类编码。 */
    public static final String FEATURE_DICTIONARY_SHEET_NAME = "特性字典";
    /** Excel xlsx 的最大列数，用于防止启用特性过多导致模板无法打开。 */
    public static final int EXCEL_MAX_COLUMN_COUNT = 16_384;

    /**
     * 下列索引均为 0-based Excel 列索引，与 FIXED_HEADERS 的顺序一一对应。
     *
     * <p>导入监听器、模板列宽、单元格格式和数据验证都引用这些常量；维护时不要使用散落的数字，
     * 否则固定列表头顺序变更时容易出现样式和解析错位。</p>
     */
    public static final int QUESTIONNAIRE_ID = 0;
    /**
     * 产品固定列成对出现：导入以产品编码匹配 pq_product，产品型号用于校验人工可读名称。
     */
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

    /**
     * 数据页固定表头。
     *
     * <p>列表顺序就是 Excel 中的固定列顺序。导入时会逐列精确比对这些名称，表头被用户改名、
     * 删除、插入或调换顺序都会被判定为旧模板或错误模板。</p>
     */
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

    /** 动态评分列表头后缀；业务要求列名固定为“特性名称 + 体验”。 */
    private static final String FEATURE_SCORE_HEADER_SUFFIX = "体验";

    private QuestionnaireExcelHeaders() {
    }

    public static int fixedHeaderCount() {
        return FIXED_HEADERS.size();
    }

    /**
     * 构造单个 pq_feature 的动态评分列表头。
     *
     * <p>列名面向填表人展示，不再包含 featureCode。导入阶段会按当前启用特性的排序位置
     * 和 featureName 校验表头；名称或排序变化后，用户持有的旧模板会被拒绝并提示重新下载。</p>
     */
    public static String featureScoreHeader(FeatureRef feature) {
        return feature.getFeatureName() + FEATURE_SCORE_HEADER_SUFFIX;
    }

    /**
     * 构造数据工作表表头。
     *
     * <p>所有启用特性都会进入模板，不按产品裁剪；具体某个产品是否可以填写某个特性评分，
     * 在导入时通过 pq_product_feature 校验。</p>
     *
     * <p>返回值使用 EasyExcel 要求的多级表头结构。当前每列只有一级表头，因此每个表头都是
     * 单元素 List；不要为了美观改成字符串列表，否则写出 API 的语义会变化。</p>
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
     * <p>只负责从“特性名称体验”中拆出特性名称；特性是否存在、是否启用、排序是否仍匹配
     * 由导入监听器结合当前 pq_feature 引用数据判断。</p>
     *
     * <p>该方法会先调用 normalizeHeader，允许用户表头中存在 Excel 自动换行，但不会放宽
     * “体验”后缀要求。</p>
     */
    public static ParsedFeatureHeader parseFeatureScoreHeader(String header) {
        String normalized = normalizeHeader(header);
        if (!normalized.endsWith(FEATURE_SCORE_HEADER_SUFFIX)
                || normalized.length() <= FEATURE_SCORE_HEADER_SUFFIX.length()) {
            throw new IllegalArgumentException(
                    "特性评分列格式错误，应为：特性名称体验，例如“音质音效体验”");
        }
        String featureName = normalized.substring(
                0,
                normalized.length() - FEATURE_SCORE_HEADER_SUFFIX.length()).trim();
        if (featureName.isEmpty()) {
            throw new IllegalArgumentException(
                    "特性评分列格式错误，应为：特性名称体验，例如“音质音效体验”");
        }
        return new ParsedFeatureHeader(featureName);
    }

    /**
     * 规范化用户可能手工调整过的表头文本。
     *
     * <p>去掉换行和首尾空白，避免 Excel 自动换行影响固定列和动态列匹配。这里不移除中间空格，
     * 因为中间空格属于用户真实修改，应该继续触发表头不匹配。</p>
     */
    public static String normalizeHeader(String value) {
        return value == null ? "" : value.replace("\r", "")
                .replace("\n", "")
                .trim();
    }

    /**
     * 从动态评分列表头中解析出的 pq_feature 标识。
     *
     * @param featureName 表头“体验”后缀前的特性名称
     */
    public record ParsedFeatureHeader(String featureName) {
    }
}
