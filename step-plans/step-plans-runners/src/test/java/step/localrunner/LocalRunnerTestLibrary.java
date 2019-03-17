package step.localrunner;

import junit.framework.Assert;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;

public class LocalRunnerTestLibrary extends AbstractKeyword {

	@Keyword
	public void keyword1() {
		output.setPayloadJson(input.toString());
		System.out.println("Keyword1!");
	}
	
	@Keyword
	public void keyword2() {
		output.add("Att2", "Val2");
		System.out.println("Keyword2!"+input.getString("Param1"));
	}
	
	@Keyword
	public void writeSessionValue() {
		session.put(input.getString("key"), input.getString("value"));
	}
	
	@Keyword
	public void readSessionValue() {
		output.add(input.getString("key"), session.get(input.getString("key")).toString());
	}
	
	@Keyword
	public void assertSessionValue() {
		Assert.assertEquals(input.getString("value"), session.get(input.getString("key"))!=null?session.get(input.getString("key")).toString():"");
	}
}
