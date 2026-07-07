package com.acme.questionnaire.service;

import com.acme.questionnaire.config.QuestionnaireImportProperties;
import com.acme.questionnaire.excel.QuestionnaireExcelHeaders;
import com.acme.questionnaire.model.AnswerAggregate;
import com.acme.questionnaire.model.UserCategory;
import com.acme.questionnaire.ref.FeatureRef;
import com.acme.questionnaire.ref.ImportReferenceData;
import com.acme.questionnaire.ref.ProductFeatureRef;
import com.acme.questionnaire.ref.ProductRef;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionnaireDataOverviewExcelServiceTest {
    @Mock
    private ImportReferenceDataLoader referenceDataLoader;

    @Mock
    private QuestionnaireImportWriter importWriter;

    @Mock
    private RecommendCategoryResolver categoryResolver;

    @Mock
    private QuestionnaireCacheVersionService cacheVersionService;

    @Test
    void downloadTemplateWritesProductDictionarySheet() throws Exception {
        when(referenceDataLoader.load()).thenReturn(new ImportReferenceData(
                List.of(feature(1L, "BATTERY", "续航", 10)),
                List.of(product(10L, "P100", "Alpha")),
                List.of()));
        QuestionnaireDataOverviewExcelService service =
                new QuestionnaireDataOverviewExcelService(
                        referenceDataLoader,
                        importWriter,
                        categoryResolver,
                        new QuestionnaireImportProperties(),
                        cacheVersionService);
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.downloadTemplate(response);

        try (Workbook workbook = WorkbookFactory.create(
                new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Sheet productSheet = workbook.getSheet("产品字典");
            assertThat(productSheet).isNotNull();

            Row headerRow = productSheet.getRow(0);
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("产品编码");
            assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("产品型号");

            Row firstDataRow = productSheet.getRow(1);
            assertThat(firstDataRow.getCell(0).getStringCellValue()).isEqualTo("P100");
            assertThat(firstDataRow.getCell(1).getStringCellValue()).isEqualTo("Alpha");
        }
    }

    @Test
    void downloadTemplateUsesFeatureNameForOpinionCategoryColumn() throws Exception {
        when(referenceDataLoader.load()).thenReturn(new ImportReferenceData(
                List.of(feature(1L, "BATTERY", "续航", 10)),
                List.of(product(10L, "P100", "Alpha")),
                List.of()));
        QuestionnaireDataOverviewExcelService service =
                new QuestionnaireDataOverviewExcelService(
                        referenceDataLoader,
                        importWriter,
                        categoryResolver,
                        new QuestionnaireImportProperties(),
                        cacheVersionService);
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.downloadTemplate(response);

        try (Workbook workbook = WorkbookFactory.create(
                new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Sheet dataSheet = workbook.getSheet("问卷观点导入");
            assertThat(dataSheet.getRow(0)
                    .getCell(QuestionnaireExcelHeaders.OPINION_FEATURE_NAME)
                    .getStringCellValue()).isEqualTo("特性分类名称");

            Sheet instructionSheet = workbook.getSheet("填写说明");
            assertThat(instructionSheet).isNotNull();
            assertThat(instructionSheet.getRow(9).getCell(1).getStringCellValue())
                    .contains("任意分类文本")
                    .doesNotContain("特性编码");

            Sheet featureSheet = workbook.getSheet("特性字典");
            assertThat(featureSheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("特性名称");
            assertThat(featureSheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("续航");
        }
    }

    @Test
    void downloadTemplateHighlightsOpinionHeadersAndGreysOtherHeaders() throws Exception {
        when(referenceDataLoader.load()).thenReturn(new ImportReferenceData(
                List.of(feature(1L, "BATTERY", "续航", 10)),
                List.of(product(10L, "P100", "Alpha")),
                List.of()));
        QuestionnaireDataOverviewExcelService service =
                new QuestionnaireDataOverviewExcelService(
                        referenceDataLoader,
                        importWriter,
                        categoryResolver,
                        new QuestionnaireImportProperties(),
                        cacheVersionService);
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.downloadTemplate(response);

        try (Workbook workbook = WorkbookFactory.create(
                new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Row headerRow = workbook.getSheet("问卷观点导入").getRow(0);

            assertHeaderFillColor(headerRow, QuestionnaireExcelHeaders.USER_CATEGORY, IndexedColors.YELLOW);
            assertHeaderFillColor(headerRow, QuestionnaireExcelHeaders.SENTIMENT, IndexedColors.YELLOW);
            assertHeaderFillColor(headerRow, QuestionnaireExcelHeaders.OPINION_FEATURE_NAME, IndexedColors.YELLOW);
            assertHeaderFillColor(headerRow, QuestionnaireExcelHeaders.FEEDBACK_CONTENT_1, IndexedColors.YELLOW);
            assertHeaderFillColor(headerRow, QuestionnaireExcelHeaders.FEEDBACK_CONTENT_2, IndexedColors.YELLOW);

            assertHeaderFillColor(headerRow, QuestionnaireExcelHeaders.QUESTIONNAIRE_ID, IndexedColors.GREY_25_PERCENT);
            assertHeaderFillColor(headerRow, QuestionnaireExcelHeaders.RECOMMEND_SCORE, IndexedColors.GREY_25_PERCENT);
            assertHeaderFillColor(headerRow, QuestionnaireExcelHeaders.fixedHeaderCount(), IndexedColors.GREY_25_PERCENT);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void importExcelStoresArbitraryOpinionFeatureCategoryName() throws Exception {
        FeatureRef feature = feature(1L, "BATTERY", "续航", 10);
        ProductRef product = product(10L, "P100", "Alpha");
        when(referenceDataLoader.load()).thenReturn(new ImportReferenceData(
                List.of(feature),
                List.of(product),
                List.of()));
        when(categoryResolver.resolve(9)).thenReturn(UserCategory.PROMOTER);
        when(importWriter.saveBatch(any())).thenReturn(new BatchWriteCount(1, 1, 0));
        QuestionnaireDataOverviewExcelService service =
                new QuestionnaireDataOverviewExcelService(
                        referenceDataLoader,
                        importWriter,
                        categoryResolver,
                        new QuestionnaireImportProperties(),
                        cacheVersionService);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "import.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes(List.of(feature), List.of(rowValues("物流包装", ""))));

        service.importExcel(file);

        ArgumentCaptor<List<AnswerAggregate>> captor = ArgumentCaptor.forClass(List.class);
        verify(importWriter).saveBatch(captor.capture());
        AnswerAggregate aggregate = captor.getValue().get(0);
        assertThat(aggregate.getOpinions()).singleElement()
                .satisfies(opinion -> assertThat(opinion.getFeatureCategoryName())
                        .isEqualTo("物流包装"));
    }

    private FeatureRef feature(Long id, String code, String name, Integer sortNo) {
        FeatureRef feature = new FeatureRef();
        feature.setId(id);
        feature.setFeatureCode(code);
        feature.setFeatureName(name);
        feature.setSortNo(sortNo);
        return feature;
    }

    private ProductRef product(Long id, String code, String model) {
        ProductRef product = new ProductRef();
        product.setId(id);
        product.setProductCode(code);
        product.setProductModel(model);
        return product;
    }

    private void assertHeaderFillColor(Row headerRow, int columnIndex, IndexedColors expectedColor) {
        Cell cell = headerRow.getCell(columnIndex);
        assertThat(cell.getCellStyle().getFillPattern()).isEqualTo(FillPatternType.SOLID_FOREGROUND);
        assertThat(cell.getCellStyle().getFillForegroundColor()).isEqualTo(expectedColor.getIndex());
    }

    private byte[] workbookBytes(List<FeatureRef> features, List<List<String>> rows) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(QuestionnaireExcelHeaders.DATA_SHEET_NAME);
            Row headerRow = sheet.createRow(0);
            List<List<String>> head = QuestionnaireExcelHeaders.buildHead(features);
            for (int columnIndex = 0; columnIndex < head.size(); columnIndex++) {
                headerRow.createCell(columnIndex).setCellValue(head.get(columnIndex).get(0));
            }
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                List<String> values = rows.get(rowIndex);
                for (int columnIndex = 0; columnIndex < values.size(); columnIndex++) {
                    row.createCell(columnIndex).setCellValue(values.get(columnIndex));
                }
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private List<String> rowValues(String opinionFeatureName, String featureScore) {
        List<String> row = new ArrayList<>(QuestionnaireExcelHeaders.fixedHeaderCount() + 1);
        row.add("Q001");
        row.add("Alpha");
        row.add("P100");
        row.add("2026-06-25 10:00:00");
        row.add("R1");
        row.add("A1");
        row.add("续航很好");
        row.add("体验稳定");
        row.add("9");
        row.add("");
        row.add("好评");
        row.add(opinionFeatureName);
        row.add("电池很耐用");
        row.add("");
        row.add(featureScore);
        return row;
    }
}
