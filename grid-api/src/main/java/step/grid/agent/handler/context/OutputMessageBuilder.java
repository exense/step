/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.grid.agent.handler.context;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.spi.JsonProvider;

import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.grid.io.Measure;
import step.grid.io.OutputMessage;

/**
 * A builder for OutputMessage instances.
 * 
 * @author Jérôme Comte
 * @author Dorian Cransac
 *
 */
public class OutputMessageBuilder {
	
	private JsonObjectBuilder payloadBuilder;
	
	private String payloadJson;
	
	private MeasurementsBuilder measureHelper;
	
	private String error;
	
	private List<Attachment> attachments;
	
	private static JsonProvider jprov = JsonProvider.provider();
	
	private Measure lastMeasureHandle = null;

	public OutputMessageBuilder() {
		super();
		
		payloadBuilder = jprov.createObjectBuilder();

		measureHelper = new MeasurementsBuilder();
	}
	
	public JsonObjectBuilder getPayloadBuilder() {
		return payloadBuilder;
	}

	public void setPayloadBuilder(JsonObjectBuilder payloadBuilder) {
		this.payloadBuilder = payloadBuilder;
	}

	/**
	 * Adds an output attribute  
	 * If the object contains a mapping for the specified name, this method replaces the old value with the specified value.
	 * 
	 * @param name the name of the output attribute
	 * @param value the value of the output attribute
	 * @return this instance
	 */
	public OutputMessageBuilder add(String name, boolean value) {
		payloadBuilder.add(name, value);
		return this;
	}

	/**
	 * Adds an output attribute  
	 * If the object contains a mapping for the specified name, this method replaces the old value with the specified value.
	 * 
	 * @param name the name of the output attribute
	 * @param value the value of the output attribute
	 * @return this instance
	 */
	public OutputMessageBuilder add(String name, double value) {
		payloadBuilder.add(name, value);
		return this;
	}

	/**
	 * Adds an output attribute  
	 * If the object contains a mapping for the specified name, this method replaces the old value with the specified value.
	 * 
	 * @param name the name of the output attribute
	 * @param value the value of the output attribute
	 * @return this instance
	 */
	public OutputMessageBuilder add(String name, int value) {
		payloadBuilder.add(name, value);
		return this;
	}

	/**
	 * Adds an output attribute  
	 * If the object contains a mapping for the specified name, this method replaces the old value with the specified value.
	 * 
	 * @param name the name of the output attribute
	 * @param value the value of the output attribute
	 * @return this instance
	 */
	public OutputMessageBuilder add(String name, long value) {
		payloadBuilder.add(name, value);
		return this;
	}
	
	/**
	 * Adds an output attribute  
	 * If the object contains a mapping for the specified name, this method replaces the old value with the specified value.
	 * 
	 * @param name the name of the output attribute
	 * @param value the value of the output attribute
	 * @return this instance
	 */
	public OutputMessageBuilder add(String name, String value) {
		payloadBuilder.add(name, value);
		return this;
	}

	/**
	 * Reports a technical error.
	 * 
	 * @param technicalError the error message of the technical error
	 * @return this instance
	 */
	public OutputMessageBuilder setError(String technicalError) {
		error = technicalError;
		return this;
	}
	
	/**
	 * Appends a technical error message.
	 * Calling this method for the first time will have the same effect as calling setError
	 * 
	 * @param technicalError the error message of the technical error
	 * @return this instance
	 */
	public OutputMessageBuilder appendError(String technicalError) {
		if(error!=null) {
			error += technicalError;			
		} else {
			error = technicalError;	
		}
		return this;
	}
	
	/**
	 * Reports a technical error and appends the exception causing this error
	 * as attachment
	 * 
	 * @param errorMessage the error message of the technical error
	 * @param e the exception that caused the technical error
	 * @return this instance
	 */
	public OutputMessageBuilder setError(String errorMessage, Throwable e) {
		setError(errorMessage);
		addAttachment(generateAttachmentForException(e));
		return this;
	}
	
