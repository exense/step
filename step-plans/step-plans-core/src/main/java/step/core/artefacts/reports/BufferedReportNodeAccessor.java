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
package step.core.artefacts.reports;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.commons.datatable.DataTable;
import step.core.accessors.InMemoryCRUDAccessor;

public class BufferedReportNodeAccessor extends InMemoryCRUDAccessor<ReportNode> implements ReportNodeAccessor, AutoCloseable {

	private final ReportNodeAccessor underlyingReportNodeAccessor;
	private volatile boolean running = true;
	private final Consumer<ReportNode> transformer;
	
	public BufferedReportNodeAccessor(ReportNodeAccessor underlyingReportNodeAccessor, Consumer<ReportNode> transformer) {
		super();
		this.transformer = transformer;
		this.underlyingReportNodeAccessor = underlyingReportNodeAccessor;
		
		writeThreadPool = Executors.newFixedThreadPool(1);
		
		writeThreadPool.submit(()->{
			// keep looping as long as running. 
			// after running, keep looping as long as the queue is not empty
			while(running || queue.size()>0) {
				try {
					List<ReportNode> batch = queue.poll(100, TimeUnit.MILLISECONDS);
					underlyingReportNodeAccessor.save(batch);
				} catch(Throwable e) {
				}
			}
			underlyingReportNodeAccessor.save(buffer);
			
		});
	}

	public void remove(ObjectId id) {
		underlyingReportNodeAccessor.remove(id);
	}

	private final LinkedBlockingQueue<List<ReportNode>> queue = new LinkedBlockingQueue<>(1000);

	private final List<ReportNode> buffer = new ArrayList<>();
	
	private final ExecutorService writeThreadPool;
	
	public ReportNode save(ReportNode entity) {
		if(transformer != null) {
			transformer.accept(entity);
		}
		
		synchronized (buffer) {
			if(buffer.size()>100) {
				queue.add(new ArrayList<>(buffer));
				buffer.clear();
			}
			buffer.add(entity);
		}
		
		return null;
	}

	public ReportNode get(ObjectId id) {
		return underlyingReportNodeAccessor.get(id);
	}

	public void createIndexesIfNeeded(Long ttl) {
		underlyingReportNodeAccessor.createIndexesIfNeeded(ttl);
	}

	public ReportNode get(String id) {
		return underlyingReportNodeAccessor.get(id);
	}

	public List<ReportNode> getReportNodePath(ObjectId id) {
		return underlyingReportNodeAccessor.getReportNodePath(id);
	}

	public Iterator<ReportNode> getChildren(ObjectId parentID) {
		return underlyingReportNodeAccessor.getChildren(parentID);
	}

	public void save(Collection<? extends ReportNode> entities) {
		entities.forEach(e->save(e));
	}

	public Iterator<ReportNode> getChildren(ObjectId parentID, int skip, int limit) {
		return underlyingReportNodeAccessor.getChildren(parentID, skip, limit);
	}

	public ReportNode findByAttributes(Map<String, String> attributes) {
		return underlyingReportNodeAccessor.findByAttributes(attributes);
	}

	public Iterator<ReportNode> getReportNodesByExecutionID(String executionID) {
		return underlyingReportNodeAccessor.getReportNodesByExecutionID(executionID);
	}

	public long countReportNodesByExecutionID(String executionID) {
		return underlyingReportNodeAccessor.countReportNodesByExecutionID(executionID);
	}

	public Iterator<ReportNode> getReportNodesByExecutionIDAndClass(String executionID, String class_) {
		return underlyingReportNodeAccessor.getReportNodesByExecutionIDAndClass(executionID, class_);
	}

	public Iterator<ReportNode> getLeafReportNodesByExecutionID(String executionID) {
		return underlyingReportNodeAccessor.getLeafReportNodesByExecutionID(executionID);
	}

	public Spliterator<ReportNode> findManyByAttributes(Map<String, String> attributes) {
		return underlyingReportNodeAccessor.findManyByAttributes(attributes);
	}

	public Iterator<ReportNode> getReportNodesByExecutionIDAndCustomAttribute(String executionID,
			List<Map<String, String>> customAttributes) {
		return underlyingReportNodeAccessor.getReportNodesByExecutionIDAndCustomAttribute(executionID, customAttributes);
	}

	public ReportNode getReportNodeByParentIDAndArtefactID(ObjectId parentID, ObjectId artefactID) {
		return underlyingReportNodeAccessor.getReportNodeByParentIDAndArtefactID(parentID, artefactID);
	}

	public Iterator<ReportNode> getAll() {
		return underlyingReportNodeAccessor.getAll();
	}

	public ReportNode findByAttributes(Map<String, String> attributes, String attributesMapKey) {
		return underlyingReportNodeAccessor.findByAttributes(attributes, attributesMapKey);
	}

	public Iterator<ReportNode> getReportNodesByExecutionIDAndArtefactID(String executionID, String artefactID) {
		return underlyingReportNodeAccessor.getReportNodesByExecutionIDAndArtefactID(executionID, artefactID);
	}

	public Iterator<ReportNode> getFailedLeafReportNodesByExecutionID(String executionID) {
		return underlyingReportNodeAccessor.getFailedLeafReportNodesByExecutionID(executionID);
	}

	public DataTable getTimeBasedReport(String executionID, int resolution) {
		return underlyingReportNodeAccessor.getTimeBasedReport(executionID, resolution);
	}

	public ReportNode getRootReportNode(String executionID) {
		return underlyingReportNodeAccessor.getRootReportNode(executionID);
	}

	public Map<ReportNodeStatus, Integer> getLeafReportNodesStatusDistribution(String executionID, String reportNodeClass) {
		return underlyingReportNodeAccessor.getLeafReportNodesStatusDistribution(executionID, reportNodeClass);
	}

	public Spliterator<ReportNode> findManyByAttributes(Map<String, String> attributes, String attributesMapKey) {
		return underlyingReportNodeAccessor.findManyByAttributes(attributes, attributesMapKey);
	}

	public Iterator<ReportNode> getChildren(String parentID) {
		return underlyingReportNodeAccessor.getChildren(parentID);
	}

	public void removeNodesByExecutionID(String executionID) {
		underlyingReportNodeAccessor.removeNodesByExecutionID(executionID);
	}

	public List<ReportNode> getRange(int skip, int limit) {
		return underlyingReportNodeAccessor.getRange(skip, limit);
	}

	@Override
	public void close() throws Exception {
		running = false;
		writeThreadPool.shutdown();
		boolean awaitTermination = writeThreadPool.awaitTermination(5, TimeUnit.SECONDS);
		
		System.out.println("Closing buffered accessor. Terminated: "+awaitTermination+", Queue size:"+queue.size());
	}
}
