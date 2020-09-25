/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.plugins.jmeter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleListener;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.AbstractTestElement;

import step.functions.io.OutputBuilder;

public class SampleListenerImpl extends AbstractTestElement implements SampleListener, Cloneable {

	private static final long serialVersionUID = -4394201534114759490L;
	
	OutputBuilder outputBuilder;
	
	List<SampleResult> samples = Collections.synchronizedList(new ArrayList<>());
	
	public SampleListenerImpl() {
		super();
	}

	public SampleListenerImpl(OutputBuilder outputBuilder) {
		super();
		this.outputBuilder = outputBuilder;
	}

	@Override
	public void sampleOccurred(SampleEvent e) {
		SampleResult result = e.getResult();
		samples.add(result);
		outputBuilder.addMeasure(result.getSampleLabel(), result.getTime(), getDataMapForSample(result));
	}

	private Map<String, Object> getDataMapForSample(SampleResult sample) {
		Map<String, Object> data = new HashMap<>();
		if(sample instanceof HTTPSampleResult) {
			HTTPSampleResult httpSampleResult = (HTTPSampleResult) sample;
			data.put("url", httpSampleResult.getUrlAsString());
			data.put("httpMethod", httpSampleResult.getHTTPMethod());
			data.put("responseCode", httpSampleResult.getResponseCode());
		}
		return data;
	}

	@Override
	public void sampleStarted(SampleEvent e) {}

	@Override
	public void sampleStopped(SampleEvent e) {}

	public OutputBuilder getOut() {
		return outputBuilder;
	}

	public void setOut(OutputBuilder out) {
		this.outputBuilder = out;
	}
	
	public void collect() {
		JsonArrayBuilder array = Json.createArrayBuilder();
		for(SampleResult sample:samples) {
			JsonObjectBuilder object = Json.createObjectBuilder();
			mapSampleAttributesToReturnObject(object, sample);
			array.add(object.build());
		}
		outputBuilder.getPayloadBuilder().add("samples", array.build());
	}
	
	private void mapSampleAttributesToReturnObject(JsonObjectBuilder object, SampleResult sample) {
		object.add("label", sample.getSampleLabel());
		if(sample instanceof HTTPSampleResult) {
			HTTPSampleResult httpSampleResult = (HTTPSampleResult) sample;
			object.add("url", httpSampleResult.getUrlAsString());
			object.add("method", httpSampleResult.getHTTPMethod());
			object.add("responseCode", httpSampleResult.getResponseCode());
		}
	}

	@Override
	public Object clone() {
		Object clone =  super.clone();
		((SampleListenerImpl)clone).outputBuilder = outputBuilder;
		((SampleListenerImpl)clone).samples = samples;
		return clone;
	}

}
