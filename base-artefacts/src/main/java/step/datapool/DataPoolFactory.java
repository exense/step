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

import step.artefacts.AbstractForBlock;
import step.artefacts.ForBlock;
import step.artefacts.ForEachBlock;
import step.datapool.excel.ExcelDataPoolImpl;
import step.datapool.file.FileDataPoolImpl;
import step.datapool.jdbc.SQLTableDataPool;
import step.datapool.sequence.IntSequenceDataPoolImpl;



public class DataPoolFactory {

	public static DataSet getDataPool(AbstractForBlock dataPoolConfiguration) {
		DataSet result = null;
		
		if(dataPoolConfiguration instanceof ForEachBlock) {
			ForEachBlock forEach = (ForEachBlock) dataPoolConfiguration;
			if(forEach.getFolder() != null && forEach.getFolder().length() > 0) {
				if(forEach.getFolder().startsWith("jdbc:"))
					result = new SQLTableDataPool(forEach);
				else
					result = new FileDataPoolImpl(forEach);				
			} else {
				result = new ExcelDataPoolImpl(forEach);
			}
		} else if (dataPoolConfiguration instanceof ForBlock) {
			result = new IntSequenceDataPoolImpl((ForBlock)dataPoolConfiguration);
		} else {
			throw new RuntimeException("No data pool configured for the artefact type " + dataPoolConfiguration.getClass());
		}
		
		return result;
	}
}
