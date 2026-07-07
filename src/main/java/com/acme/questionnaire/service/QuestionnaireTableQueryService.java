package com.acme.questionnaire.service;

import com.acme.questionnaire.dto.DataOverviewRowResponse;
import com.acme.questionnaire.dto.FeatureScoreFilterRequest;
import com.acme.questionnaire.dto.OpinionRowResponse;
import com.acme.questionnaire.dto.ScoreRowResponse;
import com.acme.questionnaire.dto.TableColumnResponse;
import com.acme.questionnaire.dto.TablePageResponse;
import com.acme.questionnaire.dto.TableQueryFilterRequest;
import com.acme.questionnaire.dto.TableQueryRequest;
import com.acme.questionnaire.dto.TableSortRequest;
import com.acme.questionnaire.exception.QuestionnaireQueryException;
import com.acme.questionnaire.excel.QuestionnaireExcelHeaders;
import com.acme.questionnaire.mapper.FeatureMapper;
import com.acme.questionnaire.mapper.QuestionnaireTableQueryMapper;
import com.acme.questionnaire.model.DataOverviewQueryRow;
import com.acme.questionnaire.model.FeatureScoreCell;
import com.acme.questionnaire.model.FeatureScoreFilterCriteria;
import com.acme.questionnaire.model.FeatureScoreSortClause;
import com.acme.questionnaire.model.OpinionQueryRow;
import com.acme.questionnaire.model.ScoreQueryRow;
import com.acme.questionnaire.model.Sentiment;
import com.acme.questionnaire.model.TableOrderClause;
import com.acme.questionnaire.model.TableQueryCriteria;
import com.acme.questionnaire.model.UserCategory;
import com.acme.questionnaire.ref.FeatureRef;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 问卷展示页查询服务。
 *
 * <p>该服务同时支撑数据概览、评分页和观点页。三个接口共享分页、固定字段过滤、
 * 动态特性评分过滤、多列排序和列元数据生成能力，但通过 QueryType 约束各自的行粒度和
 * 可用字段，避免评分页误用观点粒度条件或观点页误用特性评分条件。</p>
 *
 * <p>评分页的核心约定是一份问卷一行：主查询只查询 pq_answer + pq_product 的基础字段，
 * 动态特性评分在分页后按 answerId 批量补齐。这样既保留动态列展示能力，也避免评分明细
 * 一对多关系破坏分页总数和行数。</p>
 */
@Service
@RequiredArgsConstructor
public class QuestionnaireTableQueryService {
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;
    private static final String FEATURE_SCORE_PREFIX = "featureScore:";

    private static final Set<Integer> USER_CATEGORY_CODES = Arrays.stream(UserCategory.values())
            .map(UserCategory::getCode)
            .collect(Collectors.toUnmodifiableSet());
    private static final Set<Integer> SENTIMENT_CODES = Arrays.stream(Sentiment.values())
            .map(Sentiment::getCode)
            .collect(Collectors.toUnmodifiableSet());

    private final QuestionnaireTableQueryMapper queryMapper;
    private final FeatureMapper featureMapper;

    public TablePageResponse<DataOverviewRowResponse> queryDataOverview(TableQueryRequest request) {
        QueryContext context = normalize(request, QueryType.DATA_OVERVIEW);
        long total = queryMapper.countDataOverview(context.criteria());
        List<DataOverviewRowResponse> rows = List.of();
        if (total > 0) {
            List<DataOverviewQueryRow> queryRows = queryMapper.selectDataOverviewRows(
                    context.criteria(),
                    context.orderClauses(),
                    context.featureScoreSorts(),
                    context.offset(),
                    context.pageSize());
            rows = safeList(queryRows).stream()
                    .map(this::toDataOverviewResponse)
                    .toList();
            fillDataOverviewFeatureScores(rows, context.enabledFeatures());
        }
        return new TablePageResponse<>(
                dataOverviewColumns(context.enabledFeatures()),
                context.pageNo(),
                context.pageSize(),
                total,
                rows);
    }

