package com.acme.questionnaire.controller;

import com.acme.questionnaire.dto.QuestionnaireImportResult;
import com.acme.questionnaire.service.QuestionnaireDataOverviewExcelService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 问卷数据概览 Excel 接口。
 *
 * <p>该控制器只负责暴露 HTTP 契约，模板下载和导入的业务规则统一下沉到
 * QuestionnaireDataOverviewExcelService，避免接口层重复维护 Excel 表头、字典快照和校验规则。</p>
 *
 * <p>模板下载接口返回二进制 xlsx 文件，不使用统一 JSON 响应包装；导入接口读取同一套模板规范，
 * 因此模板列、字典页和导入校验必须保持同步演进。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product-questionnaires/data-overview")
public class QuestionnaireDataOverviewExcelController {
    private final QuestionnaireDataOverviewExcelService excelService;

    /**
     * 下载问卷观点导入模板。
     *
     * <p>当前端调用该接口时，后端会根据数据库中的启用产品和启用特性实时生成模板：
     * 第一个工作表用于填写导入数据，其余工作表用于说明规则、展示产品字典和特性字典。
     * 响应头中的 Content-Disposition 由服务层设置，前端应按附件文件名保存。</p>
     */
    @GetMapping("/import-template")
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        excelService.downloadTemplate(response);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public QuestionnaireImportResult importExcel(@RequestParam("file") MultipartFile file) {
        return excelService.importExcel(file);
    }
}
