# Auto Questionnaire Codes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let product and feature creation automatically assign stable codes when the caller omits `productCode` or `featureCode`.

**Architecture:** Keep the behavior inside the two service classes. Reuse the existing insert-and-reload flow, add mapper methods that update only the stable code after the generated database id is known, and preserve existing validation when callers provide codes.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis XML mappers, JUnit 5, Mockito, AssertJ.

---

### Task 1: Product Auto Code

**Files:**
- Modify: `src/test/java/com/acme/questionnaire/service/QuestionnaireProductServiceTest.java`
- Modify: `src/main/java/com/acme/questionnaire/service/QuestionnaireProductService.java`
- Modify: `src/main/java/com/acme/questionnaire/mapper/ProductMapper.java`
- Modify: `src/main/resources/mapper/ProductMapper.xml`

- [ ] **Step 1: Write the failing service test**

Add a test that calls `createProduct(new ProductCreateRequest(null, " Alpha ", null))`, captures the inserted row, captures the code update row, and expects the final response code to be `P42`.

- [ ] **Step 2: Run the product service test and verify it fails**

Run: `mvn -Dtest=QuestionnaireProductServiceTest test`

Expected: compilation or verification failure because `updateProductCode` does not exist or is not called.

- [ ] **Step 3: Implement product code generation**

Add `ProductMapper.updateProductCode(QuestionnaireProduct product)` and XML update SQL:

```sql
UPDATE pq_product
SET product_code = #{productCode}
WHERE id = #{id}
```

In `QuestionnaireProductService#createProduct`, treat a blank `productCode` as omitted, insert with a generated temporary code, then update the generated final code `P{id}` before reloading.

- [ ] **Step 4: Run the product service test and verify it passes**

Run: `mvn -Dtest=QuestionnaireProductServiceTest test`

Expected: tests pass.

### Task 2: Feature Auto Code

**Files:**
- Modify: `src/test/java/com/acme/questionnaire/service/QuestionnaireFeatureServiceTest.java`
- Modify: `src/main/java/com/acme/questionnaire/service/QuestionnaireFeatureService.java`
- Modify: `src/main/java/com/acme/questionnaire/mapper/FeatureMapper.java`
- Modify: `src/main/resources/mapper/FeatureMapper.xml`

- [ ] **Step 1: Write the failing service test**

Add a test that calls `createFeature(new FeatureCreateRequest(" ", " 续航 ", 10, null))`, captures the inserted row, captures the code update row, and expects the final response code to be `F42`.

- [ ] **Step 2: Run the feature service test and verify it fails**

Run: `mvn -Dtest=QuestionnaireFeatureServiceTest test`

Expected: compilation or verification failure because `updateFeatureCode` does not exist or is not called.

- [ ] **Step 3: Implement feature code generation**

Add `FeatureMapper.updateFeatureCode(QuestionnaireFeature feature)` and XML update SQL:

```sql
UPDATE pq_feature
SET feature_code = #{featureCode}
WHERE id = #{id}
```

In `QuestionnaireFeatureService#createFeature`, treat a blank `featureCode` as omitted, insert with a generated temporary code, then update the generated final code `F{id}` before reloading.

- [ ] **Step 4: Run the feature service test and verify it passes**

Run: `mvn -Dtest=QuestionnaireFeatureServiceTest test`

Expected: tests pass.

### Task 3: Documentation and Full Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update API docs**

Document that `productCode` and `featureCode` are optional on creation, and that omitted values are assigned as `P{id}` and `F{id}`.

- [ ] **Step 2: Run full test suite**

Run: `mvn test`

Expected: build success, 0 failures, 0 errors.
