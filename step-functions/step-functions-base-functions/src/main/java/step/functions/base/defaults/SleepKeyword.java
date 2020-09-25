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
package step.functions.base.defaults;

import step.core.accessors.Attribute;
import step.handlers.javahandler.AbstractKeyword;

@Attribute(key="project", value="@Common")
public class SleepKeyword extends AbstractKeyword{
	
	//@Keyword
	@Attribute(key="htmlTemplate", value="<label>Sleep time in ms</label><dynamic-textfield dynamic-value=\"inputs.sleepTime\" default-value=\"{dynamic:false,value:0}\" on-save=\"save()\" />")
	public void Sleep(){
		int sleepDurationMs;
		try {
			sleepDurationMs  = input.getInt("sleepTime");
		} catch (Exception e) {
			try {
				sleepDurationMs = Integer.parseInt(input.getString("sleepTime"));
			} catch (Exception e2) {
				throw new RuntimeException("Unable to parse attribute 'sleepTime' as long.",e2);
			}
		} 

		try {
			Thread.sleep(sleepDurationMs);
		} catch (InterruptedException e) {
		} finally {
			//
		}	
	}
	

}
