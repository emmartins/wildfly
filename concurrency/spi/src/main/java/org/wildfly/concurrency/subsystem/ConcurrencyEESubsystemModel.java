/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.concurrency.subsystem;

/**
 * @author emartins
 */
public interface ConcurrencyEESubsystemModel {

    String CONTEXT_SERVICE = "context-service";
    String MANAGED_THREAD_FACTORY = "managed-thread-factory";
    String MANAGED_EXECUTOR_SERVICE = "managed-executor-service";
    String MANAGED_SCHEDULED_EXECUTOR_SERVICE = "managed-scheduled-executor-service";
}
