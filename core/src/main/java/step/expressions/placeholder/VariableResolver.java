package step.expressions.placeholder;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.variables.ImmutableVariableException;
import step.core.variables.UndefinedVariableException;

public class VariableResolver implements Resolver {

	private static Pattern mapPattern = Pattern.compile("(.+?)\\[(.+?)\\]");
	/* 
	 * Das im varPattern vorkommende optionale Strichpunkt ';' ruehrt von der Scriptkorrektur
	 * aus ExpressionHandler.evaluate() her. Kommentar siehe dort.
	 */
	private static Pattern varPattern = Pattern.compile("\\{(.+?)\\};?");
		
	public VariableResolver() {
		super();
	}
	
	private Object getVariable(String key) {
		return  ExecutionContext.getCurrentContext().getVariablesManager().getVariable(key);
	}

	@Override
	public String resolve(String name) {
		Matcher matcher = mapPattern.matcher(name);
		if(matcher.find()) {
			String mapName = matcher.group(1);
			String indexName = matcher.group(2);
			indexName = replaceIndexVars(indexName);// Workaround fuer das Problem der Index Namen, die Variablen enthalten.
			Object object = getVariable(mapName);
			if (object instanceof Map) {
				Map<?,?> container = (Map<?,?>) object;
				return castToString(container.get(indexName));
			} else {
				throw new RuntimeException("Error while evaluating the parameter " + name + 
						". A Map was expected but "+object +" was returned.");
			}
		} else {
			Object object = getVariable(name);
			return castToString(object);
		}
	}
	
	private static String castToString(Object value) {
		if(value!=null) {
			if(value instanceof String) {
				return (String)value;
			} else {
				return value.toString();
			}
		} else {
			return null;
		}
	}

	/**
	 * Schneller Fix fuer das Problem der Index Namen, die Variablen enthalten.
	 * Hier wird mit der Parameter-Ersetzungssprache interpretiert:
	 * 			'{s1}::Vorname' --> 'Sheet1::Vorname'
	 * falls die Variable s1 den Wert 'Sheet1' enthaelt.
	 * -------------------------------------------------------------------------
	 * ACHTUNG: funktioniert nur, falls tec.extendedgroovy == true ist, da sonst
	 * die verschachtelten {} nicht korrekt aufgeloest werden!
	 * -------------------------------------------------------------------------
	 * 
	 * @param indexName Der Indexnamen mit evtl. vorhandenen Variablen
	 * @return den aufgeloesten Indexnamen mit ersetzten Variablen
	 */
	public String replaceIndexVars(String indexName) {
		Matcher match = varPattern.matcher(indexName);
		StringBuffer sb = new StringBuffer();
		while (match.find()){
			String p = match.group(1);
			p = p.replace("{", "");
			p = p.replace("}", "");
			/* In p muss jetzt eine einfache Variable vorhanden sein */
			String rplTxt = ExecutionContext.getCurrentContext().getVariablesManager().getVariableAsString(p);
			if (rplTxt == null){
				throw new RuntimeException("VariableResolver.replaceIndexVars: Variable '" + p + "' nicht definiert!");
			}
			match.appendReplacement(sb, rplTxt);
		}
		match.appendTail(sb);
		return sb.toString();
	}
	
	public void update(String name, Object value, ReportNode node) {
		Matcher matcher = mapPattern.matcher(name);
		if(matcher.find()) {
			String containerName = matcher.group(1);
			String indexName = matcher.group(2);
			indexName = replaceIndexVars(indexName);// Workaround fuer das Problem der Index Namen, die Variablen enthalten.
			
			Object object = getVariable(containerName);
			if (object != null && object instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<Object,Object> container = (Map<Object,Object>) object;
				container.put(indexName, value);
			} else {
				throw new RuntimeException("Error while evaluating the parameter " + name + 
						". A Map was expected but "+object +" was returned.");
			}
		} else {
			ExecutionContext context = ExecutionContext.getCurrentContext();
			try {
				context.getVariablesManager().updateVariable(node, name, value);
			} catch(UndefinedVariableException|ImmutableVariableException e) {
				context.getVariablesManager().putVariable(context.getReportNodeTree().getParent(node), name, value);
			}
		}
	}
}