	/**
	 * @return the payload of this output. This has no eff
	 * 
	 */
	public String getPayloadJson() {
		return payloadJson;
	}

	/**
	 * 
	 * @param payloadJson the payload of this output.
	 */
	public void setPayloadJson(String payloadJson) {
		this.payloadJson = payloadJson;
	}

	/**
	 * Adds attachments to the output
	 * 
	 * @param attachments the list of attachments to be added to the output
	 */
	public void addAttachments(List<Attachment> attachments) {
		createAttachmentListIfNeeded();
		attachments.addAll(attachments);
	}

	/**
	 * Adds an attachment to the output
	 * 
	 * @param attachment the attachment to be added to the output
	 */
	public void addAttachment(Attachment attachment) {
		createAttachmentListIfNeeded();
		attachments.add(attachment);
	}

	private void createAttachmentListIfNeeded() {
		if(attachments==null) {
			attachments = new ArrayList<>();
		}
	}
	
	/**
	 * Starts a performance measurement. The current time will be used as starttime
	 * 
	 * @param id a unique identifier of the measurement
	 */
	public void startMeasure(String id) {
		measureHelper.startMeasure(id);
	}

	/**
	 * Starts a performance measurement
	 * 
	 * @param id a unique identifier of the measurement
	 * @param begin the start time of the measurement
	 */
	public void startMeasure(String id, long begin) {
		measureHelper.startMeasure(id, begin);
	}

	/**
	 * Adds a performance measurement
	 * 
	 * @param measureName a unique identifier of the measurement
	 * @param durationMillis the duration of the measurement in ms
	 */
	public void addMeasure(String measureName, long durationMillis) {
		measureHelper.addMeasure(measureName, durationMillis);
	}
	
	/**
	 * Adds a performance measurement with custom data
	 * 
	 * @param measureName a unique identifier of the measurement
	 * @param aDurationMillis the duration of the measurement in ms
	 * @param data the custom data of the measurement
	 */
	public void addMeasure(String measureName, long aDurationMillis, Map<String, Object> data) {
		measureHelper.addMeasure(measureName, aDurationMillis, data);
	}

	/**
	 * Stops the current performance measurement and adds it to the output
	 */
	public void stopMeasure() {
		measureHelper.stopMeasure();
	}

	/**
	 * Stops the current performance measurement and adds it to the output. 
	 * 
	 * @param data custom data to be added to the measurement
	 */
	public void stopMeasure(Map<String, Object> data) {
		measureHelper.stopMeasure(data);
	}
	
	public void stopMeasureForAdditionalData() {
		this.lastMeasureHandle = measureHelper.stopMeasure();
	}
	
	public void setLastMeasureAdditionalData(Map<String, Object> data) {
		this.lastMeasureHandle.setData(data);
		this.lastMeasureHandle = null;
	}

	/**
	 * Builds the output instance
	 * 
	 * @return the output message
	 */
	public OutputMessage build() {
		OutputMessage message = new OutputMessage();
		JsonObject payload;
		if(payloadJson==null) {
			payload = payloadBuilder.build();			
		} else {
			JsonReader reader = Json.createReader(new StringReader(payloadJson));
			try {
				payload = reader.readObject();				
			} finally {
				reader.close();
			}
		}
		message.setPayload(payload);
		message.setMeasures(measureHelper.getMeasures());
		message.setAttachments(attachments);
		message.setError(error);
		return message;
	}

	private Attachment generateAttachmentForException(Throwable e) {
		Attachment attachment = new Attachment();	
		attachment.setName("exception.log");
		StringWriter w = new StringWriter();
		e.printStackTrace(new PrintWriter(w));
		attachment.setHexContent(AttachmentHelper.getHex(w.toString().getBytes()));
		return attachment;
	}
}
