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
package step.artefacts.handlers;

import step.artefacts.DataSetArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ReportNodeEventListener;
import step.core.variables.VariableType;
import step.datapool.DataPoolFactory;
import step.datapool.DataPoolRow;
import step.datapool.DataSet;

public class DataSetHandler extends ArtefactHandler<DataSetArtefact, ReportNode> {
		
	@Override
	public void createReportSkeleton_(ReportNode node, DataSetArtefact testArtefact) {		
		initDataSetAndAddItToContext(node, testArtefact);
	}

	@Override
	public void execute_(ReportNode node, DataSetArtefact testArtefact) {
		initDataSetAndAddItToContext(node, testArtefact);
	}
	
	public static class DataSetWrapper {
		
		DataSet<?> dataSet;

		public DataSetWrapper(DataSet<?> dataSet) {
			super();
			this.dataSet = dataSet;
		}

		public final Object next() {
			DataPoolRow row = dataSet.next();
			if(row==null) {
				//TODO replace the following logic by a pluggable iteration startegie (Random, etc)
				dataSet.close();
				dataSet.reset();
				row = dataSet.next();
			} 
			return row!=null?row.getValue():null;
		}
	}
	
	private void initDataSetAndAddItToContext(ReportNode node, DataSetArtefact testArtefact) {
		final DataSet<?> dataSet;
		try {
			dataSet = DataPoolFactory.getDataPool(testArtefact.getDataSourceType(), testArtefact.getDataSource());
			dataSet.reset();
			ReportNode parentNode = context.getReportNodeCache().get(node.getParentID().toString());
			
			context.getVariablesManager().putVariable(parentNode, VariableType.NORMAL, testArtefact.getItem().get(), new DataSetWrapper(dataSet));
			
			context.getGlobalContext().getEventManager().addReportNodeEventListener(parentNode, new ReportNodeEventListener() {
				@Override
				public void onDestroy() {
					dataSet.close();
				}
			});
			
			node.setStatus(ReportNodeStatus.PASSED);
		} catch(Exception e) {
			failWithException(node, e);
		}
	}
	
	@Override
	public ReportNode createReportNode_(ReportNode parentNode, DataSetArtefact testArtefact) {
		return new ReportNode();
	}
}
