/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.handle;

import java.io.Serializable;

/**
 * The Wildfly's EE context handle that sets a saved invocation context.
 *
 * @author Eduardo Martins
 */
public interface SetupContextHandle extends Serializable {

    /**
     * @return
     * @throws IllegalStateException
     */
    ResetContextHandle setup() throws IllegalStateException;

    /**
     * Retrieves the name of the factory which built the handle.
     * @return
     */
    String getFactoryName();
}
