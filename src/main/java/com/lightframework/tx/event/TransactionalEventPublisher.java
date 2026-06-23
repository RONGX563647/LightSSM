package com.lightframework.tx.event;

import com.lightframework.di.annotation.Component;
import com.lightframework.ioc.core.ApplicationContextAware;
import com.lightframework.ioc.context.ApplicationContext;
import com.lightframework.ioc.event.ApplicationEvent;
import com.lightframework.ioc.event.ApplicationEventPublisher;
import com.lightframework.tx.core.TransactionSynchronization;
import com.lightframework.tx.core.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class TransactionalEventPublisher implements ApplicationEventPublisher, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(TransactionalEventPublisher.class);

    private ApplicationEventPublisher delegate;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        if (applicationContext instanceof ApplicationEventPublisher) {
            delegate = (ApplicationEventPublisher) applicationContext;
        }
    }

    @Override
    public void publishEvent(ApplicationEvent event) {
        publishEvent(event, TransactionPhase.AFTER_COMMIT);
    }

    public void publishEvent(ApplicationEvent event, TransactionPhase phase) {
        if (TransactionSynchronizationManager.isActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void beforeCommit(boolean readOnly) throws Exception {
                        if (phase == TransactionPhase.BEFORE_COMMIT) {
                            delegate.publishEvent(event);
                        }
                    }
                    @Override
                    public void afterCompletion(int status) {
                        boolean shouldPublish =
                            (phase == TransactionPhase.AFTER_COMPLETION) ||
                            (phase == TransactionPhase.AFTER_COMMIT && status == STATUS_COMMITTED) ||
                            (phase == TransactionPhase.AFTER_ROLLBACK && status == STATUS_ROLLED_BACK);
                        if (shouldPublish) {
                            delegate.publishEvent(event);
                        }
                    }
                });
            if (logger.isTraceEnabled()) {
                logger.trace("Deferred event publication: {} (phase={})", event.getClass().getSimpleName(), phase);
            }
        } else {
            delegate.publishEvent(event);
        }
    }
}
