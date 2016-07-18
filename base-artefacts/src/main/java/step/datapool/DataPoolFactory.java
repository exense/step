package step.datapool;

import step.artefacts.AbstractForBlock;
import step.artefacts.ForBlock;
import step.artefacts.ForEachBlock;
import step.datapool.excel.ExcelDataPoolImpl;
import step.datapool.file.FileDataPoolImpl;
import step.datapool.sequence.IntSequenceDataPoolImpl;



public class DataPoolFactory {

	public static DataSet getDataPool(AbstractForBlock dataPoolConfiguration) {
		DataSet result = null;
		
		if(dataPoolConfiguration instanceof ForEachBlock) {
			ForEachBlock forEach = (ForEachBlock) dataPoolConfiguration;
			if(forEach.getFolder() != null && forEach.getFolder().length() > 0) {
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
