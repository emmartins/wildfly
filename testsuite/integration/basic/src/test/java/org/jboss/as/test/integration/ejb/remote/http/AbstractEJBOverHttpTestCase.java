/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ejb.remote.http;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import junit.framework.Assert;

import static org.junit.Assert.assertTrue;

import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb.http.extension.ConnectorModel;
import org.jboss.as.ejb.http.extension.SubsystemResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;

/**
 *
 * @author Eduardo Martins
 *
 */
public abstract class AbstractEJBOverHttpTestCase {

    private static final Logger logger = Logger.getLogger(AbstractEJBOverHttpTestCase.class);

    public static final String APP_NAME = "ejb-remote-client-api-test";
    public static final String MODULE_NAME = "ejb";
    public static final String DISTINCT_NAME = "";
    public static final String SERVLET_DEPLOYMENT_NAME = "ejb3-remote";

    private final static String propertyName = "jboss.ejb.client.properties.skip.classloader.scan";
    private String propertyValue;

    @ContainerResource
    private ManagementClient managementClient;

    private PathAddress getPathAddress() {
        final PathAddress subsystemAddress = PathAddress.pathAddress(SubsystemResourceDefinition.SUBSYSTEM_PATH);
        return subsystemAddress.append(ConnectorModel.NAME, ConnectorModel.DEFAULT_HOST + "/" + SERVLET_DEPLOYMENT_NAME);
    }

    @Before
    public void before() throws Exception {
        // sys property needed to avoid scan of ejb client properties file
        propertyValue = System.getProperty(propertyName);
        System.setProperty(propertyName, "true");
        // add connector
        final ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(ConnectorModel.CONTEXT_ATTR).set(SERVLET_DEPLOYMENT_NAME);
        op.get(OP_ADDR).set(getPathAddress().toModelNode());
        ModelNode result = managementClient.getControllerClient().execute(op);
        logger.info("\naddOperation result asString = " + result.asString());
        assertTrue("success".equals(result.get("outcome").asString()));
    }

    @After
    public void after() throws Exception {
        // remove connector
        final ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        op.get(OP_ADDR).set(getPathAddress().toModelNode());
        ModelNode result = managementClient.getControllerClient().execute(op);
        logger.info("\nremoveOperation result asString = " + result.asString());
        Assert.assertFalse(result.get(FAILURE_DESCRIPTION).toString(), result.get(FAILURE_DESCRIPTION).isDefined());
        // restore sys property value
        if (propertyValue != null) {
            System.setProperty(propertyName, propertyValue);
        } else {
            System.clearProperty(propertyName);
        }
    }

}
