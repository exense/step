package io.denkbar.step.examples.java;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import step.grid.io.OutputMessage;
import step.script.AbstractScript;
import step.script.Arg;
import step.script.Function;
import step.script.ScriptRunner;
import step.script.ScriptRunner.ScriptContext;

public class Java_Clock_Example extends AbstractScript {

	@Function
	public void Demo_Java_Clock(@Arg("prettyString")String prettyString) throws Exception {

		Date date;
		startMeasure("Demo_Java_Clock_subMeasurement");
		date = new Date();
		stopMeasure();

		outputBuilder.add("prettyMessage", prettyString + date.toString());
		outputBuilder.add("timestamp", date.getTime());
	}
	
	@Test
	public void Demo_Java_Clock_Test() {
	    Map<String, String> properties = new HashMap<>();
	    ScriptContext ctx = ScriptRunner.getExecutionContext(properties);
	    
	    OutputMessage result = ctx.run("Demo_Java_Clock","{ \"prettyString\" : \"Current time is : \" }");
    
	    System.out.println(result.getPayload());
	    
	}

}