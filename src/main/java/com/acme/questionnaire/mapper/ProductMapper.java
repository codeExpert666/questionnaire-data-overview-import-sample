package com.acme.questionnaire.mapper;

import com.acme.questionnaire.ref.ProductRef;

import java.util.List;

public interface ProductMapper {
    List<ProductRef> selectAllProducts();
}
