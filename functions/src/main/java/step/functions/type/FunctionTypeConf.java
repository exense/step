package step.functions.type;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use=Id.CLASS,property="_class")
public abstract class FunctionTypeConf {

	private ObjectId _id;

	private Integer callTimeout;
	
	public FunctionTypeConf() {
		super();
		this._id = new ObjectId();
	}

	public ObjectId getId() {
		return _id;
	}

	public void setId(ObjectId _id) {
		this._id = _id;
	}

	public Integer getCallTimeout() {
		return callTimeout;
	}

	public void setCallTimeout(Integer callTimeout) {
		this.callTimeout = callTimeout;
	}
	
}
