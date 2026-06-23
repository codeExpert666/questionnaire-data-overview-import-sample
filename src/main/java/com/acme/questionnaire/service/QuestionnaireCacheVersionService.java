package com.acme.questionnaire.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 问卷数据缓存版本维护组件。
 *
 * <p>当前项目不把产品、特性、模板内容或导入结果写入 Redis。Redis 只保存一个全局版本号，
 * 用于向前端、网关或外部缓存层表达“问卷相关数据已经发生变化”。消费者如果缓存了产品字典、
 * 特性字典、模板元数据或数据概览结果，可以通过比较该版本号判断是否需要刷新。</p>
 *
 * <p>调用方只应在会影响模板生成、导入校验或数据概览读取结果的写操作成功路径调用
 * {@link #increaseAfterCommit()}。读操作不需要递增版本；写操作如果最终回滚，也不应该让
 * 外部缓存误以为数据已经变化。</p>
 *
 * <p>一致性规则：当当前线程存在 Spring 事务同步时，版本递增注册到事务提交后的
 * {@link TransactionSynchronization#afterCommit()} 回调中执行；没有事务时才立即执行。
 * Redis 更新失败不会回滚数据库提交，只记录警告日志，因为 Redis 在这里是缓存失效信号，
 * 不是业务数据的持久化来源。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionnaireCacheVersionService {
    /**
     * 问卷相关数据的全局版本号 key。
     *
     * <p>该 key 的值只表达“发生过几次成功变更”，不承载任何产品、特性、模板或答卷内容。
     * 接入公司现有系统时，应按统一缓存命名规范替换该 key，并保证所有消费者读取同一个版本源。</p>
     */
    private static final String CACHE_VERSION_KEY = "pq:data-version";

    /**
     * Spring Boot 基于 spring.data.redis 自动配置的字符串 Redis 客户端。
     *
     * <p>这里使用 {@link StringRedisTemplate} 是因为只需要对字符串 key 做原子 INCR，
     * 不涉及对象序列化、Hash 结构或复杂缓存模型。</p>
     */
    private final StringRedisTemplate redisTemplate;

    /**
     * 在当前数据库事务提交后递增问卷数据版本。
     *
     * <p>业务服务通常在完成 MySQL 写入之后调用该方法。若调用发生在
     * {@code @Transactional} 方法内，真正的 Redis INCR 会推迟到事务成功提交之后；
     * 如果事务回滚，{@code afterCommit} 不会触发，外部缓存也不会收到错误的刷新信号。</p>
     *
     * <p>该方法只负责发布版本变更信号，不读取也不刷新任何实际业务缓存。新增真实缓存时，
     * 应由缓存消费者基于该版本号决定是否重新加载数据，而不是把业务数据直接放入本组件。</p>
     */
    public void increaseAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            increaseSafely();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                increaseSafely();
            }
        });
    }

    /**
     * 执行 Redis 版本号递增。
     *
     * <p>{@code INCR} 是 Redis 原子操作，适合多个实例同时提交变更时共同推进同一个版本号。
     * 捕获异常是有意设计：数据库事务已经成功提交时，Redis 版本更新失败不应再影响业务结果；
     * 运维或调用方可以通过日志发现该失效信号缺失，并按需要补偿刷新外部缓存。</p>
     */
    private void increaseSafely() {
        try {
            redisTemplate.opsForValue().increment(CACHE_VERSION_KEY);
        } catch (Exception ex) {
            log.warn("问卷缓存版本递增失败，数据库数据已成功提交", ex);
        }
    }
}
