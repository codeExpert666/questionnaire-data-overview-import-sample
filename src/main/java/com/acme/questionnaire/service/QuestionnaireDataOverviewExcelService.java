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
import com.acme.questionnaire.ref.ProductRef;
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

/**
 * 问卷观点 Excel 模板下载和导入服务。
 *
 * <p>pq_feature 在这里影响两个位置：下载模板时生成所有启用特性的动态评分列和特性字典页；
 * 导入时用当前启用特性重新校验模板表头，并将有效评分写入 pq_answer_feature_score。</p>
 *
 * <p>pq_product 在这里影响产品字典和行级产品解析：下载模板时只输出启用产品；导入时用
 * productCode 查找启用产品，再要求 productModel 与当前字典一致，最后写入 pq_answer.product_id。</p>
 *
 * <p>导入成功后会递增 Redis 数据版本，表示答卷和数据概览相关结果可能已经变化。该版本号
 * 只供外部缓存消费者判断是否刷新；本服务下载模板、读取参考数据和写入导入结果都直接访问
 * MySQL，不从 Redis 读取实际业务数据。</p>
 */
@Service
@RequiredArgsConstructor
public class QuestionnaireDataOverviewExcelService {
    private final ImportReferenceDataLoader referenceDataLoader;
    private final QuestionnaireImportWriter importWriter;
    private final RecommendCategoryResolver categoryResolver;
    private final QuestionnaireImportProperties properties;
    private final QuestionnaireCacheVersionService cacheVersionService;

    /**
     * 下载导入模板。
     *
     * <p>模板的动态评分列来自 pq_feature.status=1 的全量特性，按 sort_no、id 稳定排序。
     * 模板不按产品裁剪列；用户填写时通过“产品不涉及某特性时留空”的规则处理。
     * “产品字典”工作表来自 pq_product.status=1，供用户复制产品编码和型号。</p>
     *
     * <p>生成的工作簿固定包含四个工作表：第一个“问卷观点导入”是唯一会被导入流程读取的数据页；
     * “填写说明”给出面向填表人的规则摘要；“产品字典”和“特性字典”是当前数据库快照，
     * 用于降低手工填写主数据值时的出错率。后续如果新增工作表，应保持数据页仍位于 index=0，
     * 否则导入读取 sheet(0) 的契约会被破坏。</p>
     *
     * <p>该方法直接写入 HttpServletResponse 输出流，并设置浏览器下载所需的 Content-Type、
     * Content-Disposition 和 Access-Control-Expose-Headers。调用方不要再对响应体做 JSON 包装，
     * 也不要在写出后追加其他内容。</p>
     */
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        ImportReferenceData referenceData = referenceDataLoader.load();
        List<FeatureRef> features = referenceData.getEnabledFeatures();
        List<List<String>> dataHead = QuestionnaireExcelHeaders.buildHead(features);

