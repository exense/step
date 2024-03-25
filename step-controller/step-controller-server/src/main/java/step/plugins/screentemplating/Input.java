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
package step.plugins.screentemplating;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import step.commons.activation.AbstractActivableObject;

public class Input extends AbstractActivableObject {

	InputType type;
	
	String id;
	
	String label;
	
	String description;

	//Deprecated for new columns configurations (only used for table rendering)
	List<String> customUIComponents;

	//Deprecated for new columns configurations (only used for function package)
	String searchMapperService;
	
	List<Option> options;
	
	String defaultValue;

	public Input() {
		super();
	}

	public Input(String id) {
		super();
		this.id = id;
		this.label = id;
		this.type = InputType.TEXT;
	}

    @JsonCreator
	public Input(@JsonProperty("id") String id, @JsonProperty("options") List<Option> options) {
		super();
		this.id = id;
		this.label = id;
		this.options = options;
		this.type = InputType.DROPDOWN;
	}

    public Input(InputType type, String id, String label, List<Option> options) {
		this(type, id, label, null, options);
	}
    
	public Input(InputType type, String id, String label, String description, List<Option> options) {
		super();
		this.type = type;
		this.id = id;
		this.label = label;
		this.description = description;
		this.options = options;
	}

	public InputType getType() {
		return type;
	}

	public void setType(InputType type) {
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getCustomUIComponents() {
		return customUIComponents;
	}

	public void setCustomUIComponents(List<String> customUIComponents) {
		this.customUIComponents = customUIComponents;
	}

	public String getSearchMapperService() {
		return searchMapperService;
	}

	public void setSearchMapperService(String searchMapperService) {
		this.searchMapperService = searchMapperService;
	}

	public List<Option> getOptions() {
		return options;
	}

	public void setOptions(List<Option> options) {
		this.options = options;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Input other = (Input) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
		
}
