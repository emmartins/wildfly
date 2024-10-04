/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.concurrency;

import jakarta.enterprise.concurrent.ContextService;

import java.util.ServiceLoader;

/**
 * The interface for a Jakarta Concurrency Implementation
 * @author Eduardo Martins
 */
public interface ConcurrencyImplementation {

    // FIXME this should be moved to a DUP + setter/getter model
    ConcurrencyImplementation INSTANCE = ServiceLoader.load(ConcurrencyImplementation.class).findFirst().get();


}
