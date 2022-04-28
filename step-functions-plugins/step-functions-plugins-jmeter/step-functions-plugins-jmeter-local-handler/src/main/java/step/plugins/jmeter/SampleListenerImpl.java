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

import java.util.*;
import java.util.stream.Collectors;

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

	private OutputBuilder outputBuilder;
	
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
		// Using a LinkedHashMap ensures that keys are enumerated in the order they were added.
		Map<String, Object> data = new LinkedHashMap<>();

		if(sample instanceof HTTPSampleResult) {
			HTTPSampleResult httpSampleResult = (HTTPSampleResult) sample;
			data.put("url", httpSampleResult.getUrlAsString());
			data.put("httpMethod", httpSampleResult.getHTTPMethod());
			data.put("responseCode", httpSampleResult.getResponseCode());
		}

		data.put("time", sample.getTime()); // this should be the elapsed time = duration for the full request
		data.put("errorCount", sample.getErrorCount());

		return data;
	}

	@Override
	public void sampleStarted(SampleEvent e) {}

	@Override
	public void sampleStopped(SampleEvent e) {}

	public void collect() {
		JsonArrayBuilder array = Json.createArrayBuilder();
		Map<String, Long> erroredSamples = new LinkedHashMap<>();

		for(SampleResult sample:samples) {
			JsonObjectBuilder object = Json.createObjectBuilder();
			mapSampleAttributesToReturnObject(object, sample);
			array.add(object.build());

			if (sample.getErrorCount() > 0) {
				erroredSamples.merge(sample.getSampleLabel(), (long) sample.getErrorCount(), Long::sum);
			}
		}

		outputBuilder.getPayloadBuilder().add("samples", array.build());

		// We consider the keyword call to be failed (with a business exception) if any sample returned an error.
		if (!erroredSamples.isEmpty()) {
			String message = "The following samples returned errors (error count in parentheses): ";
			message += erroredSamples.entrySet().stream().map(e -> e.getKey() + " (" + e.getValue() + ")").collect(Collectors.joining(", "));
			outputBuilder.setBusinessError(message);
		}
	}
	
	private void mapSampleAttributesToReturnObject(JsonObjectBuilder object, SampleResult sample) {
		object.add("label", sample.getSampleLabel());
		Map<String, Object> attributes = getDataMapForSample(sample);
		attributes.forEach((key, valueObject) -> {
			String value = Optional.ofNullable(valueObject).map(Object::toString).orElse("null");
			object.add(key, value);
		});
	}

	@Override
	public Object clone() {
		Object clone =  super.clone();
		((SampleListenerImpl)clone).outputBuilder = outputBuilder;
		((SampleListenerImpl)clone).samples = samples;
		return clone;
	}
}
