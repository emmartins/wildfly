/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session;

import org.wildfly.clustering.ee.Creator;
import org.wildfly.clustering.ee.Remover;

/**
 * @author Paul Ferraro
 */
public interface SessionMetaDataFactory<V> extends ImmutableSessionMetaDataFactory<V>, Creator<String, V, SessionCreationMetaData>, Remover<String>, AutoCloseable {
    InvalidatableSessionMetaData createSessionMetaData(String id, V value);

    @Override
    default void close() {
        // Nothing to close
    }
}
