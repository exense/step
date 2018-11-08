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
package step.grid.agent.conf;

import java.io.File;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;

import step.grid.agent.ArgumentParser;

public class AgentConfParser {
	
	ObjectMapper mapper = new ObjectMapper();
	
	public AgentConf parser(ArgumentParser arguments, File file) throws Exception {
		byte[] bytes = Files.readAllBytes(file.toPath());
		
		String content = new String(bytes);
		
		String resolvedContent = replacePlaceholders(arguments, content);
		
		return mapper.reader(AgentConf.class).readValue(resolvedContent);
	}
	
	private String replacePlaceholders(ArgumentParser arguments, String configXml) {
        StringBuffer sb = new StringBuffer();
        Matcher m = Pattern.compile("\\$\\{(.+?)\\}").matcher(configXml);
        while (m.find()) {
            String key = m.group(1);
            String replacement = arguments.getOption(key);
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        return sb.toString();
    }

}
