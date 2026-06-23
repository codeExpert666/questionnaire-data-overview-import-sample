# pq_product 产品配置实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 打通 `pq_product` 自定义配置能力，使用户可以维护产品型号和产品编码，并在模板中查看可用产品字典。

**Architecture:** 按现有 `pq_feature` 维护链路新增产品专用 Controller、Service、DTO、模型和 Mapper 方法。导入引用数据继续由 `ImportReferenceDataLoader` 统一加载，但产品列表改为启用产品快照，并用于模板“产品字典”工作表和导入校验。

**Tech Stack:** Java 17、Spring Boot 3、MyBatis、JUnit 5、Mockito、EasyExcel。

---

### Task 1: 产品维护 Service

**Files:**
- Create: `src/main/java/com/acme/questionnaire/model/QuestionnaireProduct.java`
- Create: `src/main/java/com/acme/questionnaire/dto/ProductCreateRequest.java`
- Create: `src/main/java/com/acme/questionnaire/dto/ProductUpdateRequest.java`
- Create: `src/main/java/com/acme/questionnaire/dto/ProductStatusRequest.java`
- Create: `src/main/java/com/acme/questionnaire/dto/ProductResponse.java`
- Create: `src/main/java/com/acme/questionnaire/exception/QuestionnaireProductException.java`
- Create: `src/main/java/com/acme/questionnaire/service/QuestionnaireProductService.java`
- Modify: `src/main/java/com/acme/questionnaire/mapper/ProductMapper.java`
- Modify: `src/main/resources/mapper/ProductMapper.xml`
- Test: `src/test/java/com/acme/questionnaire/service/QuestionnaireProductServiceTest.java`

- [ ] **Step 1: Write failing service tests**

Add tests for list, duplicate create, normalized create, update, unchanged status, and soft delete.

- [ ] **Step 2: Run service tests and verify RED**

Run: `mvn -Dtest=QuestionnaireProductServiceTest test`

Expected: compilation failure because product DTO, model, exception, and service do not exist.

- [ ] **Step 3: Implement product model, DTO, exception, Mapper methods, and service**

Mirror the `QuestionnaireFeatureService` shape, with product-specific validation and error codes.

- [ ] **Step 4: Run service tests and verify GREEN**

Run: `mvn -Dtest=QuestionnaireProductServiceTest test`

Expected: tests pass.

### Task 2: 产品维护 Controller

**Files:**
- Create: `src/main/java/com/acme/questionnaire/controller/QuestionnaireProductController.java`
- Modify: `src/main/java/com/acme/questionnaire/controller/QuestionnaireImportExceptionHandler.java`
- Test: `src/test/java/com/acme/questionnaire/controller/QuestionnaireProductControllerTest.java`

- [ ] **Step 1: Write failing controller tests**

Add tests for product list JSON and product exception JSON.

- [ ] **Step 2: Run controller tests and verify RED**

Run: `mvn -Dtest=QuestionnaireProductControllerTest test`

Expected: compilation failure because product controller does not exist.

- [ ] **Step 3: Implement controller and exception handler mapping**

Expose `/api/product-questionnaires/products` CRUD-style endpoints and map `QuestionnaireProductException` to `ApiErrorResponse`.

- [ ] **Step 4: Run controller tests and verify GREEN**

Run: `mvn -Dtest=QuestionnaireProductControllerTest test`

Expected: tests pass.

### Task 3: 模板产品字典和导入引用

**Files:**
- Modify: `src/main/java/com/acme/questionnaire/ref/ImportReferenceData.java`
- Modify: `src/main/java/com/acme/questionnaire/excel/QuestionnaireExcelHeaders.java`
- Modify: `src/main/java/com/acme/questionnaire/service/QuestionnaireDataOverviewExcelService.java`
- Modify: `src/main/resources/mapper/ProductMapper.xml`
- Test: `src/test/java/com/acme/questionnaire/service/QuestionnaireDataOverviewExcelServiceTest.java`

- [ ] **Step 1: Write failing template test**

Add a test that downloads a template with one enabled product and asserts the workbook contains “产品字典” with `产品编码` and `产品型号`.

- [ ] **Step 2: Run template test and verify RED**

Run: `mvn -Dtest=QuestionnaireDataOverviewExcelServiceTest test`

Expected: test fails because the workbook has no product dictionary sheet.

- [ ] **Step 3: Implement product dictionary sheet**

Expose enabled products from `ImportReferenceData`, add `PRODUCT_DICTIONARY_SHEET_NAME`, and write rows from enabled products.

- [ ] **Step 4: Run template test and verify GREEN**

Run: `mvn -Dtest=QuestionnaireDataOverviewExcelServiceTest test`

Expected: test passes.

### Task 4: 文档和全量验证

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update README**

Document product endpoints, product status behavior, and the product dictionary sheet.

- [ ] **Step 2: Run full test suite**

Run: `mvn test`

Expected: all tests pass.

- [ ] **Step 3: Review diff**

Run: `git diff --check` and `git status --short`.

Expected: no whitespace errors; changed files match the product feature scope.
