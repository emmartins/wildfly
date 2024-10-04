/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.concurrency.subsystem;

import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;

/**
 * @author emartins
 */
public class ConcurrencyResourceDescriptionResolver extends StandardResourceDescriptionResolver {
    public ConcurrencyResourceDescriptionResolver(String keyPrefix) {
        super(keyPrefix, ConcurrencyResourceDescriptionResolver.class.getPackage().getName() + ".LocalDescriptions", ConcurrencyResourceDescriptionResolver.class.getClassLoader(), true, true);
    }
}
