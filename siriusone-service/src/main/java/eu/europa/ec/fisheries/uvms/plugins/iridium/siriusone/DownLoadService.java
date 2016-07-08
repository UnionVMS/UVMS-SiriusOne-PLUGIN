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

import eu.europa.ec.fisheries.schema.exchange.movement.mobileterminal.v1.IdList;
import eu.europa.ec.fisheries.schema.exchange.movement.mobileterminal.v1.IdType;
import eu.europa.ec.fisheries.schema.exchange.movement.mobileterminal.v1.MobileTerminalId;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementBaseType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementComChannelType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementPoint;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementSourceType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementTypeType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.SetReportMovementType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PluginType;
import eu.europa.ec.fisheries.uvms.plugins.iridium.StartupBean;
import eu.europa.ec.fisheries.uvms.plugins.iridium.service.ExchangeService;
import static eu.europa.ec.fisheries.uvms.plugins.iridium.siriusone.SiriusOneMessage.LOG;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.FlagTerm;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 **/
@Singleton
@DependsOn({"StartupBean"})
public class DownLoadService {

    final static Logger LOG = LoggerFactory.getLogger(DownLoadService.class);
    @EJB
    StartupBean startUp;

    @EJB
    ExchangeService service;

    @Asynchronous
    public Future<String> download() {
        LOG.debug("Download invoked");
        try {
            getMessages();
        } catch (Exception e) {
            LOG.debug("Exceotion", e);
        }
        return new AsyncResult<String>("");
    }

    private void getMessages() throws NoSuchProviderException, MessagingException, IOException {

        Properties props = System.getProperties();
        props.put("mail.smtp.port", startUp.getSetting("SMTPPORT"));
        props.put("mail.smtps.host", startUp.getSetting("SMTPSERVER"));
        props.put("mail.smtps.auth", "true");
        props.setProperty("mail.imap.port", startUp.getSetting("SMTPPORT"));

        Session session = Session.getInstance(props, null);

        Store store = session.getStore("imaps");
        store.connect(startUp.getSetting("SMTPSERVER"), startUp.getSetting("USERNAME"), startUp.getSetting("PSW"));
        Folder inbox = store.getFolder("INBOX");
        //Change to READ_WRITE if messages are to be deleted

        // search for all "unseen" messages
        Flags seen = new Flags(Flags.Flag.SEEN);
        FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
        //Message messages[] = inbox.search(unseenFlagTerm);

        inbox.open(Folder.READ_WRITE);
        Message[] mails = inbox.search(unseenFlagTerm);
        LOG.debug("Unseen messages: " + mails.length);
        //Message[] mails = inbox.getMessages();

        for (Message message : mails) {
            Multipart multipart = (Multipart) message.getContent();
            // System.out.println(multipart.getCount());

            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) && (bodyPart.getFileName() == null || bodyPart.getFileName().isEmpty())) {
                    continue; // dealing with attachments only
                }
                InputStream is = bodyPart.getInputStream();
                String s = bodyPart.getFileName();
                s = s.substring(0, s.indexOf("."));
                String[] ss = s.split("_");

                if (ss.length == 2) {
                    byte[] buf = new byte[100];
                    int bytesRead;
                    SiriusOneMessage mes = null;
                    while ((bytesRead = is.read(buf)) != -1) {
                        mes = new SiriusOneMessage(buf, Long.parseLong(ss[0]), Long.parseLong(ss[1]));
                        if (mes.isValid()) {
                            try {
                                msgToQue(mes);
                                message.setFlag(Flag.SEEN, true);
                            } catch (DatatypeConfigurationException dce) {
                                message.setFlag(Flag.SEEN, false);
                            }
                        }
                    }
                }
            }
        }

    }

    private void msgToQue(SiriusOneMessage msg) throws DatatypeConfigurationException {

        MovementBaseType movement = new MovementBaseType();
        //movement.setActivity(null);
        movement.setComChannelType(MovementComChannelType.MOBILE_TERMINAL);
        MobileTerminalId mobTermId = new MobileTerminalId();

        IdList deviceId = new IdList();
        deviceId.setType(IdType.SERIAL_NUMBER);
        deviceId.setValue("" + msg.getDeviceId());

        mobTermId.getMobileTerminalIdList().add(deviceId);

        movement.setMobileTerminalId(mobTermId);

        movement.setMovementType(MovementTypeType.POS);

        MovementPoint mp = new MovementPoint();
        mp.setAltitude(0.0);
        mp.setLatitude(msg.getLatitude());
        mp.setLongitude(msg.getLongitude());
        movement.setPosition(mp);

        movement.setPositionTime(msg.getDateTime());

        movement.setReportedCourse(msg.getCourse());

        movement.setReportedSpeed(msg.getSpeed());

        movement.setSource(MovementSourceType.IRIDIUM);

        //movement.setStatus("" + msg.getMemCode());
        movement.setStatus("11");

        SetReportMovementType reportType = new SetReportMovementType();
        reportType.setMovement(movement);

        reportType.setPluginName(startUp.getRegisterClassName() + "." + startUp.getApplicaionName());

        GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance();
        reportType.setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal));

        reportType.setPluginType(PluginType.SATELLITE_RECEIVER);

        LOG.debug("LONGITUDE GET " + msg.getLongitude());
        LOG.debug("LATITUDE GET " + msg.getLatitude());

        service.sendMovementReportToExchange(reportType);

        LOG.debug("Sending movement to Excahnge");
    }

}