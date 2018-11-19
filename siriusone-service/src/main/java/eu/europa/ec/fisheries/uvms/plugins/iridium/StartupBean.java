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
package eu.europa.ec.fisheries.uvms.plugins.iridium;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.*;

import eu.europa.ec.fisheries.uvms.commons.message.api.MessageException;
import eu.europa.ec.fisheries.uvms.plugins.iridium.producer.PluginToEventBusTopicProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PluginType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.CapabilityListType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.ServiceType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingListType;
import eu.europa.ec.fisheries.uvms.exchange.model.constant.ExchangeModelConstants;
import eu.europa.ec.fisheries.uvms.exchange.model.exception.ExchangeModelMarshallException;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangeModuleRequestMapper;
import eu.europa.ec.fisheries.uvms.plugins.iridium.mapper.ServiceMapper;
import eu.europa.ec.fisheries.uvms.plugins.iridium.service.FileHandlerBean;

@Singleton
@Startup
@DependsOn("PluginToEventBusTopicProducer")
public class StartupBean extends PluginDataHolder {

    private static final Logger LOG = LoggerFactory.getLogger(StartupBean.class);

    private static final int MAX_NUMBER_OF_TRIES = 5;
    private boolean isRegistered = false;
    private boolean isEnabled = false;
    private boolean waitingForResponse = false;
    private int numberOfTriesExecuted = 0;

    private String REGISTER_CLASS_NAME = "";
    private String APPLICATION_NAME = "";
    private String RESPONSE_TOPIC_NAME = "";

    @EJB
    private PluginToEventBusTopicProducer messageProducer;

    @EJB
    private FileHandlerBean fileHandler;

    private CapabilityListType capabilities;
    private SettingListType settingList;
    private ServiceType serviceType;

    @PostConstruct
    public void startup() {

        //This must be loaded first!!! Not doing that will end in dire problems later on!
        super.setPluginApplicaitonProperties(fileHandler.getPropertiesFromFile(PluginDataHolder.PLUGIN_PROPERTIES));
        REGISTER_CLASS_NAME = getPLuginApplicationProperty("application.groupid");
        APPLICATION_NAME = getPLuginApplicationProperty("application.name");
        RESPONSE_TOPIC_NAME = getPLuginApplicationProperty("application.responseTopicName");

        //Theese can be loaded in any order
        super.setPluginProperties(fileHandler.getPropertiesFromFile(PluginDataHolder.PROPERTIES));
        super.setPluginCapabilities(fileHandler.getPropertiesFromFile(PluginDataHolder.CAPABILITIES));

        ServiceMapper.mapToMapFromProperties(super.getSettings(), super.getPluginProperties(), getRegisterClassName());
        ServiceMapper.mapToMapFromProperties(super.getCapabilities(), super.getPluginCapabilities(), null);

        capabilities = ServiceMapper.getCapabilitiesListTypeFromMap(super.getCapabilities());
        settingList = ServiceMapper.getSettingsListTypeFromMap(super.getSettings());

        serviceType = ServiceMapper.getServiceType(
                getRegisterClassName() + "." + getApplicaionName(),
                getApplicaionName(),
                "A good description for the plugin",
                PluginType.SATELLITE_RECEIVER,
                getPluginResponseSubscriptionName(),
                "IRIDIUM");

        register();

        LOG.debug("Settings updated in plugin {}", REGISTER_CLASS_NAME);
        for (Map.Entry<String, String> entry : super.getSettings().entrySet()) {
            LOG.debug("Setting: KEY: {} , VALUE: {}", entry.getKey(), entry.getValue());
        }

        LOG.info("PLUGIN STARTED");
    }

    @PreDestroy
    public void shutdown() {
        unregister();
    }

    @Schedule(second = "*/10", minute = "*", hour = "*", persistent = false)
    public void timeout(Timer timer) {
        if (!isRegistered && numberOfTriesExecuted < MAX_NUMBER_OF_TRIES) {
            LOG.info(getRegisterClassName() + " is not registered, trying to register");
            register();
            numberOfTriesExecuted++;
        }
        if (isRegistered) {
            LOG.info(getRegisterClassName() + " is registered. Cancelling timer.");
            timer.cancel();
        } else if(numberOfTriesExecuted >= MAX_NUMBER_OF_TRIES) {
            LOG.info(getRegisterClassName() + " failed to register, maximum number of retries reached.");
            timer.cancel();
        }
    }

    private void register() {
        LOG.info("Registering to Exchange Module");
        setWaitingForResponse(true);
        try {
            String registerServiceRequest = ExchangeModuleRequestMapper.createRegisterServiceRequest(serviceType, capabilities, settingList);
            String correlationId = messageProducer.sendEventBusMessage(registerServiceRequest, ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
        } catch (ExchangeModelMarshallException | MessageException e) {
            LOG.error("Failed to send registration message to {}", ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
            setWaitingForResponse(false);
        }

    }

    private void unregister() {
        LOG.info("Unregistering from Exchange Module");
        try {
            String unregisterServiceRequest = ExchangeModuleRequestMapper.createUnregisterServiceRequest(serviceType);
            String correlationId = messageProducer.sendEventBusMessage(unregisterServiceRequest, ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
        } catch (ExchangeModelMarshallException | MessageException e) {
            LOG.error("Failed to send unregistration message to {}", ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
        }
    }

    public String getPLuginApplicationProperty(String key) {
        try {
            return (String) super.getPluginApplicaitonProperties().get(key);
        } catch (Exception e) {
            LOG.error("Failed to getSetting for key: " + key, getRegisterClassName());
            return null;
        }
    }

    public String getPluginResponseSubscriptionName() {
        return getRegisterClassName() + "." + getApplicaionName() + RESPONSE_TOPIC_NAME;
    }

    public String getResponseTopicMessageName() {
        return getSetting("application.groupid");
    }

    public String getRegisterClassName() {
        return REGISTER_CLASS_NAME;
    }

    public String getApplicaionName() {
        return APPLICATION_NAME;
    }

    public String getSetting(String key) {
        try {
            LOG.debug("Trying to get setting {} ", REGISTER_CLASS_NAME + "." + key);
            return super.getSettings().get(REGISTER_CLASS_NAME + "." + key);
        } catch (Exception e) {
            LOG.error("Failed to getSetting for key: " + key, REGISTER_CLASS_NAME);
            return null;
        }
    }

    public boolean isWaitingForResponse() {
        return waitingForResponse;
    }

    public void setWaitingForResponse(boolean waitingForResponse) {
        this.waitingForResponse = waitingForResponse;
    }

    public boolean isIsRegistered() {
        return isRegistered;
    }

    public void setIsRegistered(boolean isRegistered) {
        this.isRegistered = isRegistered;
    }

    public boolean isIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

}