        String rawFileName = "问卷观点导入模板_" + LocalDate.now() + ".xlsx";
        String encodedFileName = URLEncoder.encode(rawFileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        // 使用 RFC 5987 的 filename*=UTF-8'' 格式，避免中文文件名在浏览器下载时乱码。
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
            // 数据页必须保持第 0 个工作表；导入流程只读取该 sheet，并以第一行作为表头。
            WriteSheet dataSheet = EasyExcel.writerSheet(
                            0, QuestionnaireExcelHeaders.DATA_SHEET_NAME)
                    .head(dataHead)
                    .registerWriteHandler(sheetHandler)
                    .build();
            writer.write(Collections.emptyList(), dataSheet);

            // 说明页只提供填表规则提示，真正的强校验仍以导入监听器和数据库快照为准。
            WriteSheet instructionSheet = EasyExcel.writerSheet(
                            1, QuestionnaireExcelHeaders.INSTRUCTION_SHEET_NAME)
                    .head(List.of(List.of("规则"), List.of("说明")))
                    .build();
            writer.write(buildInstructionRows(), instructionSheet);

            // 产品字典页只输出启用产品；停用产品即使历史数据存在，也不再允许被新模板引用。
            WriteSheet productSheet = EasyExcel.writerSheet(
                            2, QuestionnaireExcelHeaders.PRODUCT_DICTIONARY_SHEET_NAME)
                    .head(List.of(List.of("产品编码"), List.of("产品型号")))
                    .build();
            writer.write(buildProductDictionaryRows(referenceData.getEnabledProducts()), productSheet);

            // 特性字典页与动态评分列表头使用同一份启用特性快照，便于复制特性分类名称。
            WriteSheet featureSheet = EasyExcel.writerSheet(
                            3, QuestionnaireExcelHeaders.FEATURE_DICTIONARY_SHEET_NAME)
                    .head(List.of(List.of("特性名称"), List.of("特性编码")))
                    .build();
            writer.write(buildFeatureDictionaryRows(features), featureSheet);
        }
    }

    /**
     * 导入问卷观点 Excel。
     *
     * <p>导入开始时读取当前 pq_feature 快照；如果上传文件中的动态评分列表头与当前启用特性不一致，
     * 会整体拒绝导入并提示重新下载模板。pq_product 快照只包含启用产品，停用产品编码会被视为
     * 不可引用。数据库写入发生在事务内，任一行失败则整个文件不入库。</p>
     *
     * <p>EasyExcel 只读取第 0 个工作表，并把第一行作为表头。监听器负责在读取过程中累积行级错误；
     * 一旦达到错误上限会提前中断，避免大文件持续占用内存。只有监听器完整读完且没有错误时才会
     * 执行批量写库，随后在事务提交后递增缓存版本。</p>
     */
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
            // 导入契约固定为 sheet(0) + 第一行表头；其他说明和字典页只供用户阅读，不参与解析。
            EasyExcel.read(inputStream, listener)
                    .headRowNumber(1)
                    .ignoreEmptyRow(Boolean.TRUE)
                    .sheet(0)
                    .doRead();
        } catch (ExcelImportValidationException ex) {
            // 业务校验异常已经带有行号、列名和错误明细，直接向上抛给异常处理器。
            throw ex;
        } catch (ExcelAnalysisException ex) {
            // EasyExcel 可能把监听器抛出的业务异常包装起来，先剥离真实原因，避免误报系统解析失败。
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

    /**
     * 校验上传文件的基础约束。
     *
     * <p>这里仅处理不需要打开工作簿的轻量检查：文件存在、大小限制和扩展名。模板表头、sheet 内容、
     * 日期和业务字典等规则必须进入 EasyExcel 监听器后校验，才能返回准确行列位置。</p>
     */
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

    /**
     * 构造模板说明页。
     *
     * <p>其中“不适用特性”和“特性分类”规则分别对应动态评分列与固定列特性分类名称的校验。</p>
     *
     * <p>说明页面向填表用户，不作为程序解析来源；如果规则文案需要调整，必须同步核对
     * QuestionnaireOpinionImportListener 中的真实校验逻辑，避免说明与实际行为不一致。</p>
     */
    private List<List<Object>> buildInstructionRows() {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("导入工作表", "只读取第一个工作表“问卷观点导入”"));
        rows.add(List.of("必填字段", "问卷ID、产品型号、产品编码、答卷时间、推荐意愿、情感观点"));
        rows.add(List.of("答卷时间", "建议格式：yyyy-MM-dd HH:mm:ss"));
        rows.add(List.of("评分范围", "推荐意愿及所有特性评分均为1-10的整数"));
        rows.add(List.of("用户归类", "可留空，系统将按推荐意愿计算；填写时必须与系统计算结果一致"));
        rows.add(List.of("多观点问卷", "同一问卷的多条观点必须连续排列，且问卷级字段、各特性评分必须完全一致"));
        rows.add(List.of("产品信息", "填写“产品字典”工作表中的产品编码和产品型号"));
        rows.add(List.of("不适用特性", "产品不涉及某特性时，对应特性评分列留空"));
        rows.add(List.of("特性分类", "填写“特性字典”工作表中的特性名称；无法归类时可留空"));
        rows.add(List.of("覆盖规则", "再次导入相同问卷ID时，覆盖答卷信息并整体替换原特性评分和观点"));
        rows.add(List.of("事务规则", "任一行校验失败时，整个文件不入库"));
        return rows;
    }

    /**
     * 构造产品字典页。
     *
     * <p>只输出当前启用产品，供用户填写“产品编码”和“产品型号”固定列时参考。导入时以
     * 产品编码为主键匹配，并校验产品型号是否等于这里展示的当前值。</p>
     *
     * <p>这里不输出数据库主键、状态和时间戳，避免模板使用者依赖内部字段；模板和导入文件中的
     * 对外稳定标识始终是 productCode。</p>
     */
    private List<List<Object>> buildProductDictionaryRows(List<ProductRef> products) {
        List<List<Object>> rows = new ArrayList<>(products.size());
        for (ProductRef product : products) {
            rows.add(List.of(product.getProductCode(), product.getProductModel()));
        }
        return rows;
    }

    /**
     * 构造特性字典页。
     *
     * <p>只输出当前启用特性的名称和编码，供用户填写“特性分类名称”固定列时参考。</p>
     *
     * <p>动态评分列表头也使用该字典页的特性名称追加“体验”；特性编码保留为后台/API 稳定标识，
     * 不作为用户导入时的分类填写值。</p>
     */
    private List<List<Object>> buildFeatureDictionaryRows(List<FeatureRef> features) {
        List<List<Object>> rows = new ArrayList<>(features.size());
        for (FeatureRef feature : features) {
            rows.add(List.of(feature.getFeatureName(), feature.getFeatureCode()));
        }
        return rows;
    }

    /**
     * 从第三方解析异常链中查找业务异常。
     *
     * <p>EasyExcel 回调抛出的异常常被包装为 ExcelAnalysisException。导入接口需要保留业务错误明细，
     * 因此不能只看最外层异常类型。</p>
     */
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
