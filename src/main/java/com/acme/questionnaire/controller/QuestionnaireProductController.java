package com.acme.questionnaire.controller;

import com.acme.questionnaire.dto.ProductCreateRequest;
import com.acme.questionnaire.dto.ProductResponse;
import com.acme.questionnaire.dto.ProductStatusRequest;
import com.acme.questionnaire.dto.ProductUpdateRequest;
import com.acme.questionnaire.service.QuestionnaireProductService;
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

/**
 * pq_product 产品型号字典管理接口。
 *
 * <p>该接口只维护产品自身：产品编码、产品型号和启停状态。产品与特性的适用关系
 * 仍由 pq_product_feature 维护，不在本接口范围内。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product-questionnaires/products")
public class QuestionnaireProductController {
    private final QuestionnaireProductService productService;

    /**
     * 查询全部产品，包含启用和停用数据。
     */
    @GetMapping
    public List<ProductResponse> listProducts() {
        return productService.listProducts();
    }

    /**
     * 创建产品型号字典项。
     */
    @PostMapping
    public ProductResponse createProduct(@RequestBody ProductCreateRequest request) {
        return productService.createProduct(request);
    }

    /**
     * 修改产品型号展示名。
     */
    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable Long id,
                                         @RequestBody ProductUpdateRequest request) {
        return productService.updateProduct(id, request);
    }

    /**
     * 启用或停用产品。
     */
    @PatchMapping("/{id}/status")
    public ProductResponse changeStatus(@PathVariable Long id,
                                        @RequestBody ProductStatusRequest request) {
        return productService.changeStatus(id, request);
    }

    /**
     * 软删除产品。
     */
    @DeleteMapping("/{id}")
    public ProductResponse deleteProduct(@PathVariable Long id) {
        return productService.deleteProduct(id);
    }
}
