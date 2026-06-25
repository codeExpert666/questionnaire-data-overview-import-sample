# 问卷展示查询接口设计

## 背景

当前数据总览 Tab 已具备 Excel 模板下载和导入能力。导入流程会把上传文件聚合写入三类业务数据：

- `pq_answer`：一份问卷的问卷级字段。
- `pq_answer_feature_score`：一份问卷下的非空特性评分。
- `pq_opinion`：一份问卷下的一条或多条观点明细。

同一 `source_system + questionnaire_id` 再次导入时，导入逻辑会 upsert `pq_answer`，并整体替换旧评分和旧观点。因此三个展示 Tab 查询现有业务表即可得到最新数据，不需要新增导入批次表或物化宽表。

## 范围

本次设计覆盖三个只读展示查询：

- 数据总览 Tab：以和上传 Excel 一致的表格列展示最新数据库数据。
- 评分 Tab：展示问卷级评分数据，包含推荐意愿评分和各特性评分。
- 观点 Tab：展示人工提炼出的观点明细。

三个查询都使用 `POST .../query` JSON 请求，统一支持分页、条件查询和多列排序。

不在本次设计中新增统计图表、导出接口、人工观点编辑接口、导入批次追踪表或缓存表。

## 总体接口

现有 `QuestionnaireDataOverviewExcelController` 建议改名为 `QuestionnaireDataOverviewController`，继续使用原路径：

```http
GET /api/product-questionnaires/data-overview/import-template
POST /api/product-questionnaires/data-overview/import
POST /api/product-questionnaires/data-overview/query
```

评分和观点新增独立只读查询入口：

```http
POST /api/product-questionnaires/scores/query
POST /api/product-questionnaires/opinions/query
```

三个查询使用统一请求结构：

```json
{
  "pageNo": 1,
  "pageSize": 20,
  "filters": {
    "questionnaireId": "Q001",
    "productCode": "P100",
    "productModel": "Alpha",
    "answerTimeStart": "2026-06-01T00:00:00",
    "answerTimeEnd": "2026-06-30T23:59:59",
    "romVersion": "1.0",
    "appVersion": "3.2",
    "recommendScoreMin": 7,
    "recommendScoreMax": 10,
    "userCategory": 3,
    "sentiment": 3,
    "featureId": 1,
    "keyword": "续航"
  },
  "featureScoreFilters": [
    {
      "featureId": 1,
      "min": 8,
      "max": 10
    }
  ],
  "sorts": [
    {
      "field": "answerTime",
      "direction": "desc"
    },
    {
      "field": "featureScore:1",
      "direction": "asc"
    }
  ]
}
```

统一响应结构：

```json
{
  "columns": [
    {
      "key": "questionnaireId",
      "title": "问卷ID",
      "sortable": true,
      "filterable": true
    }
  ],
  "pageNo": 1,
  "pageSize": 20,
  "total": 128,
  "rows": []
}
```

`columns` 由后端生成，前端据此渲染固定列和动态特性评分列。动态评分列沿用模板列规则：当前启用特性的 `featureName + "体验"`。

## 公共查询规则

分页：

- `pageNo` 从 1 开始，缺省为 1。
- `pageSize` 缺省为 20，最大 200。
- 返回 `total`，便于前端分页器展示。

条件查询：

- `questionnaireId`、`productCode`、`productModel`、`romVersion`、`appVersion` 使用模糊匹配。
- `answerTimeStart` 和 `answerTimeEnd` 组成闭区间过滤。
- `recommendScoreMin` 和 `recommendScoreMax` 过滤 1-10 的推荐意愿分。
- `userCategory` 使用 `UserCategory.code`。
- `sentiment` 使用 `Sentiment.code`。
- `featureId` 在数据总览和评分 Tab 中表示存在该特性评分，在观点 Tab 中表示观点归属该特性。
- `keyword` 在数据总览和观点 Tab 中搜索 `feedback_text`、`score_reason`、`feedback_content_1`、`feedback_content_2`。

动态评分过滤：

- `featureScoreFilters` 只用于数据总览和评分 Tab。
- 每个 `featureId` 必须存在于当前启用特性集合中。
- 过滤语义为该问卷对应特性评分在 `[min, max]` 范围内；未填写该特性评分的问卷不命中该过滤条件。

排序：

- `sorts` 支持多列排序，按数组顺序生成 `ORDER BY`。
- `direction` 只接受 `asc` 或 `desc`。
- 排序字段必须经过后端白名单转换，不能直接拼接前端字段。
- 动态评分排序字段格式为 `featureScore:{featureId}`，只接受当前启用特性 ID。
- 默认排序为 `answerTime desc, answerId desc`；观点相关查询在同一问卷内追加 `opinionSeq asc`。

## 数据总览 Tab

行粒度：一行对应一条 `pq_opinion`。这与上传 Excel 的行粒度一致，同一问卷有多条观点时展示多行。

列设计：

