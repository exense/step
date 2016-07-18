package step.datapool;

import java.util.concurrent.atomic.AtomicInteger;



public abstract class DataSet {
	
	protected AtomicInteger rowNum;
	
	public final synchronized void reset() {
		rowNum = new AtomicInteger();
		reset_();
	}
	
	public abstract void reset_();
	
	public final synchronized DataPoolRow next() {
		int currentRowNum = rowNum.incrementAndGet();
		Object nextValue = next_();
		return nextValue!=null?new DataPoolRow(currentRowNum,nextValue):null;
	}
	
	public abstract Object next_();
	
	public void save() {};
	
	public abstract void close();
	
}
