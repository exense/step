package step.grid.io;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Output extends AdapterMessage {
	
	private String technicalError;
	
	private String businessError;
	
	@XmlElement(name = "attachment", nillable = true)
	private List<Attachment> attachments = new ArrayList<>();
	
	private List<Measure> measures = new ArrayList<>();

	protected Output() {
		super();
	}
	
	public boolean hasTechnicalError() {
		return technicalError!=null;
	}

	public String getTechnicalError() {
		return technicalError;
	}

	protected void setTechnicalError(String technicalError) {
		this.technicalError = technicalError;
	}
	
	public boolean hasBusinessError() {
		return businessError!=null;
	}
	
	public String getBusinessError() {
		return businessError;
	}
	
	public void setBusinessError(String businessError) {
		this.businessError = businessError;
	}

	public List<Attachment> getAttachments() {
		return attachments;
	}

	protected void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}
	
	protected void addAttachment(Attachment attachment) {
		attachments.add(attachment);
	}
	
	protected void addAttachments(List<Attachment> attachments) {
		attachments.addAll(attachments);
	}
	
	public List<Measure> getMeasures() {
		return measures;
	}
	
	public Measure getMeasureByName(String name) {
		for(Measure tr:measures) {
			if(name.equals(tr.getName())) {
				return tr;
			}
 		}
		return null;
	}
	
	public List<Measure> getMeasuresByName(String name) {
		return measures.stream().filter(tr->name.equals(tr.getName())).collect(Collectors.toList());
	}
	
	protected void setMeasures(List<Measure> measures) {
		this.measures = measures;
	}
}
