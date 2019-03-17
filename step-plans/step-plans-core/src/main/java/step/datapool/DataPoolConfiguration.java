package step.datapool;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import step.core.dynamicbeans.DynamicValue;

@JsonTypeInfo(use=Id.CLASS,property="_class")
public abstract class DataPoolConfiguration {

	private DynamicValue<Boolean> forWrite = new DynamicValue<Boolean>(false);

	public DynamicValue<Boolean> getForWrite() {
		return forWrite;
	}

	public void setForWrite(DynamicValue<Boolean> forWrite) {
		this.forWrite = forWrite;
	}
}
