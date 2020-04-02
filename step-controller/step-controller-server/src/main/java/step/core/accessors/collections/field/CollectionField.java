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
package step.core.accessors.collections.field;

import com.fasterxml.jackson.annotation.JsonIgnore;

import step.core.accessors.collections.field.formatter.Formatter;
import step.core.accessors.collections.field.formatter.StringFormatter;

public class CollectionField {
	
	
	String key;
	String title;
	@JsonIgnore
	Formatter format;
	public CollectionField(String key, String title, Formatter format) {
		super();
		this.key = key;
		this.title = title;
		this.format = format;
	}
	public CollectionField(String key, String title) {
		this(key,title,new StringFormatter());
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public Formatter getFormat() {
		return format;
	}
	public void setFormat(Formatter format) {
		this.format = format;
	}

}
