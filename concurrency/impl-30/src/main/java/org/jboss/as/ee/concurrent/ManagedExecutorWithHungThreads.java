/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent;

import org.glassfish.enterprise.concurrent.AbstractManagedThread;
import org.wildfly.concurrency.logging.ConcurrencyLogger;

import java.util.Collection;

/**
 * A managed executor with support for hung threads.
 * @author emmartins
 */
public interface ManagedExecutorWithHungThreads {

    /**
     *
     * @return the executor's name
     */
    String getName();

    /**
     * Attempts to terminate the executor's hung tasks, by cancelling such tasks.
     * @return the number of hung tasks cancelled
     */
    default void terminateHungTasks() {
        final String executorName = getClass().getSimpleName() + ":" + getName();
        ConcurrencyLogger.ROOT_LOGGER.debugf("Cancelling %s hung tasks...", executorName);
        final Collection<AbstractManagedThread> hungThreads = getHungThreads();
        if (hungThreads != null) {
            for (AbstractManagedThread t : hungThreads) {
                final String taskIdentityName = t.getTaskIdentityName();
                try {
                    if (t instanceof ManagedThreadFactoryImpl.ManagedThread) {
                        if (((ManagedThreadFactoryImpl.ManagedThread)t).cancelTask()) {
                            ConcurrencyLogger.ROOT_LOGGER.hungTaskCancelled(executorName, taskIdentityName);
                        } else {
                            ConcurrencyLogger.ROOT_LOGGER.hungTaskNotCancelled(executorName, taskIdentityName);
                        }
                    }
                } catch (Throwable throwable) {
                    ConcurrencyLogger.ROOT_LOGGER.huntTaskTerminationFailure(throwable, executorName, taskIdentityName);
                }
            }
        }
    }

    /**
     *
     * @return the executor's hung threads
     */
    Collection<AbstractManagedThread> getHungThreads();
}
