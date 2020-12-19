/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.ee.concurrent;

import org.jboss.as.ee.logging.EeLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author emmartins
 */
public class ManagedExecutorHungTasksPeriodicTermination {

    public static final ManagedExecutorHungTasksPeriodicTermination INSTANCE = new ManagedExecutorHungTasksPeriodicTermination();

    private final Lock lock;
    private volatile ScheduledExecutorService scheduler;
    private final Map<ManagedExecutorWithHungThreads, Future> executorFutureMap;

    private ManagedExecutorHungTasksPeriodicTermination() {
        lock = new ReentrantLock();
        executorFutureMap = new HashMap<>();
    }

    public boolean addManagedExecutor(final ManagedExecutorWithHungThreads executor, final long hungTaskTerminationPeriod) {
        if (executor == null) {
            throw new NullPointerException("executor is null");
        }
        if (hungTaskTerminationPeriod <= 0) {
            throw new IllegalArgumentException("hungTaskTerminationPeriod is not > 0");
        }
        lock.lock();
        try {
            if (executorFutureMap.containsKey(executor)) {
                return false;
            }
            if (this.scheduler == null) {
                this.scheduler = Executors.newSingleThreadScheduledExecutor();
            }
            final Future future = scheduler.scheduleAtFixedRate(() -> {
                final String executorName = executor.getClass().getSimpleName() + ":" + executor.getName();
                EeLogger.ROOT_LOGGER.warnf("Executing periodic termination of hung tasks for executor %s...", executorName);
                try {
                    executor.terminateHungTasks();
                } catch (Throwable t) {
                    EeLogger.ROOT_LOGGER.warnf(t, "Failure on periodic termination of hung tasks for executor %s", executorName);
                }
            }, hungTaskTerminationPeriod, hungTaskTerminationPeriod, TimeUnit.MILLISECONDS);
            executorFutureMap.put(executor, future);
            return true;
        }
        finally {
            lock.unlock();
        }
    }

    public boolean removeManagedExecutor(ManagedExecutorWithHungThreads executor) {
        lock.lock();
        try {
            final Future future = executorFutureMap.remove(executor);
            if (future == null) {
                return false;
            }
            future.cancel(true);
            if (executorFutureMap.isEmpty()) {
                scheduler.shutdownNow();
                scheduler = null;
            }
            return true;
        }
        finally {
            lock.unlock();
        }
    }
}
