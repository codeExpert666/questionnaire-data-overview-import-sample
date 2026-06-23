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
 *
 * <p>接口返回原始产品字典对象，不包装统一响应体，便于示例项目接入到不同公司的
 * ControllerAdvice 或网关响应规范。业务错误由 QuestionnaireProductException
 * 交给全局异常处理转换为 JSON。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product-questionnaires/products")
public class QuestionnaireProductController {
    private final QuestionnaireProductService productService;

    /**
     * 查询全部产品，包含启用和停用数据。
     *
     * <p>用于配置页面展示完整字典；停用产品不会出现在新下载的导入模板中。</p>
     */
    @GetMapping
    public List<ProductResponse> listProducts() {
        return productService.listProducts();
    }

    /**
     * 创建产品型号字典项。
     *
     * <p>请求体中的 productCode 是稳定编码，创建成功后只能通过新增产品替换，不能通过
     * 更新接口改写。</p>
     */
    @PostMapping
    public ProductResponse createProduct(@RequestBody ProductCreateRequest request) {
        return productService.createProduct(request);
    }

    /**
     * 修改产品型号展示名。
     *
     * <p>仅修改 productModel，不接收 productCode，防止误把稳定编码当展示字段维护。</p>
     */
    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable Long id,
                                         @RequestBody ProductUpdateRequest request) {
        return productService.updateProduct(id, request);
    }

    /**
     * 启用或停用产品。
     *
     * <p>status=1 表示允许进入模板和新导入；status=0 表示从新业务入口隐藏。</p>
     */
    @PatchMapping("/{id}/status")
    public ProductResponse changeStatus(@PathVariable Long id,
                                        @RequestBody ProductStatusRequest request) {
        return productService.changeStatus(id, request);
    }

    /**
     * 软删除产品。
     *
     * <p>等价于停用产品，不做物理删除。</p>
     */
    @DeleteMapping("/{id}")
    public ProductResponse deleteProduct(@PathVariable Long id) {
        return productService.deleteProduct(id);
    }
}
