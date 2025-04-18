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
package step.core.artefacts.reports;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import org.bson.types.ObjectId;
import step.attachments.AttachmentMeta;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.reports.Error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonTypeInfo(use=Id.CLASS,property="_class")
public class ReportNode extends AbstractIdentifiableObject {
	
	protected ObjectId parentID;
	protected String path;
	protected String name;
	protected String executionID;
	protected ObjectId artefactID;
	protected String artefactHash;
	protected long executionTime;
	protected Integer duration;
	protected List<AttachmentMeta> attachments = new ArrayList<>();
	protected ReportNodeStatus status;
	protected Error error;
	protected Boolean isContributingError;
	protected Map<String, String> customAttributes;
	protected ParentSource parentSource;
	
	@JsonIgnore
	protected AbstractArtefact artefactInstance;

	@JsonIgnore
	protected boolean isOrphan;
	
	protected AbstractArtefact resolvedArtefact;

	public ReportNode() {
		super();
	}

	public ObjectId getParentID() {
		return parentID;
	}

	public void setParentID(ObjectId parentID) {
		this.parentID = parentID;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getExecutionID() {
		return executionID;
	}

	public void setExecutionID(String executionID) {
		this.executionID = executionID;
	}

	public ObjectId getArtefactID() {
		return artefactID;
	}

	public void setArtefactID(ObjectId artefactID) {
		this.artefactID = artefactID;
	}

	public String getArtefactHash() {
		return artefactHash;
	}

	public void setArtefactHash(String artefactHash) {
		this.artefactHash = artefactHash;
	}

	public long getExecutionTime() {
		return executionTime;
	}

	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
	}

	public Integer getDuration() {
		return duration;
	}

	public void setDuration(Integer duration) {
		this.duration = duration;
	}

	public List<AttachmentMeta> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<AttachmentMeta> attachments) {
		this.attachments = attachments;
	}

	public ReportNodeStatus getStatus() {
		return status;
	}

	public void setStatus(ReportNodeStatus status) {
		this.status = status;
	}

	public Error getError() {
		return error;
	}

	public void setError(Error error) {
		this.error = error;
		this.isContributingError = error != null && error.isRoot();
	}

	/**
	 * @return true if the error associated with this node ({@link ReportNode#getError()}) is marked as a contributing error.
	 * An error is marked as contributing when it is responsible for the status ({@link ReportNode#getStatus()}) of its parents to be failed.
	 *
	 * This method returns null when no error is associated with this node
	 */
	public Boolean getContributingError() {
		return isContributingError;
	}

	public void setContributingError(Boolean contributingError) {
		isContributingError = contributingError;
	}

	public void setCustomAttributes(Map<String, String> customAttributes) {
		this.customAttributes = customAttributes;
	}

	public AbstractArtefact getResolvedArtefact() {
		return resolvedArtefact;
	}

	public void setResolvedArtefact(AbstractArtefact resolvedArtefact) {
		this.resolvedArtefact = resolvedArtefact;
	}

	public void setError(String errorMessage, int errorCode, boolean isRoot) {
		Error errorObject = new Error();
		errorObject.setMsg(errorMessage);
		errorObject.setRoot(isRoot);
		errorObject.setCode(errorCode);
		setError(errorObject);
	}
	
	public void addError(String error) {
		if(error!=null) {
			if(this.error!=null) {
				getError().setMsg(getError().getMsg()+"\n"+error);
			} else {
				setError(error, 0, true);
			}
		}
	}
	
	public void addAttachment(AttachmentMeta attachment) {
		attachments.add(attachment);
	}

	public Map<String, String> getCustomAttributes() {
		return customAttributes;
	}
	
	public String getCustomAttribute(String key) {
		if(customAttributes!=null) {
			return customAttributes.get(key);
		} else {
			return null;
		}
	}

	public synchronized void addCustomAttribute(String key, String value) {
		if(customAttributes==null) {
			customAttributes = new HashMap<>();
		}
		customAttributes.put(key, value);
	}

	public AbstractArtefact getArtefactInstance() {
		return artefactInstance;
	}

	public void setArtefactInstance(AbstractArtefact artefactInstance) {
		this.artefactInstance = artefactInstance;
	}


	public boolean isOrphan() {
		return isOrphan;
	}

	public void setOrphan(boolean orphan) {
		isOrphan = orphan;
	}

	public boolean setVariableInParentScope() {
		return false;
	}

	public ParentSource getParentSource() {
		return parentSource;
	}

	public void setParentSource(ParentSource parentSource) {
		this.parentSource = parentSource;
	}

	/**
	 * @return a string representation of this report node.
	 * This method is called by report generators like the JUnit runner
	 */
	@JsonIgnore
	public String getReportAsString() {
		return null;
	}
}
