/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import java.time.Instant;

/**
 * Describes the metadata of a timer.
 * @author Paul Ferraro
 */
public interface TimerMetaData extends ImmutableTimerMetaData {

    void setLastTimout(Instant timeout);
}
