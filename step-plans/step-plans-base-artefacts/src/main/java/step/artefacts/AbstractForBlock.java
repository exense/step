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
package step.artefacts;

import javax.annotation.PostConstruct;

import step.core.artefacts.AbstractArtefact;
import ch.exense.commons.core.model.dynamicbeans.ContainsDynamicValues;
import ch.exense.commons.core.model.dynamicbeans.DynamicValue;
import step.core.entities.EntityManager;
import step.core.entities.EntityReference;
import step.datapool.DataPoolConfiguration;
import step.datapool.DataPoolFactory;


public class AbstractForBlock extends AbstractArtefact {
	
	private DynamicValue<String> item = new DynamicValue<String>("row");
	
	private String dataSourceType;
	
	private DataPoolConfiguration dataSource;
	
	private DynamicValue<Integer> maxFailedLoops = new DynamicValue<Integer>(null);
	
	private DynamicValue<Integer> threads = new DynamicValue<Integer>(1);
	
	private DynamicValue<String> globalCounter = new DynamicValue<String>("gcounter");

	private DynamicValue<String> userItem = new DynamicValue<String>("userId");
	
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
	
	@ContainsDynamicValues
	@EntityReference(type=EntityManager.recursive)
	public DataPoolConfiguration getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataPoolConfiguration dataSource) {
		this.dataSource = dataSource;
	}

	public DynamicValue<Integer> getThreads() {
		return threads;
	}

	public void setThreads(DynamicValue<Integer> threads) {
		this.threads = threads;
	}

	public DynamicValue<Integer> getMaxFailedLoops() {
		return maxFailedLoops;
	}

	public void setMaxFailedLoops(DynamicValue<Integer> maxFailedLoops) {
		this.maxFailedLoops = maxFailedLoops;
	}

	public DynamicValue<String> getItem() {
		return item;
	}

	public void setItem(DynamicValue<String> item) {
		this.item = item;
	}

	public DynamicValue<String> getGlobalCounter() {
		return globalCounter;
	}
	
	public void setGlobalCounter(DynamicValue<String> globalCounter) {
		this.globalCounter = globalCounter;
	}

	public DynamicValue<String> getUserItem() {
		return userItem;
	}

	public void setUserItem(DynamicValue<String> userItem) {
		this.userItem = userItem;
	}
}
