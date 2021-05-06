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
package step.plugins.views.functions;

import step.core.tables.ViewCounterMap;
import step.plugins.views.ViewModel;

public class ErrorDistribution extends ViewModel {
	
	protected long count = 0;

	protected long errorCount = 0;
	
	protected ViewCounterMap countByErrorCode;

	protected ViewCounterMap countByErrorMsg;
	
	private int otherThreshhold;
	
	private String defaultKey;
	
	public ErrorDistribution(int customThreshold, String defaultKey){
		this.otherThreshhold = customThreshold;
		this.defaultKey = defaultKey;
		init();
	}

	private ErrorDistribution() {
	}
	
	private void init(){
		this.countByErrorCode = new ViewCounterMap(this.otherThreshhold, this.defaultKey);
		this.countByErrorMsg = new ViewCounterMap(this.otherThreshhold, this.defaultKey);
	}
	

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public long getErrorCount() {
		return errorCount;
	}

	public void setErrorCount(long errorCount) {
		this.errorCount = errorCount;
	}

	public void incrementByMsg(String message){
		this.countByErrorMsg.incrementForKey(message);
	}

	public void incrementByCode(String code){
		this.countByErrorCode.incrementForKey(code);
	}
	
	public void decrementByMsg(String message){
		this.countByErrorMsg.decrementForKey(message);
	}

	public void decrementByCode(String code){
		this.countByErrorCode.decrementForKey(code);
	}

	public ViewCounterMap getCountByErrorCode() {
		return countByErrorCode;
	}

	public void setCountByErrorCode(ViewCounterMap countByErrorCode) {
		this.countByErrorCode = countByErrorCode;
	}

	public ViewCounterMap getCountByErrorMsg() {
		return countByErrorMsg;
	}

	public void setCountByErrorMsg(ViewCounterMap countByErrorMsg) {
		this.countByErrorMsg = countByErrorMsg;
	}
}
