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
package step.datapool;

import step.core.execution.ExecutionContext;
import step.datapool.excel.ExcelDataPool;
import step.datapool.excel.ExcelDataPoolImpl;
import step.datapool.file.CSVDataPool;
import step.datapool.file.CSVReaderDataPool;
import step.datapool.file.DirectoryDataPool;
import step.datapool.file.FileDataPool;
import step.datapool.file.FileDataPoolImpl;
import step.datapool.file.FlatFileReaderDataPool;
import step.datapool.inmemory.JsonStringDataPoolConfiguration;
import step.datapool.inmemory.JsonStringDataPoolImpl;
import step.datapool.jdbc.SQLTableDataPool;
import step.datapool.jdbc.SQLTableDataPoolConfiguration;
import step.datapool.sequence.IntSequenceDataPool;
import step.datapool.sequence.IntSequenceDataPoolImpl;

public class DataPoolFactory {

	public static DataSet<?> getDataPool(String dataSourceType, DataPoolConfiguration dataPoolConfiguration, ExecutionContext executionContext) {
		DataSet<?> result = null;

		if(dataSourceType.equals("excel")) {
			result = new ExcelDataPoolImpl((ExcelDataPool) dataPoolConfiguration);
		} else if(dataSourceType.equals("csv")) {
			result = new CSVReaderDataPool((CSVDataPool) dataPoolConfiguration); 
		} else if(dataSourceType.equals("folder")) {
			result = new FileDataPoolImpl((DirectoryDataPool) dataPoolConfiguration); 
		} else if(dataSourceType.equals("sql")) {
			result = new SQLTableDataPool((SQLTableDataPoolConfiguration) dataPoolConfiguration);
		} else if(dataSourceType.equals("file")) {
			result = new FlatFileReaderDataPool((FileDataPool) dataPoolConfiguration);
		} else if(dataSourceType.equals("sequence")) {
			result = new IntSequenceDataPoolImpl((IntSequenceDataPool) dataPoolConfiguration);
		} else if(dataSourceType.equals("json")) {
			result = new JsonStringDataPoolImpl((JsonStringDataPoolConfiguration) dataPoolConfiguration);
		} else {
			throw new RuntimeException("Unsupported data source type: "+dataSourceType);
		}
		
		result.setContext(executionContext);
		
		return result;
	}
	
	public static DataPoolConfiguration getDefaultDataPoolConfiguration(String dataSourceType) {
		DataPoolConfiguration conf = null;

		if(dataSourceType!=null) {		
			if(dataSourceType.equals("excel")) {
				conf = new ExcelDataPool();
			} else if(dataSourceType.equals("csv")) {
				conf = new CSVDataPool();
			} else if(dataSourceType.equals("folder")) {
				conf = new DirectoryDataPool();
			} else if(dataSourceType.equals("sql")) {
				conf = new SQLTableDataPoolConfiguration();
			} else if(dataSourceType.equals("file")) {
				conf = new FileDataPool();
			} else if(dataSourceType.equals("sequence")) {
				conf = new IntSequenceDataPool();
			} else if(dataSourceType.equals("json")) {
				conf = new JsonStringDataPoolConfiguration();
			} else {
				throw new RuntimeException("Unsupported data source type: "+dataSourceType);
			}
		}
		
		return conf;
	}
}