    /**
     * 查询评分页分页数据。
     *
     * <p>完整链路如下：
     * 1. normalize 统一处理空请求、分页默认值、启用特性快照、过滤条件和排序条件；
     * 2. countScores 以 pq_answer 为粒度计算满足条件的问卷数；
     * 3. selectScoreRows 查询当前页的基础字段，动态评分排序需要的 join 已在排序规范化阶段生成；
     * 4. fillScoreFeatureScores 按当前页 answerId 批量查询评分明细并写入动态响应字段；
     * 5. scoreColumns 返回固定列和启用特性动态列，供前端按同一 key 渲染 rows。</p>
     */
    public TablePageResponse<ScoreRowResponse> queryScores(TableQueryRequest request) {
        QueryContext context = normalize(request, QueryType.SCORES);
        long total = queryMapper.countScores(context.criteria());
        List<ScoreRowResponse> rows = List.of();
        if (total > 0) {
            List<ScoreQueryRow> queryRows = queryMapper.selectScoreRows(
                    context.criteria(),
                    context.orderClauses(),
                    context.featureScoreSorts(),
                    context.offset(),
                    context.pageSize());
            rows = safeList(queryRows).stream()
                    .map(this::toScoreResponse)
                    .toList();
            fillScoreFeatureScores(rows, context.enabledFeatures());
        }
        return new TablePageResponse<>(
                scoreColumns(context.enabledFeatures()),
                context.pageNo(),
                context.pageSize(),
                total,
                rows);
    }

    public TablePageResponse<OpinionRowResponse> queryOpinions(TableQueryRequest request) {
        QueryContext context = normalize(request, QueryType.OPINIONS);
        long total = queryMapper.countOpinions(context.criteria());
        List<OpinionRowResponse> rows = List.of();
        if (total > 0) {
            List<OpinionQueryRow> queryRows = queryMapper.selectOpinionRows(
                    context.criteria(),
                    context.orderClauses(),
                    context.offset(),
                    context.pageSize());
            rows = safeList(queryRows).stream()
                    .map(this::toOpinionResponse)
                    .toList();
        }
        return new TablePageResponse<>(
                opinionColumns(),
                context.pageNo(),
                context.pageSize(),
                total,
                rows);
    }

