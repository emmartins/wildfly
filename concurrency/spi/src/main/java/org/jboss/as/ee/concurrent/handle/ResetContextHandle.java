/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.handle;

import java.io.Serializable;

/**
 * The Wildfly's EE context handle.
 *
 * @author Eduardo Martins
 */
public interface ResetContextHandle extends Serializable {

    /**
     */
    void reset();

    /**
     * Retrieves the name of the factory which built the handle.
     * @return
     */
    String getFactoryName();
}
