/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb.http.extension;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author sfcoy
 * @author martins
 */
class SubsystemXMLWriter implements XMLElementWriter<SubsystemMarshallingContext> {

    static SubsystemXMLWriter INSTANCE = new SubsystemXMLWriter();

    /*
     * {@inheritDoc}
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext subsystemMarshallingContext) throws XMLStreamException {
        subsystemMarshallingContext.startSubsystemElement(Namespace.EJB_OVER_HTTP_1_0.getUriString(), false);
        ModelNode model = subsystemMarshallingContext.getModelNode();
        if (model.hasDefined(ConnectorModel.NAME)) {
            for (Property connector : model.get(ConnectorModel.NAME).asPropertyList()) {
                writeConnectorElement(writer, connector.getValue());
            }
        }
        writer.writeEndElement();
    }

    private static void writeConnectorElement(XMLExtendedStreamWriter writer, ModelNode connectorModel) throws
            XMLStreamException {
        writer.writeStartElement(SubsystemElement.CONNECTOR.getLocalName());
        ConnectorResourceDefinition.ALLOWED_ROLE_NAMES_ATTR.marshallAsAttribute(connectorModel, false, writer);
        ConnectorResourceDefinition.CONTEXT_PATH_ATTR.marshallAsAttribute(connectorModel, writer);
        ConnectorResourceDefinition.LOGIN_AUTH_METHOD_ATTR.marshallAsAttribute(connectorModel, false, writer);
        ConnectorResourceDefinition.LOGIN_REALM_NAME_ATTR.marshallAsAttribute(connectorModel, false, writer);
        ConnectorResourceDefinition.SECURITY_DOMAIN_ATTR.marshallAsAttribute(connectorModel, false, writer);
        ConnectorResourceDefinition.VIRTUAL_HOST_ATTR.marshallAsAttribute(connectorModel, false, writer);
        writer.writeEndElement();
    }

}
