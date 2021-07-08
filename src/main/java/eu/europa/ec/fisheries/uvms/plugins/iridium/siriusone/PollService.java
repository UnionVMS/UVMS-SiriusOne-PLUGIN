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
package eu.europa.ec.fisheries.uvms.plugins.iridium.siriusone;

import java.util.Date;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eu.europa.ec.fisheries.schema.exchange.common.v1.AcknowledgeTypeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.CommandType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.KeyValueType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollTypeType;
import eu.europa.ec.fisheries.uvms.plugins.iridium.StartupBean;

@RequestScoped
public class PollService {

    private static final int POSITION_REQUEST = 0x04;
    private static final int POSITION_REQUEST_LENGTH = 0x00;
    private static final int SET_INTERVAL = 0x03;
    private static final int SET_INTERVAL_LENGTH = 0x04;

    private static final Logger LOG = LoggerFactory.getLogger(PollService.class);
    
    @Inject
    StartupBean startupBean;
    
    public AcknowledgeTypeType processPollCommand(CommandType command) {
        String imei = getIMEI(command);
        if (imei == null) {
            LOG.error("Missing IMEI in poll command for poll: {}", command.getPoll().getPollId());
            return AcknowledgeTypeType.NOK;
        }

        byte[] request;
        if (PollTypeType.POLL.equals(command.getPoll().getPollTypeType())) {
            request = createPositionReportRequest();
        } else if (PollTypeType.CONFIG.equals(command.getPoll().getPollTypeType())) {
            Integer frequencySeconds = getReportFrequency(command);
            if (frequencySeconds == null) {
                LOG.error("Missing report frequency in poll command for poll: {}", command.getPoll().getPollId());
                return AcknowledgeTypeType.NOK;
            }
            request = createSetIntervalRequest(frequencySeconds);
        } else {
            LOG.error("Unsupported pollType: {} for poll: {}", command.getPoll().getPollTypeType(), command.getPoll().getPollId());
            return AcknowledgeTypeType.NOK;
        }
        
        try {
            sendRequest(imei, request, command.getPoll().getPollId(), startupBean.getSetting("MAILHOST"), startupBean.getSetting("SMTPPORT"), startupBean.getSetting("FROM"), startupBean.getSetting("GATEWAY"), startupBean.getSetting("USERNAME"), startupBean.getSetting("PSW"));
        } catch (MessagingException e) {
            LOG.error("Could not send request! Type: {}, pollId: {}", command.getPoll().getPollTypeType(), command.getPoll().getPollId());
            return AcknowledgeTypeType.NOK;
        }
        return AcknowledgeTypeType.OK;
    }

    protected void sendRequest(String imei, byte[] request, String pollId, String host, String port, String from, String to, String username, String password) throws MessagingException {
        Properties props = System.getProperties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        Session session = Session.getInstance(props);
        session.setDebug(true);
        
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        InternetAddress[] address = {new InternetAddress(to)};
        msg.setRecipients(Message.RecipientType.TO, address);
        msg.setSubject(imei);

        MimeBodyPart bodyPart = new MimeBodyPart();

        ByteArrayDataSource ds = new ByteArrayDataSource(request, "application/octet-stream");
        bodyPart.setDataHandler(new DataHandler(ds));
        bodyPart.setFileName(pollId + ".sbd");
        
        Multipart mp = new MimeMultipart();
        mp.addBodyPart(bodyPart);

        msg.setContent(mp);
        
        msg.setSentDate(new Date());
        
        Transport.send(msg, username, password);
    }
    
    private String getIMEI(CommandType command) {
        for (KeyValueType keyValueType : command.getPoll().getPollReceiver()) {
            if (keyValueType.getKey().equalsIgnoreCase("IMEI")) {
                return keyValueType.getValue();
            }
        }
        return null;
    }
    
    private Integer getReportFrequency(CommandType command) {
        for (KeyValueType keyValueType : command.getPoll().getPollPayload()) {
            if (keyValueType.getKey().equalsIgnoreCase("REPORT_FREQUENCY")) {
                return Integer.parseInt(keyValueType.getValue());
            }
        }
        return null;
    }
    
    protected byte[] createPositionReportRequest() {
        return new byte[] {(byte) POSITION_REQUEST,
                           (byte) POSITION_REQUEST_LENGTH};
    }
    
    protected byte[] createSetIntervalRequest(int intervalSeconds) {
        byte[] interval = intToByteArray(intervalSeconds);
        return new byte[] {(byte) SET_INTERVAL,
                           (byte) SET_INTERVAL_LENGTH,
                           (byte) interval[3],
                           (byte) interval[2],
                           (byte) interval[1],
                           (byte) interval[0]};
    }
    
    public static final byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }
}
