# 数据总览：问卷观点模板下载与 Excel 导入

技术栈：Java 17、Spring Boot 3、MyBatis、MySQL 5.7、Redis、EasyExcel 4.0.3。

## 1. 实现范围

- 下载问卷观点导入模板。
- 维护 `pq_product` 产品型号字典，支持用户自定义产品编码和型号。
- 维护 `pq_feature` 产品特性字典，支持用户自定义特性。
- 模板包含所有 `pq_feature.status = 1` 的特性评分列，不按产品裁剪。
- 一个模板可混合填写任意产品；产品不涉及某特性时，对应评分留空。
- 一份问卷可以有多行观点，导入时聚合为：
  - `pq_answer` 一行；
  - `pq_answer_feature_score` 多行；
  - `pq_opinion` 多行。
- 相同问卷 ID 再次导入时：upsert 答卷，并整体替换该问卷原有评分和观点。
- 不使用导入批次表。
- 全文件同一事务：任一行失败，全部回滚。
- EasyExcel 监听器流式读取；每 100 份问卷批量写库，避免把整份 Excel 留在内存中。

## 2. 模板列

固定列按以下顺序排列：

1. 问卷ID
2. 产品型号
3. 产品编码
4. 答卷时间
5. ROM版本
6. App版本
7. 用户反馈与建议
8. 打分原因
9. 推荐意愿
10. 用户归类
11. 情感观点
12. 特性分类名称
13. 特性具体反馈内容1
14. 特性具体反馈内容2

随后追加全部启用特性，表头格式为：

```text
特性名称体验
```

例如：

```text
音质音效体验
```

评分列表头固定为特性名称追加“体验”。导入时会按当前启用特性的顺序和名称校验评分列；
特性发生增删、改名或排序变化后，旧模板会提示重新下载。观点所属特性填写固定列
`特性分类名称`，特性编码仍作为后台和 API 的稳定标识保留在 `特性字典` 中。

模板还包含：

- `填写说明`：导入规则说明；
- `产品字典`：全部启用产品的产品编码和产品型号；
- `特性字典`：全部启用特性的特性名称和特性编码。

## 3. 重要导入规则

- `问卷ID + source_system=EXCEL` 唯一。
- 同一问卷的多条观点必须连续排列。
- 同一问卷重复行中的问卷级字段和全部特性评分必须一致。
- 产品编码必须存在且启用，产品型号必须与主数据匹配。
- 产品未配置的特性评分必须留空。
- 推荐意愿和特性评分必须为 1-10 的整数。
- 用户归类可留空，后端按配置计算；填写时必须与计算结果一致。
- 情感观点仅支持：好评、中评、差评、未反馈。
- 特性分类填写特性名称；无法归类时允许留空。
- Excel 中的问卷 ID、产品编码、版本号应按文本保存，避免 Excel 对长数字做精度处理。

## 4. 接口

### 4.1 产品型号配置

```http
GET /api/product-questionnaires/products
```

返回全部产品，包含已停用产品。列表按启用状态和主键排序。

```http
POST /api/product-questionnaires/products
Content-Type: application/json

{
  "productCode": "P100",
  "productModel": "Alpha",
  "status": 1
}
```

创建产品型号。`productCode` 是稳定编码，创建后不允许修改；可省略，省略时自动生成 `P{id}`。显式传入时仅支持字母、数字、下划线、点和短横线，长度不超过 64。`status` 可省略，默认启用。

```http
PUT /api/product-questionnaires/products/{id}
Content-Type: application/json

{
  "productModel": "Alpha Pro"
}
```

更新产品型号。

```http
PATCH /api/product-questionnaires/products/{id}/status
Content-Type: application/json

{
  "status": 0
}
```

启用或停用产品。`status=1` 表示启用，`status=0` 表示停用。

```http
DELETE /api/product-questionnaires/products/{id}
```

软删除产品，等价于将 `status` 置为 `0`。历史答卷仍保留原 `product_id` 引用。

产品变更后会在事务提交后递增 Redis 中的 `pq:data-version`。模板下载和导入只读取 `pq_product.status = 1` 的产品。

### 4.2 产品特性配置

```http
GET /api/product-questionnaires/features
```

返回全部产品特性，包含已停用特性。列表按启用状态、排序号和主键排序。

```http
POST /api/product-questionnaires/features
Content-Type: application/json

{
  "featureCode": "BATTERY",
  "featureName": "续航",
  "sortNo": 10,
  "status": 1
}
```

创建产品特性。`featureCode` 是稳定编码，创建后不允许修改；可省略，省略时自动生成 `F{id}`。显式传入时仅支持字母、数字、下划线、点和短横线，长度不超过 64。`status` 可省略，默认启用。

```http
PUT /api/product-questionnaires/features/{id}
Content-Type: application/json

{
  "featureName": "超长续航",
  "sortNo": 20
}
```

更新特性名称和排序号。

```http
PATCH /api/product-questionnaires/features/{id}/status
Content-Type: application/json

{
  "status": 0
}
```

启用或停用特性。`status=1` 表示启用，`status=0` 表示停用。

```http
DELETE /api/product-questionnaires/features/{id}
```

