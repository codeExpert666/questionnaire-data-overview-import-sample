package com.acme.questionnaire.dto;

/**
 * 单列排序请求。
 *
 * <p>field 必须是服务层白名单中的响应字段名，评分页支持固定字段以及
 * featureScore:{featureId} 动态评分字段。direction 只接受 asc 或 desc，大小写不敏感；
 * 为空时默认升序。SQL 表达式不会直接使用调用方传入的 field，而是由服务层映射生成。</p>
 */
public record TableSortRequest(
        String field,
        String direction
) {
}
