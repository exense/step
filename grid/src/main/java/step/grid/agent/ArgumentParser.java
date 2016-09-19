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
package step.grid.agent;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArgumentParser {
	private HashMap<String, String> options = new HashMap<String, String>();
	private Pattern p = Pattern.compile("-(.+?)(?:=(.+?))?$");

	public ArgumentParser(String[] paramArrayOfString) {
		Matcher localMatcher = null;
		for (int i = 0; i < paramArrayOfString.length; i++) {
			if (!(localMatcher = this.p.matcher(paramArrayOfString[i])).find())
				continue;
			this.options.put(localMatcher.group(1).toLowerCase(),
					localMatcher.group(2));
		}
	}

	public boolean hasOption(String paramString) {
		return this.options.containsKey(paramString.toLowerCase());
	}

	public String getOption(String paramString) {
		return (String) this.options.get(paramString.toLowerCase());
	}

	public Set<Entry<String, String>> entrySet() {
		return options.entrySet();
	}

	public Map<String, String> getOptions() {
		return options;
	}
}
