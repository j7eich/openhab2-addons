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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

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

    private static final String TR064_NODE_SERVICE = "service";
    private static final String TR064_NODE_SERVICE_TYPE = "serviceType";
    private static final String TR064_NODE_CONTROL_URL = "controlURL";
    private static final String TR064_NODE_SCPD_URL = "SCPDURL";
    private static final String TR064_NODE_STATE_VARIABLE = "stateVariable";
    private static final String TR064_NODE_NAME = "name";
    private static final String TR064_NODE_DATA_TYPE = "dataType";
    private static final String TR064_NODE_ARGUMENT_LIST = "argumentList";
    private static final String TR064_NODE_DIRECTION = "direction";
    private static final String TR064_NODE_RELATED_STATE_VARIABLE = "relatedStateVariable";
    private static final String TR064_NODE_ACTION = "action";
    private static final String TR064_DIRECTION_IN = "in";

    private static final String STRING_TRUE = "true";

    private static final String MIME_HEADERS_SOAPACTION = "SOAPACTION";

    private Logger logger = LoggerFactory.getLogger(FritzBoxHandler.class);
    private String baseUrl;
    private List<Tr064Service> services;

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

        baseUrl = "http://" + config.getIpAddress() + ":" + config.getPort().toString();

        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        updateStatus(ThingStatus.INITIALIZING);
        initServices("/tr64desc.xml");

        Tr064Argument argSecurityPort = new Tr064Argument();
        argSecurityPort.setName("NewSecurityPort");
        List<Tr064Argument> argListSecurityPort = new ArrayList<Tr064Argument>();
        argListSecurityPort.add(argSecurityPort);
        if (invokeSoapAction("urn:dslforum-org:service:DeviceInfo:1#GetSecurityPort", argListSecurityPort)) {
            logger.debug(argSecurityPort.getValue());
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR);
        }

        // try {
        // // TODO: This is a first SOAP communication test implementation. To be changed:
        // // -> read configuration to get host (set by manual config or service discovery)
        // // -> read tr64desc.xml, obtain service information
        // // -> use information to construct SOAP requests.
        // SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
        // SOAPConnection soapConnection = soapConnectionFactory.createConnection();
        //
        // MessageFactory msgFactory = MessageFactory.newInstance();
        // SOAPMessage message = msgFactory.createMessage();
        // message.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");
        //
        // MimeHeaders mimeHeaders = message.getMimeHeaders();
        // mimeHeaders.addHeader("SOAPACTION", "urn:dslforum-org:service:DeviceInfo:1#GetSecurityPort");
        //
        // SOAPHeader msgHeader = message.getSOAPHeader();
        // SOAPBody msgBody = message.getSOAPBody();
        //
        // msgHeader.detachNode();
        //
        // QName bodyName = new QName("urn:dslforum-org:service:DeviceInfo:1", "GetSecurityPort", "u");
        // msgBody.addBodyElement(bodyName);
        //
        // String sUrl = "http://" + config.getIpAddress() + ":" + config.getPort().toString()
        // + "/upnp/control/deviceinfo";
        //
        // URL endpoint = new URL(sUrl);
        // SOAPMessage response = soapConnection.call(message, endpoint);
        //
        // soapConnection.close();
        //
        // SOAPBody respBody = response.getSOAPBody();
        // QName myQName = new QName("urn:dslforum-org:service:DeviceInfo:1", "GetSecurityPortResponse");
        //
        // Iterator bodyIterator = respBody.getChildElements(myQName);
        // SOAPBodyElement bodyElement = (SOAPBodyElement) bodyIterator.next();
        // Iterator elementIterator = bodyElement.getChildElements(new QName("NewSecurityPort"));
        // SOAPElement el = (SOAPElement) elementIterator.next();
        // if (el.getLocalName() == "NewSecurityPort") {
        // logger.debug(el.getValue());
        // }
        //
        // updateStatus(ThingStatus.ONLINE);
        //
        // } catch (Exception ex) {
        // logger.error(ex.toString());
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR);
        // }

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }

    private void initServices(String descriptionPath) {
        try {
            URL descriptionUrl = new URL(baseUrl + descriptionPath);
            URLConnection myConnection = descriptionUrl.openConnection();
            InputStream response = myConnection.getInputStream();

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            Document doc = dBuilder.parse(response);

            NodeList nodeListService = doc.getElementsByTagName(TR064_NODE_SERVICE);
            for (int i = 0; i < nodeListService.getLength(); ++i) {
                Node nodeService = nodeListService.item(i);
                Tr064Service tr064Service = new Tr064Service();

                NodeList nodeListServiceChildren = nodeService.getChildNodes();
                for (int ii = 0; ii < nodeListServiceChildren.getLength(); ++ii) {
                    Node childNode = nodeListServiceChildren.item(ii);
                    if (childNode.getNodeName() == TR064_NODE_SERVICE_TYPE) {
                        tr064Service.setServiceType(childNode.getNodeValue());
                    } else if (childNode.getNodeName() == TR064_NODE_CONTROL_URL) {
                        tr064Service.setControlUrl(childNode.getNodeValue());
                    } else if (childNode.getNodeName() == TR064_NODE_SCPD_URL) {
                        tr064Service.setScpdUrl(childNode.getNodeValue());
                    }
                }

                if (tr064Service.getScpdUrl() != null) {
                    URL scpdUrl = new URL(baseUrl + tr064Service.getScpdUrl());
                    URLConnection scpdConnection = scpdUrl.openConnection();
                    InputStream scpdResponse = scpdConnection.getInputStream();

                    Document scpdDoc = dBuilder.parse(scpdResponse);

                    NodeList nodeListVariables = scpdDoc.getElementsByTagName(TR064_NODE_STATE_VARIABLE);
                    for (int ii = 0; ii < nodeListVariables.getLength(); ++ii) {
                        Node nodeVariable = nodeListVariables.item(ii);
                        Tr064Variable tr064Variable = new Tr064Variable();

                        NodeList nodeListVariableChildren = nodeVariable.getChildNodes();
                        for (int iii = 0; iii < nodeListVariableChildren.getLength(); ++iii) {
                            Node childNode = nodeListVariableChildren.item(iii);
                            if (childNode.getNodeName() == TR064_NODE_NAME) {
                                tr064Variable.setName(childNode.getNodeValue());
                            } else if (childNode.getNodeName() == TR064_NODE_DATA_TYPE) {
                                tr064Variable.setType(childNode.getNodeValue());
                            }
                        }
                        tr064Service.addVariable(tr064Variable);
                    }

                    NodeList nodeListActions = scpdDoc.getElementsByTagName(TR064_NODE_ACTION);
                    for (int ii = 0; ii < nodeListActions.getLength(); ++ii) {
                        Node nodeAction = nodeListActions.item(ii);
                        Tr064Action tr064Action = new Tr064Action();

                        NodeList nodeListActionChildren = nodeAction.getChildNodes();
                        for (int iii = 0; iii < nodeListActionChildren.getLength(); ++iii) {
                            Node childNode = nodeListActionChildren.item(iii);
                            if (childNode.getNodeName() == TR064_NODE_NAME) {
                                tr064Action.setName(childNode.getNodeValue());
                            } else if (childNode.getNodeName() == TR064_NODE_ARGUMENT_LIST) {
                                NodeList nodeListArguments = childNode.getChildNodes();
                                for (int j = 0; j < nodeListArguments.getLength(); ++j) {
                                    Node nodeArgument = nodeListArguments.item(j);
                                    Tr064Argument tr064Argument = new Tr064Argument();

                                    NodeList nodeListArgumentChildren = nodeArgument.getChildNodes();
                                    boolean bIn = false;
                                    for (int jj = 0; jj < nodeListArgumentChildren.getLength(); ++jj) {
                                        Node argumentChildNode = nodeListArgumentChildren.item(jj);
                                        if (argumentChildNode.getNodeName() == TR064_NODE_NAME) {
                                            tr064Argument.setName(argumentChildNode.getNodeValue());
                                        } else if (argumentChildNode.getNodeName() == TR064_NODE_DIRECTION
                                                && argumentChildNode.getNodeValue() == TR064_DIRECTION_IN) {
                                            bIn = true;
                                        } else
                                            if (argumentChildNode.getNodeName() == TR064_NODE_RELATED_STATE_VARIABLE) {
                                            tr064Argument.setAssociatedVariable(argumentChildNode.getNodeValue());
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
                    services.add(tr064Service);
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
    }

    private boolean invokeSoapAction(String actionPath, List<Tr064Argument> arguments) {
        try {
            // TODO: This is a first SOAP communication test implementation. To be changed:
            // -> read configuration to get host (set by manual config or service discovery)
            // -> read tr64desc.xml, obtain service information
            // -> use information to construct SOAP requests.

            String[] pathParts = actionPath.split("#");
            String serviceType = pathParts[0];
            String actionName = pathParts[1];
            String responseName = actionName + "Response";

            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            MessageFactory msgFactory = MessageFactory.newInstance();

            SOAPConnection soapConnection = soapConnectionFactory.createConnection();

            SOAPMessage message = msgFactory.createMessage();
            message.setProperty(SOAPMessage.WRITE_XML_DECLARATION, STRING_TRUE);

            MimeHeaders mimeHeaders = message.getMimeHeaders();
            mimeHeaders.addHeader(MIME_HEADERS_SOAPACTION, actionPath);

            SOAPHeader msgHeader = message.getSOAPHeader();
            SOAPBody msgBody = message.getSOAPBody();

            msgHeader.detachNode();

            QName bodyName = new QName(serviceType, actionName, "u");
            msgBody.addBodyElement(bodyName);

            String sControlUrl = "";

            ListIterator<Tr064Service> itService = services.listIterator();
            while (itService.hasNext()) {
                Tr064Service service = itService.next();
                if (service.getServiceType() == serviceType) {
                    Iterator<Tr064Action> itAction = service.getActions();
                    boolean actionFound = false;
                    while (itAction.hasNext()) {
                        Tr064Action action = itAction.next();
                        if (action.getName() == actionName) {
                            Iterator<Tr064Argument> itArgIn = action.getArgumentsIn();
                            while (itArgIn.hasNext()) {
                                Tr064Argument arg = itArgIn.next();
                                String argName = arg.getName();
                                ListIterator<Tr064Argument> itGivenArgs = arguments.listIterator();
                                boolean bMatch = false;
                                while (itGivenArgs.hasNext()) {
                                    Tr064Argument givenArg = itGivenArgs.next();
                                    if (givenArg.getName() == argName) {
                                        bMatch = true;
                                        msgBody.addChildElement(argName).addTextNode(givenArg.getValue());
                                        break;
                                    }
                                }
                                if (bMatch == false) {
                                    throw new Exception();
                                }
                            }

                            Iterator<Tr064Argument> itArgOut = action.getArgumentsOut();
                            while (itArgOut.hasNext()) {
                                Tr064Argument arg = itArgOut.next();
                                String argName = arg.getName();
                                ListIterator<Tr064Argument> itGivenArgs = arguments.listIterator();
                                boolean bMatch = false;
                                while (itGivenArgs.hasNext()) {
                                    Tr064Argument givenArg = itGivenArgs.next();
                                    if (givenArg.getName() == argName) {
                                        bMatch = true;
                                        break;
                                    }
                                }
                                if (bMatch == false) {
                                    throw new Exception();
                                }
                            }

                            actionFound = true;
                            break;
                        }
                    }
                    if (actionFound == false) {
                        throw new Exception();
                    }
                    sControlUrl = baseUrl + service.getControlUrl();
                    break;
                }
            }

            URL endpoint = new URL(sControlUrl);
            SOAPMessage response = soapConnection.call(message, endpoint);

            soapConnection.close();

            SOAPBody respBody = response.getSOAPBody();
            QName myQName = new QName(serviceType, responseName);

            Iterator bodyIterator = respBody.getChildElements(myQName);
            SOAPBodyElement bodyElement = (SOAPBodyElement) bodyIterator.next();

            Iterator elementIterator = bodyElement.getChildElements();
            while (elementIterator.hasNext()) {
                SOAPElement el = (SOAPElement) elementIterator.next();
                String elementName = el.getLocalName();
                ListIterator<Tr064Argument> itGivenArgs = arguments.listIterator();
                while (itGivenArgs.hasNext()) {
                    Tr064Argument givenArg = itGivenArgs.next();
                    if (givenArg.getName() == elementName) {
                        givenArg.setValue(el.getValue());
                    }
                }
            }
        } catch (Exception ex) {
            logger.error(ex.toString());
            return false;
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR);
        }
        return true;
    }
}
