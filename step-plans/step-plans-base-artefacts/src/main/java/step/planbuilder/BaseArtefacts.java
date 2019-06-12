package step.planbuilder;

import java.io.File;

import step.artefacts.ForBlock;
import step.artefacts.ForEachBlock;
import step.artefacts.Sequence;
import step.artefacts.Set;
import step.core.dynamicbeans.DynamicValue;
import step.datapool.excel.ExcelDataPool;
import step.datapool.sequence.IntSequenceDataPool;

public class BaseArtefacts {
	
	public static Sequence sequence() {
		return new Sequence();
	}
	
	public static Set set(String key, String valueExpression) {
		Set set = new Set();
		set.setKey(new DynamicValue<String>(key));
		set.setValue(new DynamicValue<>(valueExpression, ""));
		return set;
	}
	
	public static ForBlock for_(int start, int end) {
		ForBlock f = new ForBlock();
		IntSequenceDataPool conf = new IntSequenceDataPool();
		conf.setStart(new DynamicValue<Integer>(start));;
		conf.setEnd(new DynamicValue<Integer>(end));;
		f.setDataSource(conf);
		return f;
	}
	
	public static ForEachBlock forEachRowInExcel(File file) {
		ForEachBlock f = new ForEachBlock();
		ExcelDataPool p = new ExcelDataPool();
		p.setFile(new DynamicValue<String>(file.getAbsolutePath()));
		p.getHeaders().setValue(true);
		f.setDataSource(p);
		f.setDataSourceType("excel");
		return f;
	}

}
