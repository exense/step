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
import step.datapool.DataSetHandle;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static step.expressions.ExpressionHandler.checkProtectionAndWrapIfRequired;

public class DataSetHandler extends ArtefactHandler<DataSetArtefact, ReportNode> {
		
	@Override
	public void createReportSkeleton_(ReportNode node, DataSetArtefact testArtefact) {
		initDataSetAndAddItToContext(node, testArtefact);
	}

	@Override
	public void execute_(ReportNode node, DataSetArtefact testArtefact) {
		initDataSetAndAddItToContext(node, testArtefact);
	}
	
	public static class DataSetWrapper implements DataSetHandle {
		
		DataSet<?> dataSet;
		
		DataSetArtefact artefact;

		public DataSetWrapper(DataSetArtefact artefact, DataSet<?> dataSet) {
			super();
			this.dataSet = dataSet;
			this.artefact = artefact;
		}

		public final Object next() {
			synchronized (dataSet) {
				DataPoolRow row = dataSet.next();
				if(row==null) {
					//TODO replace the following logic by a pluggable iteration startegie (Random, etc)
					if(artefact.getResetAtEnd().get()) {
						dataSet.reset();
						row = dataSet.next();
					}
				}
				return row != null ? checkProtectionAndWrapIfRequired(artefact.getDataSource().getProtect().get(), row.getValue(), "next()") : null;
			}
		}

		public synchronized Object nextChunk(int size) {
			return checkProtectionAndWrapIfRequired(artefact.getDataSource().getProtect().get(),
					new DataPoolChunk(IntStream.range(0, size).mapToObj(i -> next()).filter(Objects::nonNull).collect(Collectors.toList())), "nextChunk()");
		}

		public static class DataPoolChunk extends ArrayList<Object> {

			public DataPoolChunk(Collection<? extends Object> c) {
				super(c);
			}

			public String asJson() {
				JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
				stream().forEach(row -> {
					JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
					if(row instanceof Map) {
						Map<Object, Object> map = (Map) row;
						map.entrySet().forEach(e -> objectBuilder.add(e.getKey().toString(), e.getValue().toString()));
					}
					arrayBuilder.add(objectBuilder.build());
				});
				return arrayBuilder.build().toString();
			}
		}
		
		public final void addRow(Object row) {
			dataSet.addRow(row);
		}
	}
	
	private void initDataSetAndAddItToContext(ReportNode node, DataSetArtefact testArtefact) {
		final DataSet<?> dataSet;
		try {
			dataSet = DataPoolFactory.getDataPool(testArtefact.getDataSourceType(), testArtefact.getDataSource(), context);
			dataSet.enableRowCommit(false);
			dataSet.init();
			ReportNode parentNode = context.getReportNodeCache().get(node.getParentID());
			
			context.getVariablesManager().putVariable(parentNode, VariableType.NORMAL, testArtefact.getItem().get(), new DataSetWrapper(testArtefact, dataSet));
			
			context.getEventManager().addReportNodeEventListener(parentNode, new ReportNodeEventListener() {
				@Override
				public void onUpdate() {}
				@Override
				public void onDestroy() {
					try {
						dataSet.save();
					} finally {
						dataSet.close();						
					}
				}
			});
			
			node.setStatus(ReportNodeStatus.PASSED);
		} catch(Throwable e) {
			failWithException(node, e);
		}
	}
	
	@Override
	public ReportNode createReportNode_(ReportNode parentNode, DataSetArtefact testArtefact) {
		return new ReportNode();
	}
}
