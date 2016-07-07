package step.grid.io;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

public class OutputBuilder extends AdapterMessageBuilder<Output> {
	
	private MeasurementContext measureHelper;

	public OutputBuilder() {
		super(new Output());

		measureHelper = new MeasurementContext();
	}
	
	private static String OUTPUT_XML_TAG = "Return";

	public OutputBuilder setOutputParameter(String paramName, String value) {
		if(document==null) {
			createDocument(OUTPUT_XML_TAG);
		}
		setPayloadAttribute(paramName, value);	
		return this;
	}
	
	public OutputBuilder setTechnicalError(String technicalError) {
		message.setTechnicalError(technicalError);
		return this;
	}
	
	public OutputBuilder setTechnicalError(String errorMessage, Throwable e) {
		setTechnicalError(errorMessage);
		addAttachment(generateAttachmentForException(e));
		return this;
	}

	public String getTechnicalError() {
		return message.getTechnicalError();
	}
	
	public OutputBuilder setBusinessError(String businessError) {
		message.setBusinessError(businessError);
		return this;
	}

	public String getBusinessError() {
		return message.getBusinessError();
	}

	public void addAttachments(List<Attachment> attachments) {
		message.setAttachments(attachments);
	}

	public void addAttachment(Attachment attachment) {
		message.addAttachment(attachment);
	}
	
	public void startMeasure(String id) {
		measureHelper.startMeasure(id);
	}

	public void startMeasure(String id, long begin) {
		measureHelper.startMeasure(id, begin);
	}

	public void stopMeasure(long end, Map<String, String> data) {
		measureHelper.stopMeasure(end, data);
	}

	public void addMeasure(String measureName, long durationMillis) {
		measureHelper.addMeasure(measureName, durationMillis);
	}

	public void stopMeasure() {
		measureHelper.stopMeasure();
	}

	public void stopMeasure(Map<String, String> data) {
		measureHelper.stopMeasure(data);
	}

	@Override
	public Output build() {
		super.build();
		message.setMeasures(measureHelper.getMeasures());
		return message;
	}

	public void append(Output output) {
		document = output.payload;
		measureHelper.addMeasures(output.getMeasures());
		addAttachments(output.getAttachments());
		setTechnicalError(output.getTechnicalError());
		setBusinessError(output.getBusinessError());
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
