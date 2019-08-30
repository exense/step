package step.core.artefacts;

import org.bson.types.ObjectId;

public class TestBean2 {
	
	private ObjectId id = new ObjectId();
	
	private String test = "Test";

	public String getTest() {
		return test;
	}

	public void setTest(String test) {
		this.test = test;
	}

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

}
