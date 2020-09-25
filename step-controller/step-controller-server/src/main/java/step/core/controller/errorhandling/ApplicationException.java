/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.core.controller.errorhandling;

import java.util.Map;

@SuppressWarnings("serial")
public class ApplicationException extends RuntimeException {

	private int errorCode;
	private String errorMessage;
	private Map<String, String> data;

	public ApplicationException(int errorCode, String errorMessage, Map<String, String> data) {
		super();
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.data = data;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public Map<String, String> getData() {
		return data;
	}
}
