package com.acme.questionnaire.controller;

import com.acme.questionnaire.dto.FeatureCreateRequest;
import com.acme.questionnaire.dto.FeatureResponse;
import com.acme.questionnaire.dto.FeatureStatusRequest;
import com.acme.questionnaire.dto.FeatureUpdateRequest;
import com.acme.questionnaire.service.QuestionnaireFeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product-questionnaires/features")
public class QuestionnaireFeatureController {
    private final QuestionnaireFeatureService featureService;

    @GetMapping
    public List<FeatureResponse> listFeatures() {
        return featureService.listFeatures();
    }

    @PostMapping
    public FeatureResponse createFeature(@RequestBody FeatureCreateRequest request) {
        return featureService.createFeature(request);
    }

    @PutMapping("/{id}")
    public FeatureResponse updateFeature(@PathVariable Long id,
                                         @RequestBody FeatureUpdateRequest request) {
        return featureService.updateFeature(id, request);
    }

    @PatchMapping("/{id}/status")
    public FeatureResponse changeStatus(@PathVariable Long id,
                                        @RequestBody FeatureStatusRequest request) {
        return featureService.changeStatus(id, request);
    }

    @DeleteMapping("/{id}")
    public FeatureResponse deleteFeature(@PathVariable Long id) {
        return featureService.deleteFeature(id);
    }
}
