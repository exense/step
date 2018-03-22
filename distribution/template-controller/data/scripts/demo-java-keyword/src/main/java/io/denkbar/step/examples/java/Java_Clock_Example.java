package io.denkbar.step.examples.java;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import step.grid.io.OutputMessage;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;
import step.handlers.javahandler.KeywordRunner;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

public class Java_Clock_Example extends AbstractKeyword {

	@Keyword
	public void Demo_Keyword_Java() throws Exception {

		Date date;
		output.startMeasure("Demo_Java_Clock_subMeasurement");
		date = new Date();
		output.stopMeasure();

		String label = input.containsKey("label")?input.getString("label"):"Date:";
		output.add("date", label + date.toString());
		output.add("timestamp", date.getTime());
	}
	
	@Keyword
	public void Demo_Keyword_Java_1() throws Exception {
		session.put("object1", new Object());
	}
	
	@Keyword
	public void Demo_Keyword_Java_2() throws Exception {
		Object object = session.get("object1");
		if(object==null) {
			output.setError("Unable to find object1 in session. Please call the Demo_Keyword_Java_1 first");
		}
	}
	
	@Test
	public void Demo_Java_Clock_Test() {
	    Map<String, String> properties = new HashMap<>();
	    ExecutionContext ctx = KeywordRunner.getExecutionContext(properties, Java_Clock_Example.class);
	    
	    OutputMessage result = ctx.run("Demo_Keyword_Java","{ \"label\" : \"Current time is : \" }");
    
	    System.out.println(result.getPayload());
	    
	}
	
	@Test
	public void Demo_Keyword_Sequence() {
	    Map<String, String> properties = new HashMap<>();
	    ExecutionContext ctx = KeywordRunner.getExecutionContext(properties, Java_Clock_Example.class);
	    
	    OutputMessage result;
	    result = ctx.run("Demo_Keyword_Java_1","{}");
	    result = ctx.run("Demo_Keyword_Java_2","{}");
	    
	    Assert.assertNull(result.getError());	    
	}

}