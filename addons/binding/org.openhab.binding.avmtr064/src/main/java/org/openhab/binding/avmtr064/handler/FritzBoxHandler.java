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

import java.net.URL;
import java.util.Iterator;

import javax.xml.namespace.QName;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        updateStatus(ThingStatus.INITIALIZING);
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
