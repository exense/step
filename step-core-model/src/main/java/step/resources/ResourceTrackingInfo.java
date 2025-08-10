/*
 * ******************************************************************************
 *  * Copyright (C) 2020, exense GmbH
 *  *
 *  * This file is part of STEP
 *  *
 *  * STEP is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * STEP is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *  *****************************************************************************
 */
package step.resources;

import java.util.Date;

public class ResourceTrackingInfo {
    private String trackingAttributeValue;
    private Date creationDate;
    private String creationUser;
    private Date lastModificationDate;
    private String lastModificationUser;

    public ResourceTrackingInfo(String trackingAttributeValue) {
        this.trackingAttributeValue = trackingAttributeValue;
        this.creationDate = new Date();
        this.lastModificationDate = new Date();
    }

    public ResourceTrackingInfo(String trackingAttributeValue, Date creationDate, String creationUser, Date lastModificationDate, String lastModificationUser) {
        this.trackingAttributeValue = trackingAttributeValue;
        this.creationDate = creationDate;
        this.creationUser = creationUser;
        this.lastModificationDate = lastModificationDate;
        this.lastModificationUser = lastModificationUser;
    }

    public String getTrackingAttributeValue() {
        return trackingAttributeValue;
    }

    public void setTrackingAttributeValue(String trackingAttributeValue) {
        this.trackingAttributeValue = trackingAttributeValue;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getCreationUser() {
        return creationUser;
    }

    public void setCreationUser(String creationUser) {
        this.creationUser = creationUser;
    }

    public Date getLastModificationDate() {
        return lastModificationDate;
    }

    public void setLastModificationDate(Date lastModificationDate) {
        this.lastModificationDate = lastModificationDate;
    }

    public String getLastModificationUser() {
        return lastModificationUser;
    }

    public void setLastModificationUser(String lastModificationUser) {
        this.lastModificationUser = lastModificationUser;
    }
}
