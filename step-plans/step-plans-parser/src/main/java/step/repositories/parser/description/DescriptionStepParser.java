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
// Generated from DescriptionStep.g4 by ANTLR 4.5.3

    package step.repositories.parser.description;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class DescriptionStepParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.5.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, EQ=4, WORD=5, STRING=6, SPACE=7;
	public static final int
		RULE_parse = 0, RULE_expr = 1, RULE_keywordExpression = 2, RULE_setExpression = 3, 
		RULE_functionDeclarationExpression = 4, RULE_functionDeclarationEndExpression = 5, 
		RULE_assignment = 6, RULE_keywordParameter = 7, RULE_keywordName = 8, 
		RULE_attributeName = 9, RULE_attributeValue = 10, RULE_setValue = 11;
	public static final String[] ruleNames = {
		"parse", "expr", "keywordExpression", "setExpression", "functionDeclarationExpression", 
		"functionDeclarationEndExpression", "assignment", "keywordParameter", 
		"keywordName", "attributeName", "attributeValue", "setValue"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'Set'", "'Function'", "'EndFunction'", "'='"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, "EQ", "WORD", "STRING", "SPACE"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "DescriptionStep.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public DescriptionStepParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class ParseContext extends ParserRuleContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode EOF() { return getToken(DescriptionStepParser.EOF, 0); }
		public ParseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parse; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).enterParse(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).exitParse(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DescriptionStepVisitor ) return ((DescriptionStepVisitor<? extends T>)visitor).visitParse(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ParseContext parse() throws RecognitionException {
		ParseContext _localctx = new ParseContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_parse);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(24);
			expr();
			setState(25);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExprContext extends ParserRuleContext {
		public KeywordExpressionContext keywordExpression() {
			return getRuleContext(KeywordExpressionContext.class,0);
		}
		public SetExpressionContext setExpression() {
			return getRuleContext(SetExpressionContext.class,0);
		}
		public FunctionDeclarationExpressionContext functionDeclarationExpression() {
			return getRuleContext(FunctionDeclarationExpressionContext.class,0);
		}
		public FunctionDeclarationEndExpressionContext functionDeclarationEndExpression() {
			return getRuleContext(FunctionDeclarationEndExpressionContext.class,0);
		}
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).enterExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).exitExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DescriptionStepVisitor ) return ((DescriptionStepVisitor<? extends T>)visitor).visitExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExprContext expr() throws RecognitionException {
		ExprContext _localctx = new ExprContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(31);
			switch (_input.LA(1)) {
			case WORD:
			case STRING:
				{
				setState(27);
				keywordExpression();
				}
				break;
			case T__0:
				{
				setState(28);
				setExpression();
				}
				break;
			case T__1:
				{
				setState(29);
				functionDeclarationExpression();
				}
				break;
			case T__2:
				{
				setState(30);
				functionDeclarationEndExpression();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class KeywordExpressionContext extends ParserRuleContext {
		public KeywordNameContext keywordName() {
			return getRuleContext(KeywordNameContext.class,0);
		}
		public List<KeywordParameterContext> keywordParameter() {
			return getRuleContexts(KeywordParameterContext.class);
		}
		public KeywordParameterContext keywordParameter(int i) {
			return getRuleContext(KeywordParameterContext.class,i);
		}
		public KeywordExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_keywordExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).enterKeywordExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).exitKeywordExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DescriptionStepVisitor ) return ((DescriptionStepVisitor<? extends T>)visitor).visitKeywordExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final KeywordExpressionContext keywordExpression() throws RecognitionException {
		KeywordExpressionContext _localctx = new KeywordExpressionContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_keywordExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(33);
			keywordName();
			setState(37);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==WORD) {
				{
				{
				setState(34);
				keywordParameter();
				}
				}
				setState(39);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SetExpressionContext extends ParserRuleContext {
		public List<AssignmentContext> assignment() {
			return getRuleContexts(AssignmentContext.class);
		}
		public AssignmentContext assignment(int i) {
			return getRuleContext(AssignmentContext.class,i);
		}
		public SetExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_setExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).enterSetExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).exitSetExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DescriptionStepVisitor ) return ((DescriptionStepVisitor<? extends T>)visitor).visitSetExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SetExpressionContext setExpression() throws RecognitionException {
		SetExpressionContext _localctx = new SetExpressionContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_setExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(40);
			match(T__0);
			setState(42); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(41);
				assignment();
				}
				}
				setState(44); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==WORD );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionDeclarationExpressionContext extends ParserRuleContext {
		public List<KeywordParameterContext> keywordParameter() {
			return getRuleContexts(KeywordParameterContext.class);
		}
		public KeywordParameterContext keywordParameter(int i) {
			return getRuleContext(KeywordParameterContext.class,i);
		}
		public FunctionDeclarationExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionDeclarationExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).enterFunctionDeclarationExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).exitFunctionDeclarationExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DescriptionStepVisitor ) return ((DescriptionStepVisitor<? extends T>)visitor).visitFunctionDeclarationExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionDeclarationExpressionContext functionDeclarationExpression() throws RecognitionException {
		FunctionDeclarationExpressionContext _localctx = new FunctionDeclarationExpressionContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_functionDeclarationExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(46);
			match(T__1);
			setState(50);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==WORD) {
				{
				{
				setState(47);
				keywordParameter();
				}
				}
				setState(52);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionDeclarationEndExpressionContext extends ParserRuleContext {
		public FunctionDeclarationEndExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionDeclarationEndExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).enterFunctionDeclarationEndExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).exitFunctionDeclarationEndExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DescriptionStepVisitor ) return ((DescriptionStepVisitor<? extends T>)visitor).visitFunctionDeclarationEndExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionDeclarationEndExpressionContext functionDeclarationEndExpression() throws RecognitionException {
		FunctionDeclarationEndExpressionContext _localctx = new FunctionDeclarationEndExpressionContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_functionDeclarationEndExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(53);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AssignmentContext extends ParserRuleContext {
		public AttributeNameContext attributeName() {
			return getRuleContext(AttributeNameContext.class,0);
		}
		public TerminalNode EQ() { return getToken(DescriptionStepParser.EQ, 0); }
		public SetValueContext setValue() {
			return getRuleContext(SetValueContext.class,0);
		}
		public AssignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).enterAssignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).exitAssignment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DescriptionStepVisitor ) return ((DescriptionStepVisitor<? extends T>)visitor).visitAssignment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignmentContext assignment() throws RecognitionException {
		AssignmentContext _localctx = new AssignmentContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_assignment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(55);
			attributeName();
			setState(56);
			match(EQ);
			setState(57);
			setValue();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class KeywordParameterContext extends ParserRuleContext {
		public AttributeNameContext attributeName() {
			return getRuleContext(AttributeNameContext.class,0);
		}
		public TerminalNode EQ() { return getToken(DescriptionStepParser.EQ, 0); }
		public AttributeValueContext attributeValue() {
			return getRuleContext(AttributeValueContext.class,0);
		}
		public KeywordParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_keywordParameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).enterKeywordParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).exitKeywordParameter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DescriptionStepVisitor ) return ((DescriptionStepVisitor<? extends T>)visitor).visitKeywordParameter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final KeywordParameterContext keywordParameter() throws RecognitionException {
		KeywordParameterContext _localctx = new KeywordParameterContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_keywordParameter);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(59);
			attributeName();
			setState(60);
			match(EQ);
			setState(61);
			attributeValue();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class KeywordNameContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(DescriptionStepParser.STRING, 0); }
		public TerminalNode WORD() { return getToken(DescriptionStepParser.WORD, 0); }
		public KeywordNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_keywordName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).enterKeywordName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).exitKeywordName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DescriptionStepVisitor ) return ((DescriptionStepVisitor<? extends T>)visitor).visitKeywordName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final KeywordNameContext keywordName() throws RecognitionException {
		KeywordNameContext _localctx = new KeywordNameContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_keywordName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(63);
			_la = _input.LA(1);
			if ( !(_la==WORD || _la==STRING) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AttributeNameContext extends ParserRuleContext {
		public TerminalNode WORD() { return getToken(DescriptionStepParser.WORD, 0); }
		public AttributeNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attributeName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).enterAttributeName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).exitAttributeName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DescriptionStepVisitor ) return ((DescriptionStepVisitor<? extends T>)visitor).visitAttributeName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AttributeNameContext attributeName() throws RecognitionException {
		AttributeNameContext _localctx = new AttributeNameContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_attributeName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(65);
			match(WORD);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AttributeValueContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(DescriptionStepParser.STRING, 0); }
		public AttributeValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attributeValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).enterAttributeValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).exitAttributeValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DescriptionStepVisitor ) return ((DescriptionStepVisitor<? extends T>)visitor).visitAttributeValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AttributeValueContext attributeValue() throws RecognitionException {
		AttributeValueContext _localctx = new AttributeValueContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_attributeValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(67);
			match(STRING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SetValueContext extends ParserRuleContext {
		public AttributeNameContext attributeName() {
			return getRuleContext(AttributeNameContext.class,0);
		}
		public TerminalNode STRING() { return getToken(DescriptionStepParser.STRING, 0); }
		public SetValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_setValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).enterSetValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DescriptionStepListener ) ((DescriptionStepListener)listener).exitSetValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DescriptionStepVisitor ) return ((DescriptionStepVisitor<? extends T>)visitor).visitSetValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SetValueContext setValue() throws RecognitionException {
		SetValueContext _localctx = new SetValueContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_setValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(71);
			switch (_input.LA(1)) {
			case WORD:
				{
				setState(69);
				attributeName();
				}
				break;
			case STRING:
				{
				setState(70);
				match(STRING);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\tL\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t\13\4"+
		"\f\t\f\4\r\t\r\3\2\3\2\3\2\3\3\3\3\3\3\3\3\5\3\"\n\3\3\4\3\4\7\4&\n\4"+
		"\f\4\16\4)\13\4\3\5\3\5\6\5-\n\5\r\5\16\5.\3\6\3\6\7\6\63\n\6\f\6\16\6"+
		"\66\13\6\3\7\3\7\3\b\3\b\3\b\3\b\3\t\3\t\3\t\3\t\3\n\3\n\3\13\3\13\3\f"+
		"\3\f\3\r\3\r\5\rJ\n\r\3\r\2\2\16\2\4\6\b\n\f\16\20\22\24\26\30\2\3\3\2"+
		"\7\bF\2\32\3\2\2\2\4!\3\2\2\2\6#\3\2\2\2\b*\3\2\2\2\n\60\3\2\2\2\f\67"+
		"\3\2\2\2\169\3\2\2\2\20=\3\2\2\2\22A\3\2\2\2\24C\3\2\2\2\26E\3\2\2\2\30"+
		"I\3\2\2\2\32\33\5\4\3\2\33\34\7\2\2\3\34\3\3\2\2\2\35\"\5\6\4\2\36\"\5"+
		"\b\5\2\37\"\5\n\6\2 \"\5\f\7\2!\35\3\2\2\2!\36\3\2\2\2!\37\3\2\2\2! \3"+
		"\2\2\2\"\5\3\2\2\2#\'\5\22\n\2$&\5\20\t\2%$\3\2\2\2&)\3\2\2\2\'%\3\2\2"+
		"\2\'(\3\2\2\2(\7\3\2\2\2)\'\3\2\2\2*,\7\3\2\2+-\5\16\b\2,+\3\2\2\2-.\3"+
		"\2\2\2.,\3\2\2\2./\3\2\2\2/\t\3\2\2\2\60\64\7\4\2\2\61\63\5\20\t\2\62"+
		"\61\3\2\2\2\63\66\3\2\2\2\64\62\3\2\2\2\64\65\3\2\2\2\65\13\3\2\2\2\66"+
		"\64\3\2\2\2\678\7\5\2\28\r\3\2\2\29:\5\24\13\2:;\7\6\2\2;<\5\30\r\2<\17"+
		"\3\2\2\2=>\5\24\13\2>?\7\6\2\2?@\5\26\f\2@\21\3\2\2\2AB\t\2\2\2B\23\3"+
		"\2\2\2CD\7\7\2\2D\25\3\2\2\2EF\7\b\2\2F\27\3\2\2\2GJ\5\24\13\2HJ\7\b\2"+
		"\2IG\3\2\2\2IH\3\2\2\2J\31\3\2\2\2\7!\'.\64I";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
