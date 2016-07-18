package step.datapool.sequence;

import java.util.HashMap;
import java.util.Map;

import step.artefacts.ForBlock;
import step.datapool.DataSet;


public class IntSequenceDataPoolImpl extends DataSet {
	
	ForBlock configuration;
	
	Map<String, String> params = new HashMap<>();
	
	int cursor;
	
	boolean init = true;
			
	public IntSequenceDataPoolImpl(ForBlock configuration) {
		super();
		this.configuration = configuration;
	}

	@Override
	public void reset_() {
		init=true;
		cursor = configuration.getStart();
	}

	@Override
	public Object next_() {
		if(init) {
			init=false;
		} else {
			cursor+=configuration.getInc();
		}
		
		if(configuration.getInc()>0) {
			if(cursor<configuration.getEnd()+1) {
				return cursor;
			} else {
				return null;
			}
		} else {
			if(cursor>configuration.getEnd()-1) {
				return cursor;
			} else {
				return null;
			}	
		}
	}

	@Override
	public void close() {
	}
}
