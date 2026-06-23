package com.acme.questionnaire.service;

import com.acme.questionnaire.config.QuestionnaireImportProperties;
import com.acme.questionnaire.dto.QuestionnaireImportResult;
import com.acme.questionnaire.exception.ExcelImportValidationException;
import com.acme.questionnaire.exception.QuestionnaireImportException;
import com.acme.questionnaire.excel.QuestionnaireExcelHeaders;
import com.acme.questionnaire.excel.QuestionnaireOpinionImportListener;
import com.acme.questionnaire.excel.QuestionnaireTemplateSheetWriteHandler;
import com.acme.questionnaire.ref.FeatureRef;
import com.acme.questionnaire.ref.ImportReferenceData;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.exception.ExcelAnalysisException;
import com.alibaba.excel.write.metadata.WriteSheet;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class QuestionnaireDataOverviewExcelService {
    private final ImportReferenceDataLoader referenceDataLoader;
    private final QuestionnaireImportWriter importWriter;
    private final RecommendCategoryResolver categoryResolver;
    private final QuestionnaireImportProperties properties;
    private final QuestionnaireCacheVersionService cacheVersionService;

    public void downloadTemplate(HttpServletResponse response) throws IOException {
        ImportReferenceData referenceData = referenceDataLoader.load();
        List<FeatureRef> features = referenceData.getEnabledFeatures();
        List<List<String>> dataHead = QuestionnaireExcelHeaders.buildHead(features);

        String rawFileName = "问卷观点导入模板_" + LocalDate.now() + ".xlsx";
        String encodedFileName = URLEncoder.encode(rawFileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename*=UTF-8''" + encodedFileName);
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        int lastColumnIndex = dataHead.size() - 1;
        QuestionnaireTemplateSheetWriteHandler sheetHandler =
                new QuestionnaireTemplateSheetWriteHandler(
                        lastColumnIndex,
                        properties.getMaxDataRows());

        try (ExcelWriter writer = EasyExcel.write(response.getOutputStream()).build()) {
            WriteSheet dataSheet = EasyExcel.writerSheet(
                            0, QuestionnaireExcelHeaders.DATA_SHEET_NAME)
                    .head(dataHead)
                    .registerWriteHandler(sheetHandler)
                    .build();
            writer.write(Collections.emptyList(), dataSheet);

            WriteSheet instructionSheet = EasyExcel.writerSheet(
                            1, QuestionnaireExcelHeaders.INSTRUCTION_SHEET_NAME)
                    .head(List.of(List.of("规则"), List.of("说明")))
                    .build();
            writer.write(buildInstructionRows(), instructionSheet);

            WriteSheet featureSheet = EasyExcel.writerSheet(
                            2, QuestionnaireExcelHeaders.FEATURE_DICTIONARY_SHEET_NAME)
                    .head(List.of(List.of("特性编码"), List.of("特性名称")))
                    .build();
            writer.write(buildFeatureDictionaryRows(features), featureSheet);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public QuestionnaireImportResult importExcel(MultipartFile file) {
        validateUpload(file);
        ImportReferenceData referenceData = referenceDataLoader.load();
        QuestionnaireOpinionImportListener listener =
                new QuestionnaireOpinionImportListener(
                        referenceData,
                        importWriter,
                        categoryResolver,
                        properties);

        try (InputStream inputStream = file.getInputStream()) {
            EasyExcel.read(inputStream, listener)
                    .headRowNumber(1)
                    .ignoreEmptyRow(Boolean.TRUE)
                    .sheet(0)
                    .doRead();
        } catch (ExcelImportValidationException ex) {
            throw ex;
        } catch (ExcelAnalysisException ex) {
            ExcelImportValidationException validationException =
                    findCause(ex, ExcelImportValidationException.class);
            if (validationException != null) {
                throw validationException;
            }
            throw new QuestionnaireImportException("Excel解析失败，请检查文件格式", ex);
        } catch (IOException ex) {
            throw new QuestionnaireImportException("读取上传文件失败", ex);
        }

        cacheVersionService.increaseAfterCommit();
        return listener.getResult();
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ExcelImportValidationException.single(null, null, "请选择要导入的Excel文件");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw ExcelImportValidationException.single(
                    null,
                    null,
                    "文件大小不能超过" + properties.getMaxFileSizeBytes() / 1024 / 1024 + "MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null
                || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw ExcelImportValidationException.single(
                    null,
                    null,
                    "仅支持.xlsx格式文件");
        }
    }

    private List<List<Object>> buildInstructionRows() {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("导入工作表", "只读取第一个工作表“问卷观点导入”"));
        rows.add(List.of("必填字段", "问卷ID、产品型号、产品编码、答卷时间、推荐意愿、情感观点"));
        rows.add(List.of("答卷时间", "建议格式：yyyy-MM-dd HH:mm:ss"));
        rows.add(List.of("评分范围", "推荐意愿及所有特性评分均为1-10的整数"));
        rows.add(List.of("用户归类", "可留空，系统将按推荐意愿计算；填写时必须与系统计算结果一致"));
        rows.add(List.of("多观点问卷", "同一问卷的多条观点必须连续排列，且问卷级字段、各特性评分必须完全一致"));
        rows.add(List.of("不适用特性", "产品不涉及某特性时，对应特性评分列留空"));
        rows.add(List.of("特性分类", "填写“特性字典”工作表中的特性编码；无法归类时可留空"));
        rows.add(List.of("覆盖规则", "再次导入相同问卷ID时，覆盖答卷信息并整体替换原特性评分和观点"));
        rows.add(List.of("事务规则", "任一行校验失败时，整个文件不入库"));
        return rows;
    }

    private List<List<Object>> buildFeatureDictionaryRows(List<FeatureRef> features) {
        List<List<Object>> rows = new ArrayList<>(features.size());
        for (FeatureRef feature : features) {
            rows.add(List.of(feature.getFeatureCode(), feature.getFeatureName()));
        }
        return rows;
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
