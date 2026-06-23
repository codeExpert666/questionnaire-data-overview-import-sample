package com.acme.questionnaire.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionnaireCacheVersionService {
    private static final String CACHE_VERSION_KEY = "pq:data-version";
    private final StringRedisTemplate redisTemplate;

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

    private void increaseSafely() {
        try {
            redisTemplate.opsForValue().increment(CACHE_VERSION_KEY);
        } catch (Exception ex) {
            log.warn("问卷缓存版本递增失败，数据库数据已成功提交", ex);
        }
    }
}
