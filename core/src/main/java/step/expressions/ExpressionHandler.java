package step.expressions;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import step.commons.conf.Configuration;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.miscellaneous.ValidationException;
import step.expressions.placeholder.PlaceHolderHandler;
import step.expressions.placeholder.VariableResolver;

public class ExpressionHandler {
		
	private static Logger logger = LoggerFactory.getLogger(ExpressionHandler.class);
	
	private final PlaceHolderHandler placeholderHandler;
	
	private static final GroovyClassLoader groovyLoader = new GroovyClassLoader();
		
	private final CompilerConfiguration groovyCompilerConfiguration;
	
	public ExpressionHandler(PlaceHolderHandler placeholderHandler) {
		this(placeholderHandler, "step.expressions.GroovyFunctions");
	}
	
	public ExpressionHandler(PlaceHolderHandler placeholderHandler, String baseScriptBase ) {
		super();
		
		this.placeholderHandler = placeholderHandler;

		groovyCompilerConfiguration = new CompilerConfiguration();
		if(baseScriptBase!=null) {
			groovyCompilerConfiguration.setScriptBaseClass(baseScriptBase);
		}
	}


	private Vector<Node> getChildElements(Node node) {
		Vector<Node> elements = new Vector<Node>();
		for (int i = 0; i < node.getAttributes().getLength(); i++) {
			if (node.getAttributes().item(i).getNodeType() == Node.ATTRIBUTE_NODE) {
				elements.add((Node)node.getAttributes().item(i));
			}
		}
		
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			if (list.item(i).getNodeType() == Node.ELEMENT_NODE) {
				elements.add((Node) list.item(i));
			}
		}
		return elements;
	}
	
	
	/**
	 * Holt die Inputparameter und leitet sie zur Evaluierung weiter.
	 * 
	 * @param input
	 * @param placeholderHandler Enthaelt eine Sammlung an resolvers.
	 * @return ersetzter input
	 */
	public Document handleInput(Document document) {
		Element root = document.getDocumentElement();
		Vector<Node> children = getChildElements(root);
		String initialValue = "";
		String replacedValue = "";
		for (Node child : children) {
			if (child.getNodeType() == Node.ATTRIBUTE_NODE) {
				initialValue = child.getNodeValue();
				replacedValue = evaluateAttributeParameter(initialValue);
				child.setNodeValue(replacedValue);
			} else if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getChildNodes().getLength() == 1 && child.getFirstChild() instanceof Text) {
					// TODO handle tags
				}
			}
		}
		return document;
	}

	public String evaluateAttributeParameter(String initialValue) {
		String replacedValue = replace(initialValue);
		return replacedValue;
	}

	public boolean handleCheck(String checkExpression) throws Exception {
		String rpl = replace("{" + checkExpression + "}");
		if (!(rpl.toUpperCase().equals("TRUE") || rpl.toUpperCase().equals("FALSE"))){
			throw new Exception("Expression did not return a boolean");
		} else {
			return new Boolean(rpl);
		}
	}

	private static final Pattern SET_VAR_PATTERN = Pattern.compile("[ ]*\\{(.+?)\\}[ ]*=[ ]*(.*)$");
	
	private static final Pattern SET_VAR_PATTERN_2 = Pattern.compile("[ ]*(.+?)[ ]*=[ ]*(.*)$");

	public void handleSet(String setExpression, ReportNode node) throws Exception {
		if(setExpression!=null) {
			Matcher matcher = SET_VAR_PATTERN.matcher(setExpression);
			Object value;
			String variableName;
			String expression;
			if(matcher.find()) {
				variableName = matcher.group(1);
				expression = matcher.group(2);
				value = replace("{" + expression + "}");
			} else {
				Matcher matcher2 = SET_VAR_PATTERN_2.matcher(setExpression);
				if(matcher2.find()) {
					variableName = matcher2.group(1);
					expression = matcher2.group(2);
					value = evaluate(expression);
				} else {
					throw new ValidationException("Falsches Format. Die set-Syntax muss lauten: set1 = \"\\{variablenName\\} = Ausdruck\", oder set1 = \"{container[index]} = Ausdruck\"");					
				}
			}
			VariableResolver resolver = new VariableResolver();
			resolver.update(variableName, value, node);
		}
	}

	private void evaluateSetAndUpdateVariable(ReportNode node, Matcher matcher, boolean setVarAsString) {
		String variableName = matcher.group(1);// Variablenname
		String expression = matcher.group(2);// Rest nach dem Gleichheitszeichen
		Object result = null;
			result = replace("{" + expression + "}");
			VariableResolver resolver = new VariableResolver();
			if(result!=null) {
				Object var = setVarAsString?result.toString():result;
				resolver.update(variableName, var, node);
			} else {
				throw new ValidationException("Das Resultat des Ausdrucks '" + expression + "' ergab null.");
			}
	}

	/* 
	 * TODO: Remove Workaround. Parse-Logik ueberarbeiten
	 */
	private String workaroundReplaceSingleSeparators(String aString) {
		
		String replacedString = aString;
		if (replacedString != null) {
			/* 
			 * Allfaellige Tausender-Separatoren aus Zahlen im String entfernen
			 * Anderenfalls kommt es zu einem Fehler wegen ungerader Anzahl von Apostrophs 
			 * Bsp.: 1'000'000.00 wird zu 1000000.00
			 */
			replacedString = replacedString.replaceAll("([0-9])(')([0-9])", "$1$3");
			
			if (!aString.equals(replacedString)) {
				logger.debug("Thousand separator has been removed (workaround)! " + "(orig:{} | new:{}", aString, replacedString);
			}
		}
		
		return replacedString;
		
	}


	/**
	 * Delegiert die Auswertung des Ausdrucks/Miniscripts/Befehls an die Groovy Shell.
	 * 
	 * @param expression
	 * @param placeholderHandler
	 * @param outputParameterMap
	 * @return
	 */
	public Object evaluate(String expression) {
		Object result = null;
		/* 
		 * Alle trivialen Ausdruecke (nur ein String, nur eine Zahl oder eine einfache Berechnung) werden
		 * vorab behandelt, ohne die ganze GroovyShell und das Parsen des Scripts auszufuehren.
		 */
		if ((result = trivialExpression(expression)) != null){
			logger.debug("Trivialausdruck: " + result);
			return result;
		}
		try {			
		
			/* verhindert Kompilierfehler bei einzeiligen Groovy-Scripts */
			expression = addSemicolonToClosingBrace(expression);

			logger.debug("Groovy evaluation:\n" + expression);
						
			/* Einbinden von placeholderHandler, Parameter und Variablen */
			Binding binding = new Binding(); 
			binding.setVariable("placeholderHandler", placeholderHandler);
			if(placeholderHandler.getOutputParameterMap()!=null) {
				for(Entry<String, String> outputParameterEntry:placeholderHandler.getOutputParameterMap().entrySet()) {
					String value =  outputParameterEntry.getValue();
					binding.setVariable(outputParameterEntry.getKey(), value);
				}
			}
			
			/* Variablen auf den neusten Stand bringen */
			ExecutionContext context = ExecutionContext.getCurrentContext();
			Map<String, Object> variableMap = context.getVariablesManager().getAllVariables();
			for(Entry<String, Object> varEntry : variableMap.entrySet()) {
				Object value =  varEntry.getValue();
				binding.setVariable(varEntry.getKey(), value);
			}

			long t1 = System.currentTimeMillis();	
			try {
				if(Configuration.getInstance().getPropertyAsBoolean("tec.expressions.usecache")) {
					GroovyPoolEntry entry = GroovyPool.getINSTANCE().borrowShell(expression);
					try {
						Script script = entry.getScript();
						script.setBinding(binding);
						result = script.run();
					} finally {
						if(entry!=null && entry.getScript()!=null) {
							// Release bindings to avoid references to be kept by the pool
							entry.getScript().setBinding(new Binding());							
						}
						GroovyPool.getINSTANCE().returnShell(entry);
					}
				} else {
					GroovyShell shell = new GroovyShell(groovyLoader, binding, groovyCompilerConfiguration);
					result = shell.evaluate(expression);
				}
			} catch (Exception e) {
				logger.error("An error occurred while evaluation groovy expression " + expression, e);
				throw e;
			}
			long duration = System.currentTimeMillis()-t1;
			
			int warnThreshold = Configuration.getInstance().getPropertyAsInteger("tec.expressions.warningthreshold");
			if(duration > warnThreshold) {
				logger.warn("Groovy-Evaluation of following expression took " + duration + ".ms: "+ expression);
			} else {
				logger.debug("Groovy-Evaluation of following expression took " + duration + ".ms: "+ expression);
			}
			
			logger.debug("Groovy Result:\n" + result);
			return result;
		} catch (CompilationFailedException cfe) {
			throw new RuntimeException(
					"Fehler im Groovyausdruck: " + expression, cfe);
		} catch (Exception e){
			throw new RuntimeException(
					"Fehler waehrend Evaluierung: " + expression, e);

		}
	}

	static final Pattern STR_PATTERN = Pattern.compile("[']([^']*)[']");
	static final Pattern XPR_PATTERN = Pattern.compile("[ \\+\\-\\*/%\\(\\)0-9.&\\|<>=\\?:]+");
	/**
	 * Triviale Ausdrücke sollten eher mit GroovyShell.evaluate als via 
	 * GroovyShell.parse ermittelt werden. Reine Strings und Zahlen sollen gar
	 * nicht evaluiert werden.
	 * Hier wird ermittelt, ob der Ausdruck 
	 * 
	 * <li> eine Zahl oder
	 * <li> ein einfacher Boolean oder
	 * <li> ein einfacher String oder
	 * <li> ein einfacher Ausdruck
	 * 
	 * ist.<br><br>
	 * 
	 * Von der Performance her sind einfache Strings und Zahlen tausend mal schneller
	 * als Ausdruecke und 10-Tausend mal schneller als ein Script, das geparst werden muss.
	 * Ein einfacher Ausdruck kann immerhin noch 10-20 mal schneller als ein Script abgearbeitet
	 * werden.
	 * 
	 * @param expr Zu analysierender Ausdruck
	 * @return     Die Rueckgabe ist null, falls geparst werden muss, sonst wird direkt das Resultat 
	 *             zurueck geliefert.
	 */
	private Object trivialExpression(String expr){
		if (expr == null) return "";
		expr = expr.trim();
		/* Zuerst schauen, ob leer */
		if (expr.isEmpty()){
			return expr;
		}
		/* dann schauen, ob es eine einfache Zahl ist */
		try{
			return Long.parseLong(expr);
		} catch (Exception e){
			// do nothing
		}
		try{
			return Double.parseDouble(expr);
		} catch (Exception e){
			// do nothing
		}
		/* dann schauen, ob es eine einfache Zuweisung true oder false ist */
		if (expr.equals("true")) return true;
		if (expr.equals("false")) return false;
		/* dann schauen, ob es ein einfacher String ist */
		Matcher strMatcher = STR_PATTERN.matcher(expr);
		if (strMatcher.matches()){
			return strMatcher.group(1);
		}
		/* dann schauen, ob es ein Ausdruck ist */
		Matcher xprMatcher = XPR_PATTERN.matcher(expr);
		if (xprMatcher.matches()){
			return new GroovyShell(groovyLoader).evaluate(expr);
		}
		return null;
	}


	/* 
	 * Identifier beginnen mit einem Buchstaben danach sind auch Ziffern, Underscore und (wegen den Platzhaltern)
	 * Punkte erlaubt. Optional haben sie maximal eine eckige Klammer in welcher mindestens ein Zeichen sein
	 * muss. Die definierten Gruppen isolieren aus '{ var1  [  idx  ]  } '
	 * Gruppe 1: '{ var1  [  idx  ]  }' (diese ist wichtig, da auf die Position der oeffnenden Klammer der Entscheid 
	 *                                   Script Nonscript gemacht wird). 
	 * Gruppe 2: 'var1  [  idx  ]'
	 * Gruppe 3: 'var1'
	 * Gruppe 4: '[  idx  ]'
	 * Gruppe 5: '  idx  '
	 */
	private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("(\\{[ ]*(([A-Za-z][\\w\\.]*)[ ]*(\\[(.+?)\\])?)[ ]*\\})");
	/* 
	 * Datumsarithmetik hat die Syntax: today + 1.weeks, etc...
	 * Wichtig ist nur Gruppe 1 welche aus '{     now  +   3.seconds  - 5.minutes    }'
	 * Die Gruppen isolieren aus '{     now  +   3.seconds  - 5.minutes    }'
	 * Gruppe 1: '{     now  +   3.seconds  - 5.minutes    }' (wichtig fuer die Position der oeffnenden Klammer)
	 * Gruppe 2: 'now  +   3.seconds  - 5.minutes'
	 * Gruppe 3: 'now'
	 * Gruppe 4 und 5: interessieren nicht
	 */
	private static final Pattern DATE_ARITH_PATTERN = Pattern.compile("(\\{[ ]*((today|first|last|now)([ ]*[+-]*[ ]*[0-9][0-9]*(.seconds?|.minutes?|.hours?|.days?|.weeks?|.months?|.years?))*)[ ]*\\})");
	/*
	 * Findet alle Strings im Input
	 */
	private static final Pattern GROOVY_STRING_PATTERN = Pattern.compile("[']([^']*)[']");

	/**
	 * Fuer Groovy werden nach schliessenden Klammern (ausserhalb Strings) jeweils noch ein 
	 * Semikolon eingefuegt. Grund:
	 * Groovy erzeugt Kompilierfehler bei verschachtelten Bloecken. Dies ist vermutlich 
	 * ein Parserfehler von Groovy, da Java dies fehlerlos verarbeitet.
	 * Groovy Strings muessen von diesen Ersetzungen natuerlich ausgenommen werden. 
	 * 
	 * @param expression Groovy Ausdruck ohne New Lines.
	 * @return Groovy Ausdruck mit zusaetzlichen Semikolons nach schliessenden geschweiften Klammern.
	 */
	private static String addSemicolonToClosingBrace(String expression) {
		StringBuffer sb = new StringBuffer();
		/* 
		 * Ersetzen der schliessenden Klammern mit '%%%%'
		 */
		Matcher match = GROOVY_STRING_PATTERN.matcher(expression);
		while (match.find()){
			String p = match.group(1);
			p = p.replace("}", "%%%%");/* schliessende Klammer in String */
			p = "'" + p + "'";
			match.appendReplacement(sb, p);
		}
		match.appendTail(sb);
		expression = sb.toString();
		/* Ersetzen aller verbleibender schliessende Klammern mit '};' */
		expression = expression.replaceAll("}", "};");
		/* Rueckersetzen der geretteten Klammern in Strings*/
		expression = expression.replaceAll("%%%%", "}");
		return expression;
	}
	
	/**
	 * Enthaelt fuer alle wichtigen Positionen im auszuwertenden Input (oeffnende Braces '{'),
	 * ob diese Position sich innerhalb eines Groovyscripts befindet oder nicht.
	 * Gleichzeitig wird fuer dieselbe Postion vermerkt, ob sie sich innerhalb eines
	 * Strings (Quotes, ' ' ) befindet oder nicht.
	 * 
	 * @author s5r
	 *
	 */
	static class RplStrategy{
		HashMap<Integer, Boolean> scriptMap = new HashMap<>();
		HashMap<Integer, Boolean> stringMap = new HashMap<>();
	}
	
	/**
	 * Enthaelt einen Bereich des Inputs (beg, end) und den Vermerk ob dieser Bereich
	 * unveraendert stehen gelassen werden kan oder an Groovy geschickt werden muss.
	 * muessen.
	 * 
	 * @author s5r
	 *
	 */
	static class ScriptNonscript{
		Boolean isScript = false;
		Integer beg = 0;
		Integer end = 0;
		
		public ScriptNonscript(Boolean isScript, Integer beg, Integer end){
			this.isScript = isScript;
			this.end = end;
		}
	}
	
	/**
	 * Das Original enthaelt Platzhalter, Scripte, Datumsarithmetik, Identifier und Text, der nicht ersetzt werden muss.
	 * Zuerst werden die Identifier ersetzt und zwar je nachdem sie sich in einem Script befinden anders als wenn es
	 * sich um eine reine Ersetzung handelt.
	 * Danach geschieht dasselbe mit der Datumsarithmetik.
	 * Am Schluss werden die restlichen Klammerausdrücke an Groovy weitergereicht.
	 * 
	 * @param original Original String gemischt mit Scripts, Platzhaltern, Variablen und nicht zu ersetzendem Text.
	 * @param strat Klasse enthaelt die Ersetzungsstrategie: Nicht ersetzen, ersetzen mit Hochkomma oder einfach ersetzen.
	 * @return Der vollständig ausgewertete und ersetzte Original Input.
	 */
	private String replace(String original) {
		
//		original = replaceOutputParametersIfPossible(original);
		
		/* 
		 * TODO: Remove Workaround. Parse-Logik ueberarbeiten
		 */
		original = workaroundReplaceSingleSeparators(original);				
		
		StringBuffer sb = new StringBuffer();
		String replaced = "";
		String rplTxt = "";
		Boolean inScript = false;
		Boolean inString = false;
		/* Zuerst alle Identifier gemaess Ersetzungsstrategie ersetzen */
		RplStrategy strat = getStrategy(original);
		Matcher match = IDENTIFIER_PATTERN.matcher(original);
		while (match.find()){
			String p = match.group(2); 
			inScript = strat.scriptMap.containsKey(match.start(1));
			inString = strat.stringMap.containsKey(match.start(1));
			if (!inString){
				if (inScript) rplTxt = "this.binding.getVariable('placeholderHandler').resolve(\"" + p + "\")";// In scripts muessen Strings erscheinen
				else rplTxt = placeholderHandler.resolve(p.trim()); // In der Ersetzungsprache ohne Hochkomma
				
				if(rplTxt!=null) {
					rplTxt = workaroundReplaceSingleSeparators(rplTxt);
					match.appendReplacement(sb, rplTxt);
				} else {
					match.appendReplacement(sb, original);
				}
			}
		}
		match.appendTail(sb);
		replaced = sb.toString();
		sb = new StringBuffer();
		
		/* Danach die Datumsarithmetik gemaess Ersetzungsstrategie ersetzen */
		strat = getStrategy(replaced);
		match = DATE_ARITH_PATTERN.matcher(replaced);
		while (match.find()){
			String p = match.group(2); 
			inScript = strat.scriptMap.containsKey(match.start(1));
			inString = strat.stringMap.containsKey(match.start(1));
			if (!inString){
				if (inScript){
					rplTxt = "this.binding.getVariable('placeholderHandler').resolve(\""+p.trim()+"\")";// In scripts muessen Strings erscheinen
				} else {
					rplTxt = placeholderHandler.resolve(p.trim()); // In der Ersetzungsprache ohne Hochkomma
				}
				match.appendReplacement(sb, rplTxt);
			}
		}
		match.appendTail(sb);
		replaced = sb.toString();

		/* Jetzt haben wir nur noch Auswertungen für Groovy */
		Map<Integer, ScriptNonscript> scripts = findScripts(replaced);
		if(scripts.size()>0) {
			String txt = "";
			sb = new StringBuffer();
			for (Integer key : scripts.keySet()){
				int beg = key;
				ScriptNonscript scriptOrNot = scripts.get(key);
				int end = scriptOrNot.end;
				if (end < replaced.length()) end++;
				txt = replaced.substring(beg, end);
				if (scriptOrNot.isScript){
					txt = txt.substring(1, txt.length()-1);
					Object result = evaluate(txt);
					if (result != null){
						txt = result.toString();
					}
				}
				sb.append(txt);
			}
			//if (sb.toString().length() == 0) sb.append(replaced);
			// replaced = sb.toString();
			return workaroundReplaceSingleSeparators(sb.toString());
		} else {
			return workaroundReplaceSingleSeparators(replaced);
		}
	}

	/**
	 * Hinterlegt fuer interessante Positionen (oeffnende Klammer) sowohl ob sie sich in
	 * einem Script oder einem String befindet.
	 * 
	 * @param str
	 * @return
	 */
	private RplStrategy getStrategy(String str) {
		/* 1. Groovyscripts und Groovystrings finden */
		RplStrategy strat = findScriptsAndStrings(str);
		return strat;
	}

	/**
	 * Hinterlegt fuer alle interessanten Positionen (oeffnende geschweifte Klammer) in einer
	 * HashMap, ob die Position sich in einem Script befindet oder nicht. Gleichzeitig wird
	 * hinterlegt, ob die Position innerhalb eines Strings ist oder nicht.
	 * 
	 * @param str Der gesamte zu analysierende Input 
	 * @return HashMap mit allen Positionen und dem Vermerk, ob es innerhalb eines Scripts ist oder nicht.
	 */
	private RplStrategy findScriptsAndStrings(String str) {
		RplStrategy rpl = new RplStrategy();
		int leftBraces = 0;
		int rightBraces = 0;
		Boolean inString = false;
		Character chr;
		
		if (str != null && str.length()>0){
			for (int pos = 0; pos < str.length(); pos++){
				chr = str.charAt(pos);
				// Zaehlen der Klammern und festlegen ob innerhalb eines Groovy Strings
				if (chr == '{'){
					if (!inString){
						leftBraces++;
						if (leftBraces > 1){
							rpl.scriptMap.put(pos, true);// markieren, dass die Klammer in einem Script ist
						}
					}
					else rpl.stringMap.put(pos, true);// markieren, dass die Klammer in einem String ist
				}
				else if (chr == '}'){
					if (!inString) rightBraces++;
				}
				else if (chr == '\''){
					inString = !inString;
				}
				// Ende des Scripts erreicht?
				if (leftBraces > 0 && leftBraces == rightBraces){
					leftBraces = 0;
					rightBraces = 0;
				}
			}
			if (inString){
				throw new RuntimeException("Unbalanced quotes found!");
			}
			if (leftBraces != rightBraces){
				throw new RuntimeException("Unbalanced braces '{}' found!");
			}
		}
		return rpl;
	}

	/**
	 * Hinterlegt in einer Map die Regionen die stehen gelassen werden muessen und
	 * jene die an Groovy weitergereicht werden muessen.
	 * 
	 * @param str Der gesamte zu analysierende Input 
	 * @return HashMap aller verbleibender Groovyscripte.
	 */
	private Map<Integer, ScriptNonscript> findScripts(String str) {
		Map<Integer, ScriptNonscript> map = new TreeMap<>();
		int leftBraces = 0;
		int rightBraces = 0;
		int scriptBeg = 0;
		int scriptEnd = 0;
		int nonscriptBeg = 0;
		int nonscriptEnd = 0;
		Boolean inString = false;
		Boolean inScript = false;
		Character chr;
		
		if (str != null && str.length()>0){
			for (int pos = 0; pos < str.length(); pos++){
				chr = str.charAt(pos);
				// Zaehlen der Klammern und festlegen ob innerhalb eines Groovy Strings
				if (chr == '{'){
					if (!inString){
						leftBraces++;
						if (leftBraces == 1){
							scriptBeg = pos;
							nonscriptEnd = pos - 1;
							map.put(nonscriptBeg, new ScriptNonscript(false, nonscriptBeg, nonscriptEnd));
							inScript = true;
						}
					}
				}
				else if (chr == '}'){
					if (!inString){
						rightBraces++;
						if (leftBraces > 0 && leftBraces == rightBraces){
							scriptEnd = pos;
							map.put(scriptBeg, new ScriptNonscript(true, scriptEnd, scriptEnd));
							scriptBeg = 0;
							scriptEnd = 0;
							leftBraces = 0;
							rightBraces = 0;
							nonscriptBeg = pos + 1;
						}
					}
				}
				else if (chr == '\''){
					inString = !inString;
				}
				if (pos == 0 && !inScript){
					nonscriptBeg = 0;
				}
			}
			if (nonscriptBeg != 0 && nonscriptBeg != str.length()){
				map.put(nonscriptBeg, new ScriptNonscript(false, nonscriptBeg, str.length()));
			}
			if (inString){
				throw new RuntimeException("Unbalanced quote found!");
			}
			if (leftBraces != rightBraces){
				throw new RuntimeException("Unbalanced braces '{}' found!");
			}
				
		}
		return map;
	}
}
