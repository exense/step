/*******************************************************************************
 * (C) Copyright 2016 Dorian Cransac and Jerome Comte
 *  
 * This file is part of rtm
 *  
 * rtm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * rtm is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with rtm.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package step.functions.base.defaults;

import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;

public class SleepKeyword extends AbstractKeyword{
	
	@Keyword(htmlTemplate = "<label>Sleep time in ms</label><dynamic-textfield dynamic-value=\"inputs['sleepTime']\" on-save=\"save()\" />")
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
