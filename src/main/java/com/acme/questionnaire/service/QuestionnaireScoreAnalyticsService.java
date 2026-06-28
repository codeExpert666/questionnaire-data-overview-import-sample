package com.acme.questionnaire.service;

import com.acme.questionnaire.dto.AnalyticsFeatureOptionResponse;
import com.acme.questionnaire.dto.AnalyticsProductOptionResponse;
import com.acme.questionnaire.dto.NpsOverviewResponse;
import com.acme.questionnaire.dto.NpsTrendPointResponse;
import com.acme.questionnaire.dto.NpsTrendResponse;
import com.acme.questionnaire.dto.NssFeatureAnalyticsRequest;
import com.acme.questionnaire.dto.NssFeatureScoreResponse;
import com.acme.questionnaire.dto.NssFeatureScoresResponse;
import com.acme.questionnaire.dto.NssTrendAnalyticsRequest;
import com.acme.questionnaire.dto.NssTrendPointResponse;
import com.acme.questionnaire.dto.NssTrendResponse;
import com.acme.questionnaire.dto.RecommendDistributionResponse;
import com.acme.questionnaire.dto.ScoreAnalyticsFilterOptionsResponse;
import com.acme.questionnaire.dto.ScoreAnalyticsGranularity;
import com.acme.questionnaire.dto.ScoreAnalyticsRequest;
import com.acme.questionnaire.exception.QuestionnaireQueryException;
import com.acme.questionnaire.mapper.FeatureMapper;
import com.acme.questionnaire.mapper.QuestionnaireScoreAnalyticsMapper;
import com.acme.questionnaire.model.AnalyticsProductOptionRow;
import com.acme.questionnaire.model.NpsAnalyticsBucketRow;
import com.acme.questionnaire.model.NpsAnalyticsOverviewRow;
import com.acme.questionnaire.model.NssAnalyticsBucketRow;
import com.acme.questionnaire.model.NssFeatureAnalyticsRow;
import com.acme.questionnaire.model.ScoreAnalyticsCriteria;
import com.acme.questionnaire.ref.FeatureRef;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 评分 Tab 可视化分析服务。
 *
 * <p>该服务独立于评分表格查询链路，专门处理图表公共筛选、NPS/NSS 口径、空周期补点和
 * 百分数字段格式化。</p>
 */
@Service
@RequiredArgsConstructor
public class QuestionnaireScoreAnalyticsService {
    private final QuestionnaireScoreAnalyticsMapper analyticsMapper;
    private final FeatureMapper featureMapper;
    private final Clock clock;

    /**
     * 查询 analytics 公共筛选选项。
     *
     * <p>产品、App 版本和 ROM 版本来自已导入答卷，启用特性来自特性字典，避免前端展示
     * 无法参与当前分析的历史停用特性。</p>
     */
    public ScoreAnalyticsFilterOptionsResponse queryFilterOptions() {
        List<AnalyticsProductOptionResponse> products = safeList(analyticsMapper.selectProductOptions()).stream()
                .map(this::toProductOption)
                .toList();
        List<AnalyticsFeatureOptionResponse> features = safeList(featureMapper.selectEnabledFeatures()).stream()
                .map(this::toFeatureOption)
                .toList();
        return new ScoreAnalyticsFilterOptionsResponse(
                products,
                safeList(analyticsMapper.selectAppVersionOptions()),
                safeList(analyticsMapper.selectRomVersionOptions()),
                features);
    }

    /**
     * 查询 NPS 总览。
     *
     * <p>NPS 分母使用推荐者、中立者、贬损者三类数量之和；当前导入链路要求推荐意愿必填，
     * 因此不再返回未知分类。</p>
     */
    public NpsOverviewResponse queryNpsOverview(ScoreAnalyticsRequest request) {
        ScoreAnalyticsCriteria criteria = normalizeCriteria(
                request == null ? null : request.startDate(),
                request == null ? null : request.endDate(),
                request == null ? null : request.productModels(),
                request == null ? null : request.appVersions(),
                request == null ? null : request.romVersions());
        NpsAnalyticsOverviewRow row = analyticsMapper.selectNpsOverview(criteria);
        long promoters = count(row == null ? null : row.getPromoterCount());
        long passives = count(row == null ? null : row.getPassiveCount());
        long detractors = count(row == null ? null : row.getDetractorCount());
        long denominator = promoters + passives + detractors;
        return new NpsOverviewResponse(
                percent(promoters - detractors, denominator),
                oneDecimal(row == null ? null : row.getAverageRecommendScore()),
                new RecommendDistributionResponse(promoters, passives, detractors));
    }