- 问卷ID
- 产品型号
- 产品编码
- 答卷时间
- ROM版本
- App版本
- 用户反馈与建议
- 打分原因
- 推荐意愿
- 用户归类
- 情感观点
- 特性分类名称
- 特性具体反馈内容1
- 特性具体反馈内容2
- 当前启用特性的动态评分列，列名为 `特性名称体验`

查询数据流：

1. 分页查询 `pq_answer`、`pq_product`、`pq_opinion`，必要时左连接 `pq_feature` 获取观点归属特性名称。
2. 收集本页 `answer_id`。
3. 批量查询本页问卷的 `pq_answer_feature_score`。
4. 按 `answer_id + feature_id` 组装动态评分列。

## 评分 Tab

行粒度：一行对应一份问卷 `pq_answer`。

列设计：

- 问卷ID
- 产品型号
- 产品编码
- 答卷时间
- ROM版本
- App版本
- 推荐意愿
- 用户归类
- 当前启用特性的动态评分列，列名为 `特性名称体验`

评分 Tab 不增加平均分、总分、观点数量等衍生指标，避免把展示页变成统计页。

查询数据流：

1. 分页查询 `pq_answer` 和 `pq_product`。
2. 收集本页 `answer_id`。
3. 批量查询本页问卷的 `pq_answer_feature_score`。
4. 按当前启用特性顺序组装动态评分列。

## 观点 Tab

行粒度：一行对应一条 `pq_opinion`。

列设计：

- 问卷ID
- 产品型号
- 产品编码
- 答卷时间
- 推荐意愿
- 用户归类
- 观点序号
- 特性分类名称
- 情感观点
- 特性具体反馈内容1
- 特性具体反馈内容2

`用户反馈与建议` 和 `打分原因` 作为详情字段返回，但默认不作为主列，避免观点表过宽。`keyword` 查询仍覆盖这两个原始描述字段，便于从原始语料定位观点。

查询数据流：

1. 分页查询 `pq_opinion`、`pq_answer`、`pq_product`，左连接 `pq_feature` 获取观点归属特性名称。
2. 直接返回观点行，不需要动态评分组装。

## 后端模块

建议新增或调整以下类：

- `QuestionnaireDataOverviewController`：替代 `QuestionnaireDataOverviewExcelController`，承接模板下载、导入和数据总览查询。
- `QuestionnaireScoreController`：评分 Tab 查询入口。
- `QuestionnaireOpinionController`：观点 Tab 查询入口。
- `QuestionnaireTableQueryService`：统一编排三个展示查询。
- `QuestionnaireTableQueryMapper`：承载分页 count、分页 rows、批量评分查询。
- `TableQueryRequest`：分页、过滤和排序请求。
- `TablePageResponse<T>`：统一表格响应。
- `TableColumnResponse`：后端生成的列元数据。
- `DataOverviewRowResponse`：数据总览行。
- `ScoreRowResponse`：评分行。
- `OpinionRowResponse`：观点行。

## SQL 设计

MyBatis XML 中应将 SQL 拆成可复用片段：

- 基础过滤片段：问卷、产品、时间、版本、推荐意愿、用户归类。
- 观点过滤片段：情感观点、观点特性、关键词。
- 特性评分过滤片段：使用 `EXISTS` 子查询过滤 `pq_answer_feature_score`。
- 排序片段：由 Service 先把前端字段转换为安全 SQL 片段，再传给 Mapper。

动态评分排序可用左连接子查询实现：

```sql
LEFT JOIN pq_answer_feature_score sort_score
       ON sort_score.answer_id = a.id
      AND sort_score.feature_id = #{sortFeatureId}
```

多列排序中允许出现多个动态评分排序项。Service 为每个动态评分排序项生成稳定别名，例如 `sort_score_0`、`sort_score_1`，并只把白名单校验后的 SQL 片段传给 Mapper。

## 错误处理

新增只读查询异常，例如 `QuestionnaireQueryException`，由现有全局异常处理器转换为 `ApiErrorResponse`。

错误码建议：

- `QUESTIONNAIRE_QUERY_INVALID`：分页、过滤、排序参数非法。
- `QUESTIONNAIRE_QUERY_FEATURE_NOT_FOUND`：动态评分过滤或排序引用的特性不存在或已停用。

查询接口不写数据库，也不递增 `pq:data-version`。

## 测试

Controller 测试：

- 三个 `POST .../query` 能接收 JSON 并返回统一分页结构。
- 非法排序字段返回 `QUESTIONNAIRE_QUERY_INVALID`。
- 动态评分排序字段格式错误返回 400。

Service 测试：

- 默认分页和分页上限规范化。
- 排序字段白名单转换。
- 动态评分过滤校验当前启用特性。
- 评分明细能按 `answerId + featureId` 组装成行内动态评分 map。

Mapper 或 SQL 结构测试：

- SQL 不直接拼接原始排序字段。
- 数据总览和评分 Tab 能按本页 `answer_id` 批量查询评分。
- 观点 Tab 能返回 `opinion_seq` 并按默认顺序稳定展示。

最终验证命令：

```bash
mvn test
git diff --check
```
