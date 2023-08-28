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

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import step.artefacts.Assert;
import step.artefacts.Assert.AssertOperator;
import step.artefacts.CallFunction;
import step.artefacts.Export;
import step.artefacts.Set;
import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.repositories.parser.expected.ExpectedStepBaseVisitor;
import step.repositories.parser.expected.ExpectedStepLexer;
import step.repositories.parser.expected.ExpectedStepParser;
import step.repositories.parser.expected.ExpectedStepParser.AssignmentContext;
import step.repositories.parser.expected.ExpectedStepParser.CheckExprContext;
import step.repositories.parser.expected.ExpectedStepParser.ExportExpressionContext;
import step.repositories.parser.expected.ExpectedStepParser.ParseContext;
import step.repositories.parser.steps.ExpectedStep;

@StepParserExtension
public class CustomExpectedStepParser implements StepParser<ExpectedStep> {

	@Override
	public int getParserScoreForStep(AbstractStep step) {
		return (step instanceof ExpectedStep)?10:0;
	}

	@Override
	public void parseStep(ParsingContext parsingContext, ExpectedStep step) {
		String expression = step.getValue();
		try {
			ParseContext context = parse(expression);			
			Visitor visitor = new Visitor(parsingContext);
			Object o = visitor.visit(context.getChild(0));
			if(o!=null && o instanceof CallFunction) {
				parsingContext.addArtefactToCurrentParent((AbstractArtefact) o);
			}
		} catch(Exception e) {
			parsingContext.addParsingError(e.getMessage());
		}
	}
	
	private ParseContext parse(String expression) {
		ExpectedStepLexer lexer = new ExpectedStepLexer(new ANTLRInputStream(expression));
		ExpectedStepParser parser = new ExpectedStepParser(new CommonTokenStream(lexer));
		parser.addErrorListener(new BaseErrorListener() {
	        @Override
	        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
	            throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
	        }
	    });
		return parser.parse();
		
	}
	
	public static class Visitor extends ExpectedStepBaseVisitor<Object> {

		JsonObjectBuilder builder = Json.createObjectBuilder();;
		
		ParsingContext parsingContext;
		
		public Visitor(ParsingContext parsingContext) {
			super();
			this.parsingContext = parsingContext;
		}

		@Override
		public Object visitCheckExpr(CheckExprContext ctx) {
			Assert assert_ = new Assert();
			String operator = ctx.op.getText();
			
			String key = CustomDescriptionStepParser.stripQuotesIfNeeded(ctx.outputAttributeName().getText());
			
			if(ctx.NOT()!=null) {
				//assert_.setNegate(true);
				assert_.setDoNegate(new DynamicValue<Boolean>(true));
			} 
			
			assert_.getActual().setValue(key);
			assert_.getExpected().setDynamic(true);
			if(ctx.attributeValue() != null) {
				// can be null for 'isNull' operator
				assert_.getExpected().setExpression(ctx.attributeValue().getText());
			} else{
				assert_.getExpected().setDynamic(false);
				assert_.getExpected().setValue(null);
			}

			if(operator.equals("=")) {
				assert_.setOperator(AssertOperator.EQUALS);
			} else if(operator.equals("~")) {
				assert_.setOperator(AssertOperator.MATCHES);
			} else if(operator.equals("contains")) {
				assert_.setOperator(AssertOperator.CONTAINS);
			} else if(operator.equals("beginsWith")) {
				assert_.setOperator(AssertOperator.BEGINS_WITH);
			} else if(operator.equals("endsWith")) {
				assert_.setOperator(AssertOperator.ENDS_WITH);
			} else if(operator.equals("<")){
				assert_.setOperator(AssertOperator.LESS_THAN);
			} else if(operator.equals("<=")){
				assert_.setOperator(AssertOperator.LESS_THAN_OR_EQUALS);
			} else if(operator.equals(">")) {
				assert_.setOperator(AssertOperator.GREATER_THAN);
			} else if(operator.equals(">=")){
				assert_.setOperator(AssertOperator.GREATER_THAN_OR_EQUALS);
			} else if(operator.equals("isNull")){
				assert_.setOperator(AssertOperator.IS_NULL);
			}
			
			parsingContext.addArtefactToCurrentParent(assert_);
			assert_.addCustomAttribute("check", ctx.getText());
			return super.visitCheckExpr(ctx);
		}

		@Override
		public Object visitAssignment(AssignmentContext ctx) {
			Set result = new Set();
			
			String key = ctx.attributeName().getText();
			String value = ctx.setValue().getText();
			
			result.setKey(new DynamicValue<String>(key));
			
			if(value.startsWith("\"")) {
				result.setValue(new DynamicValue<String>(value,""));				
			} else {
				// TODO remove this as of release 3.7.x
				result.setValue(new DynamicValue<String>("output.getString('"+value+"')",""));		
			}
			
			parsingContext.addArtefactToCurrentParent(result);
			
			return super.visitAssignment(ctx);
		}

		@Override
		public Object visitExportExpression(ExportExpressionContext ctx) {
			Export export = new Export();
			ctx.controlParameter().forEach(parameter->{
				if(parameter.attributeName().getText().equals("File")) {
					export.setFile(new DynamicValue<>(parameter.setValue().getText(), ""));
				} else if(parameter.attributeName().getText().equals("Value")) {
					export.setValue(new DynamicValue<>(parameter.setValue().getText(), ""));
				} else if(parameter.attributeName().getText().equals("Prefix")) {
					export.setPrefix(new DynamicValue<>(parameter.setValue().getText(), ""));
				} else if(parameter.attributeName().getText().equals("Filter")) {
					export.setFilter(new DynamicValue<>(parameter.setValue().getText(), ""));
				} 
			});
			
			parsingContext.addArtefactToCurrentParent(export);
			return super.visitExportExpression(ctx);
		}
	}

}
