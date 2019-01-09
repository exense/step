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
package step.functions.base.types.handler;

import java.util.List;

import javax.json.JsonObject;

import step.functions.base.types.LocalFunctionType;
import step.functions.io.Input;
import step.functions.io.Output;
import step.handlers.javahandler.KeywordHandler;

public class LocalFunctionHandler extends KeywordHandler {

	@Override
	public Output<JsonObject> handle(Input<JsonObject> input) throws Exception{

		List<String> keywordClasses;
		try {
			keywordClasses = LocalFunctionType.getLocalKeywordClassNames();
		} catch (Exception e) {
			String errorMsg = "Error while looking for LocalFunction class names";
			throw new Exception(errorMsg, e);
		}

		StringBuilder classMessageToken = new StringBuilder();

		keywordClasses.forEach(s -> classMessageToken.append(s).append(";"));

		input.getProperties().put(KEYWORD_CLASSES, classMessageToken.toString().substring(0, classMessageToken.length() - 1));
		
		return super.handle(input);
	}

}
