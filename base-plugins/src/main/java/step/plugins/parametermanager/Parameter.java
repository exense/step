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
package step.plugins.parametermanager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import step.commons.activation.ActivableObject;
import step.commons.activation.Expression;

public class Parameter extends ActivableObject {
	
	String key;
	
	String value;
	
	public Parameter(Expression activationExpression, String key, String value) {
		super();
		this.activationExpression = activationExpression;
		this.key = key;
		this.value = value;
	}
	
	
	public String getKey() {
		return key;
	}


	public void setKey(String key) {
		this.key = key;
	}


	public String getValue() {
		return value;
	}


	public void setValue(String value) {
		this.value = value;
	}


	public static void main(String[] args) {
		
		ScriptEngineManager mgr = new ScriptEngineManager();
        List<ScriptEngineFactory> factories = mgr.getEngineFactories();

        for (ScriptEngineFactory factory : factories) {

            System.out.println("ScriptEngineFactory Info");

            String engName = factory.getEngineName();
            String engVersion = factory.getEngineVersion();
            String langName = factory.getLanguageName();
            String langVersion = factory.getLanguageVersion();

            System.out.printf("\tScript Engine: %s (%s)%n", engName, engVersion);

            List<String> engNames = factory.getNames();
            for(String name : engNames) {
                System.out.printf("\tEngine Alias: %s%n", name);
            }

            System.out.printf("\tLanguage: %s (%s)%n", langName, langVersion);

        }

		
		ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("nashorn");
        try {
        	
        	CompiledScript script = ((Compilable)engine).compile("user=='cja'");
        	
        	Map<String, Object> m = new HashMap<>();
        	m.put("user", "cja");
        	
        	long t1 = System.currentTimeMillis();
        	SimpleBindings b = new SimpleBindings(m); 
        	for(int i=0;i<1000;i++) {
        		script.eval(b);
        	}
        	System.out.println("js compiled script ms:" + (System.currentTimeMillis()-t1));
        	
        	engine.eval("function test(user){return user=='cja'}");
        	Invocable invocable = (Invocable) engine;  		
        	t1 = System.currentTimeMillis();
        	for(int i=0;i<100000;i++) {
        		invocable.invokeFunction("test", "cja");
        	}
        	System.out.println("js ms:" + (System.currentTimeMillis()-t1));
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        engine = manager.getEngineByName("groovy");
        try {
        	engine.put("user", "cja");
        	engine.eval("def test(){user=='cja'}");
        	Invocable invocable = (Invocable) engine;  		
        	long t1 = System.currentTimeMillis();
        	for(int i=0;i<100000;i++) {
        		invocable.invokeFunction("test", null);
        	}
        	System.out.println("groovy ms:" + (System.currentTimeMillis()-t1));
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