软删除特性，等价于将 `status` 置为 `0`。历史答卷、评分和观点仍保留原 `feature_id` 引用。

特性变更后会在事务提交后递增 Redis 中的 `pq:data-version`，便于外部缓存刷新。模板下载和导入继续只读取 `pq_feature.status = 1` 的特性。

### 4.3 产品-特性适用关系配置

`pq_product_feature` 用于配置某个产品支持哪些特性。模板仍会列出全部启用特性，但导入时会按该关系校验：产品未配置的特性评分必须留空，观点的特性分类也不能归到该产品不支持的特性。

```http
GET /api/product-questionnaires/products/{productId}/features
```

返回某个产品的特性配置视图。`features` 包含全部特性字典项，`selected=true` 表示当前产品已启用该适用关系。

```json
{
  "productId": 1,
  "productCode": "P100",
  "productModel": "Alpha",
  "productStatus": 1,
  "features": [
    {
      "id": 1,
      "featureCode": "BATTERY",
      "featureName": "续航",
      "sortNo": 10,
      "status": 1,
      "selected": true
    }
  ]
}
```

```http
PUT /api/product-questionnaires/products/{productId}/features
Content-Type: application/json

{
  "featureIds": [1, 2, 3]
}
```

按产品整包保存适用特性集合。`featureIds` 表示保存后的完整启用集合；未提交的旧关系会被置为 `status=0`，提交的关系会插入或重新置为 `status=1`。空数组表示清空该产品全部适用特性。保存时只接受存在且启用的特性 ID，产品和关系变更后会在事务提交后递增 Redis 中的 `pq:data-version`。

### 4.4 模板下载与导入

```http
GET /api/product-questionnaires/data-overview/import-template
```

下载 `.xlsx` 模板。

```http
POST /api/product-questionnaires/data-overview/import
Content-Type: multipart/form-data
file=<xlsx>
```

成功示例：

```json
{
  "dataRowCount": 7,
  "questionnaireCount": 3,
  "opinionCount": 7,
  "featureScoreCount": 15
}
```

校验失败示例：

```json
{
  "code": "QUESTIONNAIRE_IMPORT_VALIDATION_FAILED",
  "message": "问卷观点表导入失败，共发现2个问题",
  "errors": [
    {
      "rowNumber": 3,
      "columnName": "产品编码",
      "message": "产品编码不存在：P999"
    }
  ]
}
```

## 5. 数据库要求

参考 `db/schema-reference.sql`。现有库至少需要：

- `pq_product.status`，用于控制产品是否进入模板产品字典和新导入校验；
- `pq_feature.sort_no`，用于模板中全量特性的稳定顺序；
- `pq_answer` 上唯一索引 `(source_system, questionnaire_id)`；
- `pq_answer_feature_score` 主键 `(answer_id, feature_id)`；
- `pq_opinion` 唯一索引 `(answer_id, opinion_seq)`。

项目明确不使用 `pq_import_batch`，代码也没有该表依赖。

## 6. 配置

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 20MB

questionnaire:
  import:
    max-file-size-bytes: 20971520
    max-data-rows: 100000
    max-errors: 200
    answer-batch-size: 100
    detractor-max-score: 6
    passive-max-score: 8
```

用户归类默认规则：

- 1-6：贬损者；
- 7-8：中立者；
- 9-10：推荐者。

如公司规则不同，只修改配置或 `RecommendCategoryResolver`。

## 7. 接入现有项目

1. 复制 `com.acme.questionnaire` 下相关类到公司包名。
2. 合并 MyBatis XML；如公司使用注解 SQL，可按 XML 等价迁移。
3. 将 ControllerAdvice 合并到公司统一异常处理。
4. 将 `QuestionnaireCacheVersionService` 的 key 与现有缓存规范对齐。
5. 如果已有统一响应体，替换 Controller 返回类型和异常响应类型。
6. 如果产品主数据不是 `pq_product`，只需替换三个参考数据 Mapper。

## 8. 性能与事务

- EasyExcel 不把整份工作簿加载到业务集合中。
- 同一时间只保留当前问卷和最多 `answer-batch-size` 份待写问卷。
- 答卷按批 upsert，评分及观点按块 insert。
- 整个文件仍处于一个数据库事务中，因此后半段失败会回滚前面已经执行的批次。
- 100,000 行只是保护上限，不代表所有生产环境都应开放到该值；应结合文本长度、接口超时、MySQL `max_allowed_packet` 和连接池配置压测。

## 9. 文件说明

- `QuestionnaireDataOverviewExcelService`：模板生成、导入事务入口。
- `QuestionnaireProductController` / `QuestionnaireProductService`：产品型号字典管理。
- `QuestionnaireFeatureController` / `QuestionnaireFeatureService`：产品特性字典管理。
- `QuestionnaireProductFeatureController` / `QuestionnaireProductFeatureService`：产品-特性适用关系配置。
- `QuestionnaireOpinionImportListener`：表头校验、逐行解析、问卷聚合、批量刷新。
- `QuestionnaireImportWriter`：答卷 upsert、子表整体替换。
- `QuestionnaireTemplateSheetWriteHandler`：冻结表头、列宽、下拉选项、1-10 校验。
- `db/`：参考 DDL 与已有表迁移提示。