    /**
     * 查询 NPS 趋势并补齐空周期。
     *
     * <p>Mapper 只返回有数据的周期；服务层按请求起止日期和粒度补点，并在补点后维护
     * 从起始日期到当前周期的累计推荐者、中立者、贬损者数量。</p>
     */
    public NpsTrendResponse queryNpsTrend(ScoreAnalyticsRequest request) {
        ScoreAnalyticsCriteria criteria = normalizeCriteria(
                request == null ? null : request.startDate(),
                request == null ? null : request.endDate(),
                request == null ? null : request.productModels(),
                request == null ? null : request.appVersions(),
                request == null ? null : request.romVersions());
        ScoreAnalyticsGranularity granularity = normalizeGranularity(request == null ? null : request.granularity());
        Map<LocalDate, NpsAnalyticsBucketRow> rowByBucket = safeList(analyticsMapper.selectNpsTrend(
                        criteria,
                        granularity.bucketExpression())).stream()
                .filter(row -> row.getPeriodStartDate() != null)
                .collect(Collectors.toMap(
                        NpsAnalyticsBucketRow::getPeriodStartDate,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));

        List<NpsTrendPointResponse> points = new ArrayList<>();
        long cumulativePromoters = 0;
        long cumulativePassives = 0;
        long cumulativeDetractors = 0;
        LocalDate current = granularity.bucketStart(criteria.getStartDate());
        LocalDate end = granularity.bucketStart(criteria.getEndDate());
        while (!current.isAfter(end)) {
            NpsAnalyticsBucketRow row = rowByBucket.get(current);
            long promoters = count(row == null ? null : row.getPromoterCount());
            long passives = count(row == null ? null : row.getPassiveCount());
            long detractors = count(row == null ? null : row.getDetractorCount());
            long denominator = promoters + passives + detractors;
            cumulativePromoters += promoters;
            cumulativePassives += passives;
            cumulativeDetractors += detractors;
            long cumulativeDenominator = cumulativePromoters + cumulativePassives + cumulativeDetractors;
            points.add(new NpsTrendPointResponse(
                    current,
                    count(row == null ? null : row.getQuestionnaireCount()),
                    percent(promoters - detractors, denominator),
                    percent(cumulativePromoters - cumulativeDetractors, cumulativeDenominator)));
            current = granularity.nextBucket(current);
        }
        return new NpsTrendResponse(points);
    }

    /**
     * 查询所有启用特性的 NSS 得分。
     *
     * <p>未传 sortDirection 时保持特性启用顺序；升降序排序时，没有有效评分样本的特性
     * 始终排在末尾。</p>
     */
    public NssFeatureScoresResponse queryNssFeatures(NssFeatureAnalyticsRequest request) {
        ScoreAnalyticsCriteria criteria = normalizeCriteria(
                request == null ? null : request.startDate(),
                request == null ? null : request.endDate(),
                request == null ? null : request.productModels(),
                request == null ? null : request.appVersions(),
                request == null ? null : request.romVersions());
        String sortDirection = normalizeOptionalSortDirection(request == null ? null : request.sortDirection());
        List<NssFeatureScoreResponse> features = safeList(analyticsMapper.selectNssFeatureScores(criteria)).stream()
                .map(this::toNssFeatureScore)
                .collect(Collectors.toCollection(ArrayList::new));
        if (sortDirection != null) {
            features.sort((left, right) -> compareScores(left.nssScore(), right.nssScore(), sortDirection));
        }
        return new NssFeatureScoresResponse(features);
    }

    /**
     * 查询单个启用特性的 NSS 趋势。
     *
     * <p>featureId 必须来自当前启用特性列表；停用或不存在的特性会返回统一查询错误。</p>
     */
    public NssTrendResponse queryNssTrend(NssTrendAnalyticsRequest request) {
        Long featureId = request == null ? null : request.featureId();
        FeatureRef feature = enabledFeatureById().get(featureId);
        if (featureId == null || featureId <= 0) {
            throw QuestionnaireQueryException.invalid("NSS趋势必须指定有效特性ID");
        }
        if (feature == null) {
            throw QuestionnaireQueryException.featureNotFound(featureId);
        }

        ScoreAnalyticsCriteria criteria = normalizeCriteria(
                request.startDate(),
                request.endDate(),
                request.productModels(),
                request.appVersions(),
                request.romVersions());
        ScoreAnalyticsGranularity granularity = normalizeGranularity(request.granularity());
        Map<LocalDate, NssAnalyticsBucketRow> rowByBucket = safeList(analyticsMapper.selectNssTrend(
                        criteria,
                        featureId,
                        granularity.bucketExpression())).stream()
                .filter(row -> row.getPeriodStartDate() != null)
                .collect(Collectors.toMap(
                        NssAnalyticsBucketRow::getPeriodStartDate,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));

        List<NssTrendPointResponse> points = new ArrayList<>();
        long cumulativePromoters = 0;
        long cumulativePassives = 0;
        long cumulativeDetractors = 0;
        LocalDate current = granularity.bucketStart(criteria.getStartDate());
        LocalDate end = granularity.bucketStart(criteria.getEndDate());
        while (!current.isAfter(end)) {
            NssAnalyticsBucketRow row = rowByBucket.get(current);
            long promoters = count(row == null ? null : row.getPromoterCount());
            long passives = count(row == null ? null : row.getPassiveCount());
            long detractors = count(row == null ? null : row.getDetractorCount());
            long denominator = promoters + passives + detractors;
            cumulativePromoters += promoters;
            cumulativePassives += passives;
            cumulativeDetractors += detractors;
            long cumulativeDenominator = cumulativePromoters + cumulativePassives + cumulativeDetractors;
            points.add(new NssTrendPointResponse(
                    current,
                    count(row == null ? null : row.getQuestionnaireCount()),
                    percent(promoters - detractors, denominator),
                    percent(cumulativePromoters - cumulativeDetractors, cumulativeDenominator)));
            current = granularity.nextBucket(current);
        }
        return new NssTrendResponse(
                feature.getId(),
                feature.getFeatureCode(),
                feature.getFeatureName(),
                points);
    }

