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
package step.datapool;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.execution.ExecutionContext;

public abstract class DataSet<T> {
	
	private static final Logger logger = LoggerFactory.getLogger(DataSet.class);
	
	protected final T configuration;
	
	protected ExecutionContext context;
	
	/**
	 * An ordered FIFO queue containing all the rows retrieved by {@link DataSet#next()}.
	 * This queue is processed by the writeQueueProcessor thread and is used to
	 * to persist changes to data pool performed by the user
	 */
	protected LinkedBlockingQueue<DataPoolRow> writeQueue;
	protected ExecutorService writeQueueProcessor;
	
	protected boolean isRowCommitEnabled = false;
	protected volatile boolean closing;
	
	public DataSet(T configuration) {
		super();
		this.configuration = configuration;
	}
	
	/**
	 * @param rowCommit if the row commit is enabled. IMPORTANT: If row commit is enabled, 
	 * it is the responsibility of the user to call the method {@link DataPoolRow#commit()} after the row has been processed
	 * If the method {@link DataPoolRow#commit()} isn't called, closing the DataSet may block indefinitely 
	 */
	public void enableRowCommit(boolean rowCommit) {
		this.isRowCommitEnabled = rowCommit;
	}

	public void init() {
		closing = false;
		
		Integer maxQueueSize = context.getConfiguration().getPropertyAsInteger("datasets.write.queue.maxsize", 1000);
		writeQueue = new LinkedBlockingQueue<>(maxQueueSize);
		
		BasicThreadFactory factory = new BasicThreadFactory.Builder().namingPattern("dataset-write-thread-%d").build();
		writeQueueProcessor = Executors.newFixedThreadPool(1, factory);
		writeQueueProcessor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					DataPoolRow row;
					while(true) {
						row=writeQueue.poll(100, TimeUnit.MILLISECONDS);
						if(row == null) {
							if(closing) {
								// the data set has been closed. no more row will be added to the queue => break
								break;
							} else {
								// the data set has not been closed. new rows might be added to the queue => poll again 
								continue;
							}
						}
						
						if(isRowCommitEnabled) {
							// wait for the row to be committed
							row.waitForCommit();
						}
						try {
							writeRow(row);
						} catch (Exception e) {
							logger.error("Error while writing row"+row.toString(), e);
						}
					}
				} catch (InterruptedException e) {
					logger.error("Error while running queue processor thread", e);
				}
			}
		});
	}

	public abstract void reset();
	
	public void close() {
		closing = true;
		writeQueueProcessor.shutdown();
		try {
			boolean terminated = writeQueueProcessor.awaitTermination(1, TimeUnit.MINUTES);
			if(!terminated) {
				logger.error("Timeout while waiting for write queue processor to terminate");
			}
		} catch (InterruptedException e) {
			logger.error("Error while waiting for write queue processor to terminate", e);
		}
	}
	
	public final synchronized DataPoolRow next() {
		Object nextValue = next_();
		DataPoolRow dataPoolRow = nextValue!=null?new DataPoolRow(nextValue):null;
		if(dataPoolRow!=null) {
			// Put the row to the write queue
			writeQueue.offer(dataPoolRow);
		}
		return dataPoolRow;
	}
	
	public abstract Object next_();
	
	public abstract void addRow(Object row);
	
	public void save() {}

	protected void setContext(ExecutionContext context) {
		this.context = context;
	};
	
	public void writeRow(DataPoolRow row) throws IOException {
		
	}
}
