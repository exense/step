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
		cursor = configuration.getStartInt();
	}

	@Override
	public Object next_() {
		if(init) {
			init=false;
		} else {
			cursor+=configuration.getIncInt();
		}
		
		if(configuration.getIncInt()>0) {
			if(cursor<configuration.getEndInt()+1) {
				return cursor;
			} else {
				return null;
			}
		} else {
			if(cursor>configuration.getEndInt()-1) {
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
