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

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.duplicateNamedElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author sfcoy
 */
class SubsystemXMLReader implements XMLElementReader<List<ModelNode>> {

    static SubsystemXMLReader INSTANCE = new SubsystemXMLReader();

    /*
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {

        final PathAddress subsystemAddress = PathAddress.pathAddress(SubsystemResourceDefinition.SUBSYSTEM_PATH);
        list.add(Util.createAddOperation(subsystemAddress));

        requireNoAttributes(reader);

        Set<ConnectorSpecification> existingVirtualHostNames = new HashSet<ConnectorSpecification>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case EJB_OVER_HTTP_1_0:
                    switch (SubsystemElement.forLocalName(reader.getLocalName())) {
                        case CONNECTOR:
                            readConnectorElement(reader, list, subsystemAddress, existingVirtualHostNames);
                            break;
                        case UNKNOWN:
                            throw unexpectedElement(reader);
                    }
                    break;
                case UNKNOWN:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void readConnectorElement(XMLExtendedStreamReader reader, List<ModelNode> list, PathAddress subsystemAddress,
                                      Set<ConnectorSpecification> existingConnectors) throws XMLStreamException {
        final ModelNode addOperation = new ModelNode();
        addOperation.get(OP).set(ADD);
        list.add(addOperation);
        ConnectorSpecification connectorSpecification = readConnectorDefinition(reader);

        if (existingConnectors.contains(connectorSpecification))
            throw duplicateNamedElement(reader, SubsystemElement.CONNECTOR.getLocalName());
        if (connectorSpecification.getContext() == null)
            requireAttributes(reader, ConnectorAttribute.CONTEXT.getLocalName());

        requireNoContent(reader);

        ConnectorResourceDefinition.parseAndSetConnectorDefinition(connectorSpecification, addOperation, reader);
        final PathAddress address
            = subsystemAddress.append(ConnectorModel.NAME, connectorSpecification.getVirtualHost() + "/" +
                connectorSpecification.getContext());
        addOperation.get(OP_ADDR).set(address.toModelNode());
    }

    private ConnectorSpecification readConnectorDefinition(XMLExtendedStreamReader reader) throws XMLStreamException {
        ConnectorSpecification.Builder connectorDefinitionBuilder = new ConnectorSpecification.Builder();
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            requireNoNamespaceAttribute(reader, i);
            final String attributeValue = reader.getAttributeValue(i);
            final ConnectorAttribute attribute = ConnectorAttribute.forLocalName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CONTEXT:
                    connectorDefinitionBuilder.setContext(attributeValue);
                    break;
                case VIRTUAL_HOST:
                    connectorDefinitionBuilder.setVirtualHost(attributeValue);
                    break;
                case SECURITY_DOMAIN:
                    connectorDefinitionBuilder.setSecurityDomain(attributeValue);
                    break;
                case UNKNOWN:
                    throw unexpectedAttribute(reader, i);

            }
        }
        return connectorDefinitionBuilder.build();
    }

}
