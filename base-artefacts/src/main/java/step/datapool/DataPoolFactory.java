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

import org.json.JSONObject;

import step.datapool.excel.ExcelDataPoolImpl;
import step.datapool.file.CSVReaderDataPool;
import step.datapool.file.FileDataPoolImpl;
import step.datapool.file.FlatFileReaderDataPool;
import step.datapool.jdbc.SQLTableDataPool;
import step.datapool.sequence.IntSequenceDataPoolImpl;

public class DataPoolFactory {

	public static DataSet getDataPool(String dataSourceType, JSONObject dataPoolConfiguration) {
		DataSet result = null;

		if(dataSourceType.equals("excel")) {
			result = new ExcelDataPoolImpl(dataPoolConfiguration);
		} else if(dataSourceType.equals("csv")) {
			result = new CSVReaderDataPool(dataPoolConfiguration); 
		} else if(dataSourceType.equals("folder")) {
			result = new FileDataPoolImpl(dataPoolConfiguration); 
		} else if(dataSourceType.equals("sql")) {
			result = new SQLTableDataPool(dataPoolConfiguration);
		} else if(dataSourceType.equals("file")) {
			result = new FlatFileReaderDataPool(dataPoolConfiguration);
		} else if(dataSourceType.equals("sequence")) {
			result = new IntSequenceDataPoolImpl(dataPoolConfiguration);
		} else {
			throw new RuntimeException("Unsupported data source type: "+dataSourceType);
		}

		return result;
	}
	
	public static JSONObject getDefaultDataPoolConfiguration(String dataSourceType) {
		JSONObject conf = new JSONObject();
		if(dataSourceType!=null) {
			if(dataSourceType.equals("excel")) {
			} else if(dataSourceType.equals("csv")) {
			} else if(dataSourceType.equals("folder")) {
			} else if(dataSourceType.equals("sql")) {
			} else if(dataSourceType.equals("file")) {
			} else if(dataSourceType.equals("sequence")) {
				conf.put("start", 1);
				conf.put("end", 2);
				conf.put("inc", 1);
			}
		}	
		return conf;
	}
}
