package io.denkbar.step.examples.java;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import step.grid.io.OutputMessage;
import step.handlers.javahandler.AbstractScript;
import step.handlers.javahandler.Function;
import step.handlers.javahandler.ScriptRunner;
import step.handlers.javahandler.ScriptRunner.ScriptContext;

public class Java_Clock_Example extends AbstractScript {

	@Function
	public void Demo_Keyword_Java() throws Exception {

		Date date;
		output.startMeasure("Demo_Java_Clock_subMeasurement");
		date = new Date();
		output.stopMeasure();

		String label = input.containsKey("label")?input.getString("label"):"Date:";
		output.add("date", label + date.toString());
		output.add("timestamp", date.getTime());
	}
	
	@Test
	public void Demo_Java_Clock_Test() {
	    Map<String, String> properties = new HashMap<>();
	    ScriptContext ctx = ScriptRunner.getExecutionContext(properties);
	    
	    OutputMessage result = ctx.run("Demo_Keyword_Java","{ \"label\" : \"Current time is : \" }");
    
	    System.out.println(result.getPayload());
	    
	}

}