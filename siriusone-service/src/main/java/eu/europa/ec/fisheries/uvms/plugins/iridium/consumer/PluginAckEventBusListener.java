/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.europa.ec.fisheries.uvms.plugins.iridium.consumer;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PluginFault;
import eu.europa.ec.fisheries.schema.exchange.registry.v1.ExchangeRegistryBaseRequest;
import eu.europa.ec.fisheries.schema.exchange.registry.v1.RegisterServiceResponse;
import eu.europa.ec.fisheries.schema.exchange.registry.v1.UnregisterServiceResponse;
import eu.europa.ec.fisheries.uvms.exchange.model.exception.ExchangeModelMarshallException;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.JAXBMarshaller;
import eu.europa.ec.fisheries.uvms.plugins.iridium.StartupBean;

public class PluginAckEventBusListener implements MessageListener {

    private static final Logger LOG = LoggerFactory.getLogger(PluginAckEventBusListener.class);

    @EJB
    private StartupBean startupService;

    @Override
    public void onMessage(Message inMessage) {
        LOG.info("Eventbus listener for siriusone at selector: {} got a message", startupService.getPluginResponseSubscriptionName());
        TextMessage textMessage = (TextMessage) inMessage;
        try {
            ExchangeRegistryBaseRequest request = tryConsumeRegistryBaseRequest(textMessage);
            if (request == null) {
                PluginFault fault = JAXBMarshaller.unmarshallTextMessage(textMessage, PluginFault.class);
                handlePluginFault(fault);
            } else {
                switch (request.getMethod()) {
                    case REGISTER_SERVICE:
                        RegisterServiceResponse registerResponse = JAXBMarshaller.unmarshallTextMessage(textMessage, RegisterServiceResponse.class);
                        startupService.setWaitingForResponse(Boolean.FALSE);
                        switch (registerResponse.getAck().getType()) {
                            case OK:
                                LOG.info("Register OK");
                                startupService.setIsRegistered(Boolean.TRUE);
                                break;
                            case NOK:
                                LOG.info("Register NOK: " + registerResponse.getAck().getMessage());
                                startupService.setIsRegistered(Boolean.FALSE);
                                break;
                            default:
                                LOG.error("[ Type not supperted: ]" + request.getMethod());
                        }
                        break;
                    case UNREGISTER_SERVICE:
                        UnregisterServiceResponse unregisterResponse = JAXBMarshaller.unmarshallTextMessage(textMessage, UnregisterServiceResponse.class);
                        switch (unregisterResponse.getAck().getType()) {
                            case OK:
                                LOG.info("Unregister OK");
                                break;
                            case NOK:
                                LOG.info("Unregister NOK");
                                break;
                            default:
                                LOG.error("[ Ack type not supported ] ");
                                break;
                        }
                        break;
                    default:
                        LOG.error("Not supported method");
                        break;
                }
            }
        } catch (ExchangeModelMarshallException | NullPointerException e) {
            LOG.error("[ Error when receiving message in siriusone ]", e);
        }
    }

    private void handlePluginFault(PluginFault fault) {
        LOG.error(startupService.getPluginResponseSubscriptionName() + " received fault " + fault.getCode() + " : " + fault.getMessage());
    }

    private ExchangeRegistryBaseRequest tryConsumeRegistryBaseRequest(TextMessage textMessage) {
        try {
            return JAXBMarshaller.unmarshallTextMessage(textMessage, ExchangeRegistryBaseRequest.class);
        } catch (ExchangeModelMarshallException e) {
            return null;
        }
    }
}