package step.core.artefacts;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")  
public abstract class ArtefactFilter {
	
	public abstract boolean isSelected(AbstractArtefact artefact);

}
