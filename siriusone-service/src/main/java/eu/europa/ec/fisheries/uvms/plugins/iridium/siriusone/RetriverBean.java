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

import java.util.concurrent.Future;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ec.fisheries.uvms.plugins.iridium.StartupBean;

/***/
@Singleton
@Startup
public class RetriverBean {

    private static final Logger LOG = LoggerFactory.getLogger(RetriverBean.class);

    private Future connectFuture = null;

    @EJB
    private DownLoadService downloadService;

    @EJB
    private StartupBean startupBean;

    @Schedule(minute = "*/1", hour = "*", persistent = false)
    public void download() {
        if (startupBean.isIsEnabled() &&
                (connectFuture == null || (connectFuture != null && connectFuture.isDone()))) {
            connectFuture = downloadService.download();
        } else {
            LOG.debug("Future is not null and busy");
        }
    }

}