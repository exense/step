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
package step.artefacts;

import javax.annotation.PostConstruct;

import org.json.JSONObject;

import step.commons.dynamicbeans.DynamicAttribute;
import step.core.artefacts.AbstractArtefact;
import step.datapool.DataPoolFactory;


public class AbstractForBlock extends AbstractArtefact {
	
	private String item = "dataPool";
	
	private String dataSourceType;
	
	@DynamicAttribute
	private JSONObject dataSource;
	
	@DynamicAttribute
	private String maxFailedLoops;
	
	@DynamicAttribute
	private String maxLoops;
	
	@DynamicAttribute
	private String parallel = "false";
	
	@DynamicAttribute
	private String threads = "1";
	
	@PostConstruct
	public void init() {
		dataSource = DataPoolFactory.getDefaultDataPoolConfiguration(dataSourceType);
	}
	
	public String getDataSourceType() {
		return dataSourceType;
	}

	public void setDataSourceType(String dataSourceType) {
		this.dataSourceType = dataSourceType;
	}

	public JSONObject getDataSource() {
		return dataSource;
	}

	public void setDataSource(JSONObject dataSource) {
		this.dataSource = dataSource;
	}

	public void setItem(String item) {
		this.item = item;
	}

	public void setMaxFailedLoops(String maxFailedLoops) {
		this.maxFailedLoops = maxFailedLoops;
	}

	public void setMaxLoops(String maxLoops) {
		this.maxLoops = maxLoops;
	}

	public void setParallel(String parallel) {
		this.parallel = parallel;
	}

	public void setThreads(String threads) {
		this.threads = threads;
	}

	public String getItem() {
		return item;
	}

	public String getMaxFailedLoops() {
		return maxFailedLoops;
	}

	public String getMaxLoops() {
		return maxLoops;
	}

	public String getParallel() {
		return parallel;
	}

	public String getThreads() {
		return threads;
	}
}