    /**
     * 规范化 analytics 公共筛选条件。
     *
     * <p>日期默认近三月，结束日期转换为次日零点开区间；多选字符串会 trim、去空和去重。</p>
     */
    private ScoreAnalyticsCriteria normalizeCriteria(LocalDate requestedStartDate,
                                                     LocalDate requestedEndDate,
                                                     List<String> productModels,
                                                     List<String> appVersions,
                                                     List<String> romVersions) {
        LocalDate endDate = requestedEndDate == null ? LocalDate.now(clock) : requestedEndDate;
        LocalDate startDate = requestedStartDate == null ? endDate.minusMonths(3) : requestedStartDate;
        if (startDate.isAfter(endDate)) {
            throw QuestionnaireQueryException.invalid("数据周期开始日期不能晚于结束日期");
        }
        return ScoreAnalyticsCriteria.builder()
                .startDate(startDate)
                .endDate(endDate)
                .startTimeInclusive(startDate.atStartOfDay())
                .endTimeExclusive(endDate.plusDays(1).atStartOfDay())
                .productModels(normalizeTextList(productModels))
                .appVersions(normalizeTextList(appVersions))
                .romVersions(normalizeTextList(romVersions))
                .build();
    }

    private ScoreAnalyticsGranularity normalizeGranularity(ScoreAnalyticsGranularity granularity) {
        return granularity == null ? ScoreAnalyticsGranularity.DAY : granularity;
    }

    private String normalizeOptionalSortDirection(String direction) {
        String normalized = normalizeText(direction);
        if (normalized == null) {
            return null;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (!"ASC".equals(upper) && !"DESC".equals(upper)) {
            throw QuestionnaireQueryException.invalid("NSS评分排序方向只支持 asc 或 desc");
        }
        return upper;
    }

    private List<String> normalizeTextList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String text = normalizeText(value);
            if (text != null) {
                normalized.add(text);
            }
        }
        return List.copyOf(normalized);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private AnalyticsProductOptionResponse toProductOption(AnalyticsProductOptionRow row) {
        return new AnalyticsProductOptionResponse(
                row.getProductId(),
                row.getProductCode(),
                row.getProductModel());
    }

    private AnalyticsFeatureOptionResponse toFeatureOption(FeatureRef feature) {
        return new AnalyticsFeatureOptionResponse(
                feature.getId(),
                feature.getFeatureCode(),
                feature.getFeatureName());
    }

    private NssFeatureScoreResponse toNssFeatureScore(NssFeatureAnalyticsRow row) {
        long denominator = count(row.getScoreCount());
        return new NssFeatureScoreResponse(
                row.getFeatureId(),
                row.getFeatureCode(),
                row.getFeatureName(),
                row.getCreatedAt(),
                percent(count(row.getPromoterCount()) - count(row.getDetractorCount()), denominator));
    }

    private Map<Long, FeatureRef> enabledFeatureById() {
        return safeList(featureMapper.selectEnabledFeatures()).stream()
                .filter(feature -> feature.getId() != null)
                .collect(Collectors.toMap(
                        FeatureRef::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private int compareScores(BigDecimal left, BigDecimal right, String sortDirection) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        int comparison = left.compareTo(right);
        return "DESC".equals(sortDirection) ? -comparison : comparison;
    }

    /**
     * 统一格式化百分数。
     *
     * <p>结果单位是百分数，最多保留一位小数；小数部分为 0 时返回整数 scale 的 BigDecimal，
     * 保证 JSON 中输出 25 而不是 25.0。</p>
     */
    private BigDecimal percent(long numerator, long denominator) {
        if (denominator == 0) {
            return null;
        }
        return trimIntegerScale(BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP));
    }

    private BigDecimal oneDecimal(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return trimIntegerScale(value.setScale(1, RoundingMode.HALF_UP));
    }

    private BigDecimal trimIntegerScale(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() < 0) {
            return stripped.setScale(0, RoundingMode.UNNECESSARY);
        }
        return stripped;
    }

    private long count(Long value) {
        return value == null ? 0 : value;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
