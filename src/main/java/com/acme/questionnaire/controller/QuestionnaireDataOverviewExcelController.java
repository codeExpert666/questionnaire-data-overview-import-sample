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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product-questionnaires/data-overview")
public class QuestionnaireDataOverviewExcelController {
    private final QuestionnaireDataOverviewExcelService excelService;

    @GetMapping("/import-template")
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        excelService.downloadTemplate(response);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public QuestionnaireImportResult importExcel(@RequestParam("file") MultipartFile file) {
        return excelService.importExcel(file);
    }
}
