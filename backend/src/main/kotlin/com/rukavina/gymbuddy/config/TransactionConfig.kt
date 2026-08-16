package com.rukavina.gymbuddy.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * One TransactionTemplate bean, shared by every per-type sync service.
 * Programmatic transactions (not @Transactional) because push applies
 * each entity in its own transaction inside a loop over the batch -
 * @Transactional's proxy-per-method-call model doesn't fit a "commit
 * this one, then start a fresh one for the next" loop within a single
 * request, and self-invocation from within the same class wouldn't go
 * through the proxy at all.
 */
@Configuration
class TransactionConfig {

    @Bean
    fun transactionTemplate(transactionManager: PlatformTransactionManager): TransactionTemplate =
        TransactionTemplate(transactionManager)
}
