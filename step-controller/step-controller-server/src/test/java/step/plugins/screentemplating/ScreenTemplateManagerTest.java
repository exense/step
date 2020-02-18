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
package step.plugins.screentemplating;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;

import org.bson.types.ObjectId;
import org.junit.Test;

import junit.framework.Assert;
import step.commons.activation.Expression;

public class ScreenTemplateManagerTest {

	@Test
	public void test() {
		
		ScreenInputAccessor a = new ScreenInputAccessor() {

			@Override
			public List<ScreenInput> getScreenInputsByScreenId(String screenId) {
				if(screenId.equals("testScreen1")) {
					List<ScreenInput> result = new ArrayList<>();
					result.add(new ScreenInput("testScreen1", new Input("Param1")));
					result.add(new ScreenInput("testScreen1", new Input("Param2", Arrays.asList(new Option[] {new Option("Option1"), new Option("Option2")}))));
					result.add(new ScreenInput("testScreen1", new Input(InputType.TEXT,"Param3","LabelParam3", Arrays.asList(new Option[] {new Option("Option1"), new Option("Option2"), new Option("Option3","user=='user1'")}))));
					ScreenInput i = new ScreenInput("testScreen1", new Input(InputType.TEXT,"Param4","LabelParam4", Arrays.asList(new Option[] {new Option("Option1"), new Option("Option2")})));
					i.getInput().setActivationExpression(new Expression("user=='user1'"));
					result.add(i);
					i = new ScreenInput("testScreen1", new Input("Param5", Arrays.asList(new Option[] {new Option("Option1"), new Option("Option2")})));
					i.getInput().setActivationExpression(new Expression("user=='user1'"));
					result.add(i);
					return result;
				}
				throw new RuntimeException("Unknown screen "+screenId);
			}

			@Override
			public void remove(ObjectId id) {
			}

			@Override
			public ScreenInput save(ScreenInput entity) {
				return null;
			}

			@Override
			public void save(Collection<? extends ScreenInput> entities) {
			}

			@Override
			public ScreenInput get(ObjectId id) {
				return null;
			}

			@Override
			public ScreenInput findByAttributes(Map<String, String> attributes) {
				return null;
			}

			@Override
			public Iterator<ScreenInput> getAll() {
				return null;
			}

			@Override
			public Spliterator<ScreenInput> findManyByAttributes(Map<String, String> attributes) {
				return null;
			}

			@Override
			public ScreenInput findByAttributes(Map<String, String> attributes, String attributesMapKey) {
				return null;
			}

			@Override
			public Spliterator<ScreenInput> findManyByAttributes(Map<String, String> attributes,
					String attributesMapKey) {
				return null;
			}

			@Override
			public ScreenInput get(String id) {
				return get(new ObjectId(id));
			}
		};

		ScreenTemplateManager s = new ScreenTemplateManager(a);
		
		List<Input> inputs = s.getInputsForScreen("testScreen1", new HashMap<String, Object>());
		Assert.assertEquals(3, inputs.size());
		Assert.assertEquals(inputs.get(0), new Input(InputType.TEXT, "Param1", "Param1",null));
		
		List<Option> options = getDefaultOptionList();
		Assert.assertEquals(inputs.get(1), new Input(InputType.DROPDOWN, "Param2", "Param2",options));
		
		Assert.assertEquals(inputs.get(2), new Input(InputType.TEXT, "Param3", "LabelParam3",options));
		
		HashMap<String, Object> ctx = new HashMap<String, Object>();
		ctx.put("user", "user1");
		inputs = s.getInputsForScreen("testScreen1", ctx);
		Assert.assertEquals(5, inputs.size());
		Assert.assertEquals(inputs.get(2), new Input(InputType.TEXT, "Param3", "LabelParam3",getOptionListForUser1()));
		Assert.assertEquals(inputs.get(3), new Input(InputType.TEXT, "Param4", "LabelParam4",options));
		Assert.assertEquals(inputs.get(4), new Input(InputType.DROPDOWN, "Param5", "Param5",options));
	}

	private List<Option> getDefaultOptionList() {
		List<Option> options = new ArrayList<Option>();
		options.add(new Option("Option1"));
		options.add(new Option("Option2"));
		return options;
	}
	
	private List<Option> getOptionListForUser1() {
		List<Option> options = new ArrayList<Option>(getDefaultOptionList());
		options.add(new Option("Option3"));
		return options;
	}


}
