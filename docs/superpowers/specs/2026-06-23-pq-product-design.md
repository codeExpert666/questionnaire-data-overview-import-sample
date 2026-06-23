# pq_product 产品配置设计

## 背景

`pq_product` 是问卷导入中的产品主数据。当前代码只在导入校验时读取该表，用户无法通过接口维护产品型号和产品编码，模板中也缺少产品字典页，导致“产品型号表可由用户自定义配置”这一链路没有打通。

## 范围

本次只维护 `pq_product` 自身，不维护 `pq_product_feature` 产品-特性适用关系。

需要完成：

- 新增产品配置接口，支持查询、创建、修改型号、启停和软删除。
- `product_code` 作为稳定编码，创建后不提供修改入口。
- 模板下载增加“产品字典”工作表，列出当前启用产品的产品编码和产品型号。
- 导入引用数据只接受启用产品，停用产品不再允许新导入引用。
- 产品变更后递增 `pq:data-version`，与特性配置保持一致。
- README 补充产品配置接口说明。

## 接口设计

接口路径统一放在 `/api/product-questionnaires/products`：

- `GET /api/product-questionnaires/products`：查询全部产品，包含停用产品。
- `POST /api/product-questionnaires/products`：创建产品，字段为 `productCode`、`productModel`、`status`。
- `PUT /api/product-questionnaires/products/{id}`：更新产品型号。
- `PATCH /api/product-questionnaires/products/{id}/status`：启用或停用产品。
- `DELETE /api/product-questionnaires/products/{id}`：软删除产品，等价于 `status=0`。

`productCode` 允许字母、数字、下划线、点和短横线，长度不超过 64。`productModel` 去除首尾空白后不能为空，长度不超过 128。`status` 只接受 `1` 或 `0`，创建时为空默认启用。

## 数据流

产品维护接口通过 `QuestionnaireProductService` 写入 `pq_product`。创建时由应用层和数据库唯一索引共同防重，更新时只允许改 `product_model`，状态变更使用软删除语义。

模板下载通过 `ImportReferenceDataLoader` 读取启用产品，并写入“产品字典”工作表。导入时同样只读取启用产品，因此停用产品会触发“产品编码不存在”的校验错误。

## 错误处理

新增 `QuestionnaireProductException`，由现有 `QuestionnaireImportExceptionHandler` 转成统一 JSON：

- `QUESTIONNAIRE_PRODUCT_INVALID`：编码、型号、状态不合法，或编码重复。
- `QUESTIONNAIRE_PRODUCT_NOT_FOUND`：按 id 找不到产品。

## 测试

新增产品 Service 单元测试，覆盖列表、重复编码、创建规范化、更新不改编码、状态未变化跳过写库、软删除。

新增产品 Controller 测试，覆盖列表 JSON 和产品异常响应。

模板产品字典通过 `QuestionnaireDataOverviewExcelService` 单元测试覆盖，验证下载模板会输出“产品字典”工作表，并只包含启用产品。