    /**
     * 将外部请求规范化为 Mapper 可直接消费的查询上下文。
     *
     * <p>启用特性在这里一次性读取，后续动态评分过滤、动态评分排序、响应动态列和评分回填
     * 都使用同一份快照，避免一次请求内出现校验和展示列不一致。</p>
     */
    private QueryContext normalize(TableQueryRequest request, QueryType queryType) {
        int pageNo = normalizePageNo(request == null ? null : request.pageNo());
        int pageSize = normalizePageSize(request == null ? null : request.pageSize());
        int offset = normalizeOffset(pageNo, pageSize);
        List<FeatureRef> enabledFeatures = safeList(featureMapper.selectEnabledFeatures());
        Map<Long, FeatureRef> featureById = enabledFeatures.stream()
                .filter(feature -> feature.getId() != null)
                .collect(Collectors.toMap(
                        FeatureRef::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
        TableQueryCriteria criteria = normalizeCriteria(
                request == null ? null : request.filters(),
                request == null ? null : request.featureScoreFilters(),
                queryType,
                featureById);
        SortContext sortContext = normalizeSorts(
                request == null ? null : request.sorts(),
                queryType,
                featureById);
        return new QueryContext(
                pageNo,
                pageSize,
                offset,
                criteria,
                sortContext.orderClauses(),
                sortContext.featureScoreSorts(),
                enabledFeatures);
    }

    private int normalizePageNo(Integer pageNo) {
        if (pageNo == null || pageNo < 1) {
            return DEFAULT_PAGE_NO;
        }
        return pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private int normalizeOffset(int pageNo, int pageSize) {
        long offset = (long) (pageNo - 1) * pageSize;
        if (offset > Integer.MAX_VALUE) {
            throw QuestionnaireQueryException.invalid("分页偏移量超出限制");
        }
        return (int) offset;
    }

    /**
     * 校验并构造固定过滤条件。
     *
     * <p>评分页只接受问卷和评分相关条件；sentiment、featureCategoryName 和 keyword 属于观点文本维度，
     * 会在 validateQueryTypeFilters 中被拒绝。评分页的 featureId 必须来自当前启用特性，否则后续 SQL
     * 无法与响应动态列保持一致。数据概览和观点页的分类过滤使用 featureCategoryName 自由文本，
     * 不再接受 featureId 作为观点分类条件。</p>
     */
    private TableQueryCriteria normalizeCriteria(TableQueryFilterRequest filters,
                                                 List<FeatureScoreFilterRequest> featureScoreFilters,
                                                 QueryType queryType,
                                                 Map<Long, FeatureRef> enabledFeatureById) {
        if (filters != null && filters.answerTimeStart() != null && filters.answerTimeEnd() != null
                && filters.answerTimeStart().isAfter(filters.answerTimeEnd())) {
            throw QuestionnaireQueryException.invalid("答卷时间开始值不能晚于结束值");
        }
        validateScoreRange(
                filters == null ? null : filters.recommendScoreMin(),
                filters == null ? null : filters.recommendScoreMax(),
                "推荐意愿评分");
        validateQueryTypeFilters(filters, queryType);
        if (filters != null && filters.userCategory() != null
                && !USER_CATEGORY_CODES.contains(filters.userCategory())) {
            throw QuestionnaireQueryException.invalid("用户归类不支持：" + filters.userCategory());
        }
        if (filters != null && filters.sentiment() != null
                && !SENTIMENT_CODES.contains(filters.sentiment())) {
            throw QuestionnaireQueryException.invalid("情感观点不支持：" + filters.sentiment());
        }
        if (filters != null && filters.featureId() != null
                && !enabledFeatureById.containsKey(filters.featureId())) {
            throw QuestionnaireQueryException.featureNotFound(filters.featureId());
        }

        List<FeatureScoreFilterCriteria> scoreFilters = normalizeFeatureScoreFilters(
                featureScoreFilters,
                queryType,
                enabledFeatureById);
        return TableQueryCriteria.builder()
                .questionnaireId(normalizeText(filters == null ? null : filters.questionnaireId()))
                .productCode(normalizeText(filters == null ? null : filters.productCode()))
                .productModel(normalizeText(filters == null ? null : filters.productModel()))
                .answerTimeStart(filters == null ? null : filters.answerTimeStart())
                .answerTimeEnd(filters == null ? null : filters.answerTimeEnd())
                .romVersion(normalizeText(filters == null ? null : filters.romVersion()))
                .appVersion(normalizeText(filters == null ? null : filters.appVersion()))
                .recommendScoreMin(filters == null ? null : filters.recommendScoreMin())
                .recommendScoreMax(filters == null ? null : filters.recommendScoreMax())
                .userCategory(filters == null ? null : filters.userCategory())
                .sentiment(filters == null ? null : filters.sentiment())
                .featureId(filters == null ? null : filters.featureId())
                .featureCategoryName(normalizeText(filters == null ? null : filters.featureCategoryName()))
                .keyword(normalizeText(filters == null ? null : filters.keyword()))
                .featureScoreFilters(scoreFilters)
                .build();
    }

    /**
     * 过滤条件的页面级约束。
     *
     * <p>评分页以问卷为粒度，不连接 pq_opinion，因此不允许按情感观点、观点分类或文本关键词过滤；
     * 这类条件只在概览/观点查询中有明确语义。反过来，概览/观点查询中的“特性分类名称”已经是自由文本，
     * 不再支持 featureId 分类过滤。</p>
     */
    private void validateQueryTypeFilters(TableQueryFilterRequest filters, QueryType queryType) {
        if (filters == null) {
            return;
        }
        if (queryType == QueryType.SCORES) {
            if (filters.sentiment() != null) {
                throw QuestionnaireQueryException.invalid("评分查询不支持情感观点过滤");
            }
            if (normalizeText(filters.featureCategoryName()) != null) {
                throw QuestionnaireQueryException.invalid("评分查询不支持特性分类名称过滤");
            }
            if (normalizeText(filters.keyword()) != null) {
                throw QuestionnaireQueryException.invalid("评分查询不支持关键词过滤");
            }
            return;
        }
        if (filters.featureId() != null) {
            throw QuestionnaireQueryException.invalid("数据总览和观点查询不支持特性ID过滤，请使用特性分类名称过滤");
        }
    }

    /**
     * 校验并转换动态特性评分过滤。
     *
     * <p>每个过滤项最终会在 XML 中转换为一个 EXISTS 子查询，表示同一份问卷必须存在指定
     * featureId 的评分且满足区间条件。min/max 都为空时只判断该特性评分是否存在。</p>
     */
    private List<FeatureScoreFilterCriteria> normalizeFeatureScoreFilters(
            List<FeatureScoreFilterRequest> featureScoreFilters,
            QueryType queryType,
            Map<Long, FeatureRef> enabledFeatureById) {
        List<FeatureScoreFilterRequest> requests = featureScoreFilters == null
                ? List.of()
                : featureScoreFilters;
        if (requests.isEmpty()) {
            return List.of();
        }
        if (queryType == QueryType.OPINIONS) {
            throw QuestionnaireQueryException.invalid("观点查询不支持特性评分过滤");
        }

        List<FeatureScoreFilterCriteria> normalized = new ArrayList<>(requests.size());
        for (FeatureScoreFilterRequest filter : requests) {
            Long featureId = filter == null ? null : filter.featureId();
            if (featureId == null || featureId <= 0) {
                throw QuestionnaireQueryException.invalid("特性评分过滤必须指定有效特性ID");
            }
            if (!enabledFeatureById.containsKey(featureId)) {
                throw QuestionnaireQueryException.featureNotFound(featureId);
            }
            Integer min = filter == null ? null : filter.min();
            Integer max = filter == null ? null : filter.max();
            validateScoreRange(min, max, "特性评分");
            normalized.add(new FeatureScoreFilterCriteria(featureId, min, max));
        }
        return normalized;
    }

    private void validateScoreRange(Integer min, Integer max, String fieldName) {
        if (min != null && (min < 1 || min > 10)) {
            throw QuestionnaireQueryException.invalid(fieldName + "范围必须在1到10之间");
        }
        if (max != null && (max < 1 || max > 10)) {
            throw QuestionnaireQueryException.invalid(fieldName + "范围必须在1到10之间");
        }
        if (min != null && max != null && min > max) {
            throw QuestionnaireQueryException.invalid(fieldName + "最小值不能大于最大值");
        }
    }

    /**
     * 校验排序请求并生成受控 SQL 排序上下文。
     *
     * <p>固定字段排序通过 resolveSortExpression 映射到白名单 SQL 表达式。动态评分字段必须
     * 使用 featureScore:{featureId}，解析后会生成一个受控 LEFT JOIN 别名，并以该别名的
     * score 列排序。Mapper XML 中使用 ${} 输出排序表达式，安全边界就在这里。</p>
     */
    private SortContext normalizeSorts(List<TableSortRequest> sorts,
                                       QueryType queryType,
                                       Map<Long, FeatureRef> enabledFeatureById) {
        List<TableSortRequest> requestedSorts = sorts == null ? List.of() : sorts;
        if (requestedSorts.isEmpty()) {
            return defaultSort(queryType);
        }

        List<TableOrderClause> orderClauses = new ArrayList<>();
        List<FeatureScoreSortClause> scoreSorts = new ArrayList<>();
        int scoreSortIndex = 0;
        for (TableSortRequest sort : requestedSorts) {
            String field = normalizeText(sort == null ? null : sort.field());
            if (field == null) {
                throw QuestionnaireQueryException.invalid("排序字段不能为空");
            }
            String direction = normalizeDirection(sort == null ? null : sort.direction());
            if (field.startsWith(FEATURE_SCORE_PREFIX)) {
                if (queryType == QueryType.OPINIONS) {
                    throw QuestionnaireQueryException.invalid("观点查询不支持特性评分排序");
                }
                Long featureId = parseFeatureScoreSortField(field);
                if (!enabledFeatureById.containsKey(featureId)) {
                    throw QuestionnaireQueryException.featureNotFound(featureId);
                }
                String alias = "sort_score_" + scoreSortIndex++;
                scoreSorts.add(new FeatureScoreSortClause(alias, featureId));
                orderClauses.add(new TableOrderClause(alias + ".score", direction));
                continue;
            }
            orderClauses.add(new TableOrderClause(resolveSortExpression(field, queryType), direction));
        }
        return new SortContext(appendStableTailSorts(orderClauses, queryType), scoreSorts);
    }

    private String normalizeDirection(String direction) {
        if (direction == null) {
            return "ASC";
        }
        String normalized = direction.trim().toUpperCase(Locale.ROOT);
        if (!"ASC".equals(normalized) && !"DESC".equals(normalized)) {
            throw QuestionnaireQueryException.invalid("排序方向只支持 asc 或 desc");
        }
        return normalized;
    }

    /**
     * 解析动态评分排序字段。
     *
     * <p>字段格式必须与响应动态列 key 完全一致，即 featureScore:{featureId}。
     * 解析出的 featureId 还会在调用方校验是否属于当前启用特性。</p>
     */
    private Long parseFeatureScoreSortField(String field) {
        String idText = field.substring(FEATURE_SCORE_PREFIX.length());
        try {
            long featureId = Long.parseLong(idText);
            if (featureId <= 0) {
                throw new NumberFormatException("featureId must be positive");
            }
            return featureId;
        } catch (NumberFormatException ex) {
            throw QuestionnaireQueryException.invalid("动态评分排序字段格式应为 featureScore:{featureId}");
        }
    }

    /**
     * 将外部排序字段映射为内部 SQL 表达式白名单。
     *
     * <p>调用方只能传响应字段名，不能传 SQL 片段。评分页支持问卷基础字段和版本字段，
     * 不支持观点序号、情感观点和特性名称等观点粒度字段。</p>
     */
    private String resolveSortExpression(String field, QueryType queryType) {
        Map<String, String> whitelist = new LinkedHashMap<>();
        whitelist.put("questionnaireId", "a.questionnaire_id");
        whitelist.put("productModel", "p.product_model");
        whitelist.put("productCode", "p.product_code");
        whitelist.put("answerTime", "a.answer_time");
        whitelist.put("recommendScore", "a.recommend_score");
        whitelist.put("userCategory", "a.user_category");
        if (queryType == QueryType.DATA_OVERVIEW || queryType == QueryType.SCORES) {
            whitelist.put("romVersion", "a.rom_version");
            whitelist.put("appVersion", "a.app_version");
        }
        if (queryType == QueryType.DATA_OVERVIEW || queryType == QueryType.OPINIONS) {
            whitelist.put("opinionSeq", "o.opinion_seq");
            whitelist.put("sentiment", "o.sentiment_code");
            whitelist.put("featureCategoryName", "o.feature_category_name");
        }

        String expression = whitelist.get(field);
        if (expression == null) {
            throw QuestionnaireQueryException.invalid("不支持的排序字段：" + field);
        }
        return expression;
    }

    /**
     * 补充稳定尾排序。
     *
     * <p>业务排序字段可能存在重复值，追加主键或观点顺序可以保证分页翻页时结果顺序稳定。
     * 如果调用方已经显式排序同一表达式，则不重复追加。</p>
     */
    private List<TableOrderClause> appendStableTailSorts(List<TableOrderClause> orderClauses, QueryType queryType) {
        List<TableOrderClause> stableOrders = new ArrayList<>(orderClauses);
        addOrderIfAbsent(stableOrders, "a.id", "ASC");
        if (queryType == QueryType.DATA_OVERVIEW || queryType == QueryType.OPINIONS) {
            addOrderIfAbsent(stableOrders, "o.opinion_seq", "ASC");
            addOrderIfAbsent(stableOrders, "o.id", "ASC");
        }
        return stableOrders;
    }

    private void addOrderIfAbsent(List<TableOrderClause> orderClauses, String expression, String direction) {
        boolean exists = orderClauses.stream()
                .anyMatch(order -> expression.equals(order.getExpression()));
        if (!exists) {
            orderClauses.add(new TableOrderClause(expression, direction));
        }
    }

    /**
     * 未传排序时的默认顺序。
     *
     * <p>评分页默认按答卷时间倒序、答卷主键倒序展示最新导入/最新答卷；概览和观点页还需要
     * 在同一问卷下保持观点序号升序。</p>
     */
    private SortContext defaultSort(QueryType queryType) {
        List<TableOrderClause> orders = new ArrayList<>();
        orders.add(new TableOrderClause("a.answer_time", "DESC"));
        orders.add(new TableOrderClause("a.id", "DESC"));
        if (queryType == QueryType.DATA_OVERVIEW || queryType == QueryType.OPINIONS) {
            orders.add(new TableOrderClause("o.opinion_seq", "ASC"));
        }
        return new SortContext(orders, List.of());
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private DataOverviewRowResponse toDataOverviewResponse(DataOverviewQueryRow row) {
        DataOverviewRowResponse response = new DataOverviewRowResponse();
        response.setAnswerId(row.getAnswerId());
        response.setOpinionId(row.getOpinionId());
        response.setQuestionnaireId(row.getQuestionnaireId());
        response.setProductModel(row.getProductModel());
        response.setProductCode(row.getProductCode());
        response.setAnswerTime(row.getAnswerTime());
        response.setRomVersion(row.getRomVersion());
        response.setAppVersion(row.getAppVersion());
        response.setFeedbackText(row.getFeedbackText());
        response.setScoreReason(row.getScoreReason());
        response.setRecommendScore(row.getRecommendScore());
        response.setUserCategory(userCategoryDisplayName(row.getUserCategory()));
        response.setSentiment(sentimentDisplayName(row.getSentiment()));
        response.setFeatureCategoryName(row.getFeatureCategoryName());
        response.setFeedbackContent1(row.getFeedbackContent1());
        response.setFeedbackContent2(row.getFeedbackContent2());
        return response;
    }

    /**
     * 将评分页基础查询行转换为接口响应行。
     *
     * <p>这里仅转换固定字段和枚举展示名，动态特性评分由 fillScoreFeatureScores 在后续步骤
     * 根据当前页 answerId 批量补齐。</p>
     */
    private ScoreRowResponse toScoreResponse(ScoreQueryRow row) {
        ScoreRowResponse response = new ScoreRowResponse();
        response.setAnswerId(row.getAnswerId());
        response.setQuestionnaireId(row.getQuestionnaireId());
        response.setProductModel(row.getProductModel());
        response.setProductCode(row.getProductCode());
        response.setAnswerTime(row.getAnswerTime());
        response.setRomVersion(row.getRomVersion());
        response.setAppVersion(row.getAppVersion());
        response.setRecommendScore(row.getRecommendScore());
        response.setUserCategory(userCategoryDisplayName(row.getUserCategory()));
        return response;
    }

    private OpinionRowResponse toOpinionResponse(OpinionQueryRow row) {
        OpinionRowResponse response = new OpinionRowResponse();
        response.setOpinionId(row.getOpinionId());
        response.setAnswerId(row.getAnswerId());
        response.setQuestionnaireId(row.getQuestionnaireId());
        response.setProductModel(row.getProductModel());
        response.setProductCode(row.getProductCode());
        response.setAnswerTime(row.getAnswerTime());
        response.setRecommendScore(row.getRecommendScore());
        response.setUserCategory(userCategoryDisplayName(row.getUserCategory()));
        response.setOpinionSeq(row.getOpinionSeq());
        response.setFeatureCategoryName(row.getFeatureCategoryName());
        response.setSentiment(sentimentDisplayName(row.getSentiment()));
        response.setFeedbackContent1(row.getFeedbackContent1());
        response.setFeedbackContent2(row.getFeedbackContent2());
        response.setFeedbackText(row.getFeedbackText());
        response.setScoreReason(row.getScoreReason());
        return response;
    }

    private List<TableColumnResponse> dataOverviewColumns(List<FeatureRef> enabledFeatures) {
        List<TableColumnResponse> columns = new ArrayList<>(List.of(
                column("questionnaireId", "问卷ID"),
                column("productModel", "产品型号"),
                column("productCode", "产品编码"),
                column("answerTime", "答卷时间"),
                column("romVersion", "ROM版本"),
                column("appVersion", "App版本"),
                column("feedbackText", "用户反馈与建议", false, false),
                column("scoreReason", "打分原因", false, false),
                column("recommendScore", "推荐意愿"),
                column("userCategory", "用户归类"),
                column("sentiment", "情感观点"),
                column("featureCategoryName", "特性分类名称"),
                column("feedbackContent1", "特性具体反馈内容1", false, false),
                column("feedbackContent2", "特性具体反馈内容2", false, false)
        ));
        appendFeatureScoreColumns(columns, enabledFeatures);
        return columns;
    }

    /**
     * 生成评分页列定义。
     *
     * <p>固定列保持“问卷上下文 + 推荐意愿评分 + 用户归类”的精简字段集，动态列按启用特性顺序
     * 追加，key 与 ScoreRowResponse 中的扁平字段一致。</p>
     */
    private List<TableColumnResponse> scoreColumns(List<FeatureRef> enabledFeatures) {
        List<TableColumnResponse> columns = new ArrayList<>(List.of(
                column("questionnaireId", "问卷ID"),
                column("productModel", "产品型号"),
                column("productCode", "产品编码"),
                column("answerTime", "答卷时间"),
                column("romVersion", "ROM版本"),
                column("appVersion", "App版本"),
                column("recommendScore", "推荐意愿"),
                column("userCategory", "用户归类")
        ));
        appendFeatureScoreColumns(columns, enabledFeatures);
        return columns;
    }

    private List<TableColumnResponse> opinionColumns() {
        return List.of(
                column("questionnaireId", "问卷ID"),
                column("productModel", "产品型号"),
                column("productCode", "产品编码"),
                column("answerTime", "答卷时间"),
                column("recommendScore", "推荐意愿"),
                column("userCategory", "用户归类"),
                column("opinionSeq", "观点序号", true, false),
                column("featureCategoryName", "特性分类名称"),
                column("sentiment", "情感观点"),
                column("feedbackContent1", "特性具体反馈内容1", false, false),
                column("feedbackContent2", "特性具体反馈内容2", false, false)
        );
    }

    private TableColumnResponse column(String key, String title) {
        return column(key, title, true, true);
    }

    private TableColumnResponse column(String key, String title, boolean sortable, boolean filterable) {
        return new TableColumnResponse(key, title, sortable, filterable);
    }

    /**
     * 追加动态特性评分列。
     *
     * <p>列标题复用 Excel 模板表头规则，即 QuestionnaireExcelHeaders.featureScoreHeader；
     * 当前约定是“特性名称 + 体验”。列 key 使用 featureScore:{featureId}，与过滤、排序和行数据
     * 的动态字段保持同一个契约。</p>
     */
    private void appendFeatureScoreColumns(List<TableColumnResponse> columns, List<FeatureRef> enabledFeatures) {
        for (FeatureRef feature : enabledFeatures) {
            columns.add(new TableColumnResponse(
                    FEATURE_SCORE_PREFIX + feature.getId(),
                    QuestionnaireExcelHeaders.featureScoreHeader(feature),
                    true,
                    true));
        }
    }

    private void fillDataOverviewFeatureScores(List<DataOverviewRowResponse> rows, List<FeatureRef> enabledFeatures) {
        fillFeatureScores(
                rows,
                DataOverviewRowResponse::getAnswerId,
                enabledFeatureIds(enabledFeatures),
                DataOverviewRowResponse::putFeatureScore);
    }

    /**
     * 为评分页当前页结果补齐动态特性评分。
     *
     * <p>只处理当前页 rows 中出现的 answerId，并且只输出当前仍启用的特性评分。历史上停用的特性
     * 即使仍保留在明细表中，也不会出现在当前评分页动态列中。</p>
     */
    private void fillScoreFeatureScores(List<ScoreRowResponse> rows, List<FeatureRef> enabledFeatures) {
        fillFeatureScores(
                rows,
                ScoreRowResponse::getAnswerId,
                enabledFeatureIds(enabledFeatures),
                ScoreRowResponse::putFeatureScore);
    }

    private Set<Long> enabledFeatureIds(List<FeatureRef> enabledFeatures) {
        return enabledFeatures.stream()
                .map(FeatureRef::getId)
                .filter(id -> id != null)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 动态评分回填的通用实现。
     *
     * <p>主查询先完成分页，再用 answerId IN (...) 批量查评分明细。这样可以避免为了展示动态列
     * 在主查询中展开 pq_answer_feature_score，导致一份问卷被多条评分记录放大为多行。</p>
     */
    private <T> void fillFeatureScores(List<T> rows,
                                       Function<T, Long> answerIdGetter,
                                       Set<Long> enabledFeatureIds,
                                       FeatureScoreWriter<T> writer) {
        if (rows.isEmpty() || enabledFeatureIds.isEmpty()) {
            return;
        }
        List<Long> answerIds = rows.stream()
                .map(answerIdGetter)
                .filter(answerId -> answerId != null)
                .distinct()
                .toList();
        if (answerIds.isEmpty()) {
            return;
        }
        Map<Long, List<FeatureScoreCell>> scoresByAnswerId = safeList(queryMapper
                .selectFeatureScoresByAnswerIds(answerIds))
                .stream()
                .filter(score -> score != null
                        && score.getAnswerId() != null
                        && enabledFeatureIds.contains(score.getFeatureId()))
                .collect(Collectors.groupingBy(FeatureScoreCell::getAnswerId));
        for (T row : rows) {
            Long answerId = answerIdGetter.apply(row);
            if (answerId == null) {
                continue;
            }
            for (FeatureScoreCell score : scoresByAnswerId.getOrDefault(answerId, List.of())) {
                writer.write(row, score.getFeatureId(), score.getScore());
            }
        }
    }

    private String userCategoryDisplayName(Integer code) {
        return Arrays.stream(UserCategory.values())
                .filter(category -> code != null && category.getCode() == code)
                .map(UserCategory::getDisplayName)
                .findFirst()
                .orElse(UserCategory.UNKNOWN.getDisplayName());
    }

    private String sentimentDisplayName(Integer code) {
        return Arrays.stream(Sentiment.values())
                .filter(sentiment -> code != null && sentiment.getCode() == code)
                .map(Sentiment::getDisplayName)
                .findFirst()
                .orElse(Sentiment.NO_FEEDBACK.getDisplayName());
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    /**
     * 查询页面类型。
     *
     * <p>同一套请求 DTO 会被三个页面复用，QueryType 用来限定每个页面允许的过滤条件、
     * 排序字段、默认排序和行粒度。</p>
     */
    private enum QueryType {
        DATA_OVERVIEW,
        SCORES,
        OPINIONS
    }

    @FunctionalInterface
    private interface FeatureScoreWriter<T> {
        void write(T row, Long featureId, Integer score);
    }

    /**
     * 排序规范化结果。
     *
     * <p>orderClauses 是最终 ORDER BY 项；featureScoreSorts 是为了支持动态评分排序而额外
     * 传给 XML 的 LEFT JOIN 描述。没有动态评分排序时，该列表为空。</p>
     */
    private record SortContext(
            List<TableOrderClause> orderClauses,
            List<FeatureScoreSortClause> featureScoreSorts
    ) {
    }

    /**
     * 单次查询的完整上下文。
     *
     * <p>Controller 传入的请求经过 normalize 后只以该对象向后流转，Mapper 和响应装配阶段
     * 不再读取原始请求，保证分页、过滤、排序和动态列都基于同一轮校验结果。</p>
     */
    private record QueryContext(
            int pageNo,
            int pageSize,
            int offset,
            TableQueryCriteria criteria,
            List<TableOrderClause> orderClauses,
            List<FeatureScoreSortClause> featureScoreSorts,
            List<FeatureRef> enabledFeatures
    ) {
    }
}
