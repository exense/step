/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.core.miscellaneous;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;

public class TestArtefactResultHandler {
	
	static Logger logger = LoggerFactory.getLogger(TestArtefactResultHandler.class);
	
	public static ReportNode failWithException(ReportNode result, Throwable e) {
		return failWithException(result, e, true);
	}
	
	public static ReportNode failWithException(ReportNode result, Throwable e, boolean generateAttachment) {
		return failWithException(result, null, e, generateAttachment);

	}
	
	public static ReportNode failWithException(ReportNode result, String errorMsg, Throwable e, boolean generateAttachment) {
		if(generateAttachment && !(e instanceof ValidationException)) {			
			ReportNodeAttachmentManager.attach(e, result);
		}
		result.addError(errorMsg!=null?errorMsg+":"+e.getMessage():e.getMessage());
		result.setStatus(ReportNodeStatus.TECHNICAL_ERROR);
		
		return result;

	}
	
	

}
