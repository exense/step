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
package step.automation.packages;

import java.util.ArrayList;
import java.util.List;

public class RefreshResourceResult {
    private ResultStatus resultStatus = ResultStatus.NOT_REQUIRED;
    private final List<String> errorMessages = new ArrayList<>();
    private final List<String> infoMessages = new ArrayList<>();

    public void addError(String errorMessage) {
        resultStatus = ResultStatus.FAILED;
        errorMessages.add(errorMessage);
    }

    public void addInfo(String infoMessage){
        infoMessages.add(infoMessage);
    }

    public ResultStatus getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(ResultStatus resultStatus) {
        this.resultStatus = resultStatus;
    }

    public boolean isFailed(){
        return resultStatus == ResultStatus.FAILED;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public List<String> getInfoMessages() {
        return infoMessages;
    }

    public enum ResultStatus {
        REFRESHED,
        NOT_REQUIRED,
        FAILED
    }
}
