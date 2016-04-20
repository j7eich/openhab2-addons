/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.avmtr064.handler;

import static org.openhab.binding.avmtr064.BindingConstants.CHANNEL_CONNSTATUS;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.avmtr064.BindingConstants;
import org.openhab.binding.avmtr064.config.FritzBoxConfiguration;
import org.openhab.binding.avmtr064.internal.Tr064Action;
import org.openhab.binding.avmtr064.internal.Tr064Argument;
import org.openhab.binding.avmtr064.internal.Tr064Service;
import org.openhab.binding.avmtr064.internal.Tr064Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The {@link FritzBoxHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Jan Siebeneich - Initial contribution
 */
public class FritzBoxHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(FritzBoxHandler.class);

    public FritzBoxHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_CONNSTATUS)) {
            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing " + BindingConstants.THING_TYPE_FRITZBOX);

        FritzBoxConfiguration config = this.getConfigAs(FritzBoxConfiguration.class);

        logger.debug(BindingConstants.THING_TYPE_FRITZBOX + " config: " + config.toString());

        String baseUrl = "http://" + config.getIpAddress() + ":" + config.getPort().toString();

        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        updateStatus(ThingStatus.INITIALIZING);

        try {
            URL descriptionUrl = new URL(baseUrl + "/tr64desc.xml");
            URLConnection myConnection = descriptionUrl.openConnection();
            InputStream response = myConnection.getInputStream();
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(response);
            NodeList nodes = doc.getElementsByTagName("service");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node n = nodes.item(i);
                Tr064Service tr064Service = new Tr064Service();
                NodeList serviceNodes = n.getChildNodes();
                for (int j = 0; j < serviceNodes.getLength(); j++) {
                    Node serviceNode = serviceNodes.item(j);
                    if (serviceNode.getNodeName() == "serviceType") {
                        tr064Service.setServiceType(serviceNode.getNodeValue());
                    } else if (serviceNode.getNodeName() == "controlURL") {
                        tr064Service.setControlUrl(serviceNode.getNodeValue());
                    } else if (serviceNode.getNodeName() == "SCPDURL") {
                        tr064Service.setScpdUrl(serviceNode.getNodeValue());
                    }
                }
                if (tr064Service.getScpdUrl() != null) {
                    URL scpdUrl = new URL(baseUrl + tr064Service.getScpdUrl());
                    URLConnection scpdConnection = scpdUrl.openConnection();
                    InputStream scpdResponse = scpdConnection.getInputStream();
                    Document scpdDoc = dBuilder.parse(scpdResponse);

                    NodeList variableNodes = scpdDoc.getElementsByTagName("stateVariable");
                    for (int j = 0; j < variableNodes.getLength(); j++) {
                        Node nVariable = variableNodes.item(j);
                        Tr064Variable tr064Variable = new Tr064Variable();
                        NodeList nVariableChildren = nVariable.getChildNodes();
                        for (int k = 0; k < nVariableChildren.getLength(); k++) {
                            Node nVariableChild = nVariableChildren.item(k);
                            if (nVariableChild.getNodeName() == "name") {
                                tr064Variable.setName(nVariableChild.getNodeValue());
                            } else if (nVariableChild.getNodeName() == "dataType") {
                                tr064Variable.setType(nVariableChild.getNodeValue());
                            }
                        }
                        tr064Service.addVariable(tr064Variable);
                    }

                    NodeList actionNodes = scpdDoc.getElementsByTagName("action");
                    for (int j = 0; j < actionNodes.getLength(); j++) {
                        Node nAction = actionNodes.item(j);
                        Tr064Action tr064Action = new Tr064Action();
                        NodeList nActionChildren = nAction.getChildNodes();
                        for (int k = 0; k < nActionChildren.getLength(); k++) {
                            Node nActionChild = nActionChildren.item(k);
                            if (nActionChild.getNodeName() == "name") {
                                tr064Action.setName(nActionChild.getNodeValue());
                            } else if (nActionChild.getNodeName() == "argumentList") {
                                NodeList arguments = nActionChild.getChildNodes();
                                for (int l = 0; l < arguments.getLength(); l++) {
                                    Node nArgument = arguments.item(l);
                                    Tr064Argument tr064Argument = new Tr064Argument();
                                    NodeList nArgumentChildren = nArgument.getChildNodes();
                                    boolean bIn = false;
                                    for (int m = 0; m < nArgumentChildren.getLength(); m++) {
                                        Node nArgumentChild = nArgumentChildren.item(m);
                                        if (nArgumentChild.getNodeName() == "name") {
                                            tr064Argument.setName(nArgumentChild.getNodeValue());
                                        } else if (nArgumentChild.getNodeName() == "direction") {
                                            if (nArgumentChild.getNodeValue() == "in") {
                                                bIn = true;
                                            }
                                        } else if (nArgumentChild.getNodeName() == "relatedStateVariable") {
                                            tr064Argument.setAssociatedVariable(nArgumentChild.getNodeValue());
                                        }
                                    }
                                    if (bIn == true) {
                                        tr064Action.addArgumentIn(tr064Argument);
                                    } else {
                                        tr064Action.addArgumentOut(tr064Argument);
                                    }
                                }
                            }
                        }
                        tr064Service.addAction(tr064Action);
                    }
                }
            }

        } catch (IOException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        } catch (ParserConfigurationException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        } catch (SAXException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }

        try {
            // TODO: This is a first SOAP communication test implementation. To be changed:
            // -> read configuration to get host (set by manual config or service discovery)
            // -> read tr64desc.xml, obtain service information
            // -> use information to construct SOAP requests.
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection soapConnection = soapConnectionFactory.createConnection();

            MessageFactory msgFactory = MessageFactory.newInstance();
            SOAPMessage message = msgFactory.createMessage();
            message.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");

            MimeHeaders mimeHeaders = message.getMimeHeaders();
            mimeHeaders.addHeader("SOAPACTION", "urn:dslforum-org:service:DeviceInfo:1#GetSecurityPort");

            SOAPHeader msgHeader = message.getSOAPHeader();
            SOAPBody msgBody = message.getSOAPBody();

            msgHeader.detachNode();

            QName bodyName = new QName("urn:dslforum-org:service:DeviceInfo:1", "GetSecurityPort", "u");
            msgBody.addBodyElement(bodyName);

            String sUrl = "http://" + config.getIpAddress() + ":" + config.getPort().toString()
                    + "/upnp/control/deviceinfo";

            URL endpoint = new URL(sUrl);
            SOAPMessage response = soapConnection.call(message, endpoint);

            soapConnection.close();

            SOAPBody respBody = response.getSOAPBody();
            QName myQName = new QName("urn:dslforum-org:service:DeviceInfo:1", "GetSecurityPortResponse");

            Iterator bodyIterator = respBody.getChildElements(myQName);
            SOAPBodyElement bodyElement = (SOAPBodyElement) bodyIterator.next();
            Iterator elementIterator = bodyElement.getChildElements(new QName("NewSecurityPort"));
            SOAPElement el = (SOAPElement) elementIterator.next();
            if (el.getLocalName() == "NewSecurityPort") {
                logger.debug(el.getValue());
            }

            updateStatus(ThingStatus.ONLINE);

        } catch (Exception ex) {
            logger.error(ex.toString());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR);
        }

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }
}
