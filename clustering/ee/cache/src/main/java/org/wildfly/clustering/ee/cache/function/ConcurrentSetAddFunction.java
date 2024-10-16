/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

/**
 * Function that adds an item to a set within a non-transactional cache.
 * @author Paul Ferraro
 * @param <V> the set element type
 */
public class ConcurrentSetAddFunction<V> extends SetAddFunction<V> {

    public ConcurrentSetAddFunction(V value) {
        super(value, new ConcurrentSetOperations<>());
    }
}
