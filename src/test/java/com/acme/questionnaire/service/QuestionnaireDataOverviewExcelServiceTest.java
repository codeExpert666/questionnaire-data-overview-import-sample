package com.acme.questionnaire.service;

import com.acme.questionnaire.config.QuestionnaireImportProperties;
import com.acme.questionnaire.ref.FeatureRef;
import com.acme.questionnaire.ref.ImportReferenceData;
import com.acme.questionnaire.ref.ProductRef;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
}
