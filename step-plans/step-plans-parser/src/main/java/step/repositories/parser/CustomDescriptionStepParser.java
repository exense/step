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
package step.repositories.parser;

import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.bson.types.ObjectId;

import ch.exense.commons.app.Configuration;
import step.artefacts.CallFunction;
import step.artefacts.Sequence;
import step.artefacts.Set;
import step.core.artefacts.AbstractArtefact;
import ch.exense.commons.core.model.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.plugins.functions.types.CompositeFunction;
import step.repositories.parser.description.DescriptionStepBaseVisitor;
import step.repositories.parser.description.DescriptionStepLexer;
import step.repositories.parser.description.DescriptionStepParser;
import step.repositories.parser.description.DescriptionStepParser.AssignmentContext;
import step.repositories.parser.description.DescriptionStepParser.FunctionDeclarationEndExpressionContext;
import step.repositories.parser.description.DescriptionStepParser.FunctionDeclarationExpressionContext;
import step.repositories.parser.description.DescriptionStepParser.KeywordExpressionContext;
import step.repositories.parser.description.DescriptionStepParser.KeywordParameterContext;
import step.repositories.parser.description.DescriptionStepParser.ParseContext;
import step.repositories.parser.steps.DescriptionStep;

@StepParserExtension
public class CustomDescriptionStepParser implements StepParser<DescriptionStep> {

	@Override
	public int getParserScoreForStep(AbstractStep step) {
		return (step instanceof DescriptionStep) ? 10 : 0;
	}

	@Override
	public void parseStep(ParsingContext parsingContext, DescriptionStep step) {
		String expression = step.getValue();
		try {
			ParseContext context = parse(expression);
			Visitor visitor = new Visitor(parsingContext);
			visitor.visit(context.getChild(0));
		} catch (Exception e) {
			parsingContext.addParsingError(e.getMessage());
		}
	}

	private ParseContext parse(String expression) {
		BaseErrorListener baseErrorListener = new BaseErrorListener() {
			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
					int charPositionInLine, String msg, RecognitionException e) {
				throw new IllegalStateException(msg, e);
			}
		};

		DescriptionStepLexer lexer = new DescriptionStepLexer(new ANTLRInputStream(expression));
		lexer.addErrorListener(baseErrorListener);
		DescriptionStepParser parser = new DescriptionStepParser(new CommonTokenStream(lexer));
		parser.addErrorListener(baseErrorListener);
		return parser.parse();
	}

	public static class Visitor extends DescriptionStepBaseVisitor<Object> {

		private static final String FUNCTION_ATTRIBUTES = "functionAttributes";

		private static final String MAPPING_PROPERTY = "step.repositories.parser.attributes.mapping";

		JsonObjectBuilder builder = Json.createObjectBuilder();;

		ParsingContext parsingContext;

		public Visitor(ParsingContext parsingContext) {
			super();
			this.parsingContext = parsingContext;
		}

		@Override
		public Object visitAssignment(AssignmentContext ctx) {
			super.visitAssignment(ctx);

			Set set = new Set();
			set.setKey(new DynamicValue<String>(ctx.attributeName().getText()));
			set.setValue(new DynamicValue<>(ctx.setValue().getText(), ""));

			parsingContext.addArtefactToCurrentParent(set);
			return set;
		}

		@Override
		public Object visitFunctionDeclarationExpression(FunctionDeclarationExpressionContext ctx) {
			Map<String, String> attributes = new HashMap<>();
			ctx.keywordParameter().forEach(p -> attributes.put(p.attributeName().getText(),
					p.attributeValue().getText().substring(1, p.attributeValue().getText().length() - 1)));

			Sequence rootSequence = new Sequence();

			// TODO put the attributes map to an intermediary stack as it doesn't belong to
			// the artefact
			rootSequence.addCustomAttribute(FUNCTION_ATTRIBUTES, attributes);
			parsingContext.pushArtefact(rootSequence);

			return super.visitFunctionDeclarationExpression(ctx);
		}

		@Override
		public Object visitFunctionDeclarationEndExpression(FunctionDeclarationEndExpressionContext ctx) {
			AbstractArtefact rootArtefact = parsingContext.popCurrentArtefact();

			// TODO put the attributes map to an intermediary stack as it doesn't belong to
			// the artefact
			@SuppressWarnings("unchecked")
			Map<String, String> attributes = (Map<String, String>) rootArtefact.getCustomAttribute(FUNCTION_ATTRIBUTES);
			rootArtefact.getCustomAttributes().remove(FUNCTION_ATTRIBUTES);

			Plan plan = new Plan(rootArtefact);
			parsingContext.getPlanAccessor().save(plan);
			
			CompositeFunction function = new CompositeFunction();
			function.setId(new ObjectId());
			function.setPlanId(plan.getId().toString());
			function.setAttributes(attributes);
			function.setSchema(Json.createObjectBuilder().build());
			FunctionAccessor functionAccessor = parsingContext.getFunctionAccessor();
			functionAccessor.save(function);
			return super.visitFunctionDeclarationEndExpression(ctx);
		}

		@Override
		public Object visitKeywordExpression(KeywordExpressionContext ctx) {
			Configuration configuration = parsingContext.getConfiguration();

			String mapping = configuration.getProperty(MAPPING_PROPERTY);

			String keywordFullName = stripQuotesIfNeeded(ctx.keywordName().getText());

			super.visitKeywordExpression(ctx);
			CallFunction result = new CallFunction();

			String keywordSelector = null;
			
			if (keywordFullName.contains(".")) {
				String[] split = keywordFullName.split("\\.");
				if (split.length != 2) {
					parsingContext.addParsingError("Invalid keyword '" + keywordFullName
							+ "' The keyword should follow the syntax: application.keywordname");
				} else {
					String application = split[0];
					String keywordName = split[1];
					keywordSelector = "{\"" + Function.APPLICATION + "\":\"" + application + "\",\"name\":\""
							+ keywordName + "\"";
				}
			} else {
				keywordSelector = "{\"name\":\"" + keywordFullName + "\"";
			}

			if (mapping!=null && !mapping.isEmpty()) {
				for (String pair : mapping.split(";")) {
					String[] split = pair.split(",");
					if (split.length != 2) {
						parsingContext.addParsingError("Invalid mapping \""+mapping+"\"");
					} else {
						String func_attribute = split[0];
						String parameter = split[1];

						keywordSelector += ", \"" + func_attribute + "\":{ \"dynamic\" : true, \"expression\" : \""
								+ parameter + "\" }";
					}
				}
			}
			keywordSelector += "}";

			result.getFunction().setValue(keywordSelector);
			result.setArgument(new DynamicValue<String>(builder.build().toString()));

			parsingContext.addArtefactToCurrentParent((AbstractArtefact) result);
			return result;
		}

		@Override
		public Object visitKeywordParameter(KeywordParameterContext ctx) {
			String key = stripQuotesIfNeeded(ctx.attributeName().getText());
			String value = ctx.attributeValue().getText();

			// builder.add(key, value.substring(1,value.length()-1));
			JsonObjectBuilder dynamicAttribute = Json.createObjectBuilder();
			;
			dynamicAttribute.add("dynamic", true);
			dynamicAttribute.add("expression", value);
			builder.add(key, dynamicAttribute);

			return super.visitKeywordParameter(ctx);
		}
	}
	
	protected static String stripQuotesIfNeeded(String str) {
		if(str.startsWith("\"") && str.endsWith("\"")) {
			str = str.substring(1, str.length()-1);
		}
		return str;
	}

}
