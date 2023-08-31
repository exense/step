// Generated from ExpectedStep.g4 by ANTLR 4.5.3

    package step.repositories.parser.expected;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class ExpectedStepParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.5.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, NOT=3, EQ=4, REGEX=5, CONTAINS=6, BEGINS=7, ENDS=8, GREATER_THAN_OR_EQUALS=9, 
		GREATER_THAN=10, LESS_THAN_OR_EQUALS=11, LESS_THAN=12, IS_NULL=13, NUM=14, 
		BOOL=15, WORD=16, STRING=17, SPACE=18;
	public static final int
		RULE_parse = 0, RULE_expr = 1, RULE_checkExpression = 2, RULE_setExpression = 3, 
		RULE_assignment = 4, RULE_exportExpression = 5, RULE_controlParameter = 6, 
		RULE_outputAttributeName = 7, RULE_attributeName = 8, RULE_setValue = 9, 
		RULE_attributeValue = 10;
	public static final String[] ruleNames = {
		"parse", "expr", "checkExpression", "setExpression", "assignment", "exportExpression", 
		"controlParameter", "outputAttributeName", "attributeName", "setValue", 
		"attributeValue"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'Set'", "'Export'", null, "'='", "'~'", "'contains'", "'beginsWith'", 
		"'endsWith'", "'>='", "'>'", "'<='", "'<'", "'isNull'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, "NOT", "EQ", "REGEX", "CONTAINS", "BEGINS", "ENDS", 
		"GREATER_THAN_OR_EQUALS", "GREATER_THAN", "LESS_THAN_OR_EQUALS", "LESS_THAN", 
		"IS_NULL", "NUM", "BOOL", "WORD", "STRING", "SPACE"
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
	public String getGrammarFileName() { return "ExpectedStep.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public ExpectedStepParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class ParseContext extends ParserRuleContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode EOF() { return getToken(ExpectedStepParser.EOF, 0); }
		public ParseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parse; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).enterParse(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).exitParse(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpectedStepVisitor ) return ((ExpectedStepVisitor<? extends T>)visitor).visitParse(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ParseContext parse() throws RecognitionException {
		ParseContext _localctx = new ParseContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_parse);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(22);
			expr();
			setState(23);
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
		public List<CheckExpressionContext> checkExpression() {
			return getRuleContexts(CheckExpressionContext.class);
		}
		public CheckExpressionContext checkExpression(int i) {
			return getRuleContext(CheckExpressionContext.class,i);
		}
		public List<SetExpressionContext> setExpression() {
			return getRuleContexts(SetExpressionContext.class);
		}
		public SetExpressionContext setExpression(int i) {
			return getRuleContext(SetExpressionContext.class,i);
		}
		public List<ExportExpressionContext> exportExpression() {
			return getRuleContexts(ExportExpressionContext.class);
		}
		public ExportExpressionContext exportExpression(int i) {
			return getRuleContext(ExportExpressionContext.class,i);
		}
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).enterExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).exitExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpectedStepVisitor ) return ((ExpectedStepVisitor<? extends T>)visitor).visitExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExprContext expr() throws RecognitionException {
		ExprContext _localctx = new ExprContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_expr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(30);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << WORD) | (1L << STRING))) != 0)) {
				{
				setState(28);
				switch (_input.LA(1)) {
				case WORD:
				case STRING:
					{
					setState(25);
					checkExpression();
					}
					break;
				case T__0:
					{
					setState(26);
					setExpression();
					}
					break;
				case T__1:
					{
					setState(27);
					exportExpression();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(32);
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

	public static class CheckExpressionContext extends ParserRuleContext {
		public CheckExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_checkExpression; }
	 
		public CheckExpressionContext() { }
		public void copyFrom(CheckExpressionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class CheckExprContext extends CheckExpressionContext {
		public Token op;
		public OutputAttributeNameContext outputAttributeName() {
			return getRuleContext(OutputAttributeNameContext.class,0);
		}
		public TerminalNode EQ() { return getToken(ExpectedStepParser.EQ, 0); }
		public TerminalNode REGEX() { return getToken(ExpectedStepParser.REGEX, 0); }
		public TerminalNode CONTAINS() { return getToken(ExpectedStepParser.CONTAINS, 0); }
		public TerminalNode BEGINS() { return getToken(ExpectedStepParser.BEGINS, 0); }
		public TerminalNode ENDS() { return getToken(ExpectedStepParser.ENDS, 0); }
		public TerminalNode GREATER_THAN_OR_EQUALS() { return getToken(ExpectedStepParser.GREATER_THAN_OR_EQUALS, 0); }
		public TerminalNode GREATER_THAN() { return getToken(ExpectedStepParser.GREATER_THAN, 0); }
		public TerminalNode LESS_THAN_OR_EQUALS() { return getToken(ExpectedStepParser.LESS_THAN_OR_EQUALS, 0); }
		public TerminalNode LESS_THAN() { return getToken(ExpectedStepParser.LESS_THAN, 0); }
		public TerminalNode IS_NULL() { return getToken(ExpectedStepParser.IS_NULL, 0); }
		public TerminalNode NOT() { return getToken(ExpectedStepParser.NOT, 0); }
		public AttributeValueContext attributeValue() {
			return getRuleContext(AttributeValueContext.class,0);
		}
		public CheckExprContext(CheckExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).enterCheckExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).exitCheckExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpectedStepVisitor ) return ((ExpectedStepVisitor<? extends T>)visitor).visitCheckExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CheckExpressionContext checkExpression() throws RecognitionException {
		CheckExpressionContext _localctx = new CheckExpressionContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_checkExpression);
		int _la;
		try {
			_localctx = new CheckExprContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(33);
			outputAttributeName();
			setState(35);
			_la = _input.LA(1);
			if (_la==NOT) {
				{
				setState(34);
				match(NOT);
				}
			}

			setState(37);
			((CheckExprContext)_localctx).op = _input.LT(1);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << EQ) | (1L << REGEX) | (1L << CONTAINS) | (1L << BEGINS) | (1L << ENDS) | (1L << GREATER_THAN_OR_EQUALS) | (1L << GREATER_THAN) | (1L << LESS_THAN_OR_EQUALS) | (1L << LESS_THAN) | (1L << IS_NULL))) != 0)) ) {
				((CheckExprContext)_localctx).op = (Token)_errHandler.recoverInline(this);
			} else {
				consume();
			}
			setState(39);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				{
				setState(38);
				attributeValue();
				}
				break;
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
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).enterSetExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).exitSetExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpectedStepVisitor ) return ((ExpectedStepVisitor<? extends T>)visitor).visitSetExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SetExpressionContext setExpression() throws RecognitionException {
		SetExpressionContext _localctx = new SetExpressionContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_setExpression);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(41);
			match(T__0);
			setState(43); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(42);
					assignment();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(45); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
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
		public TerminalNode EQ() { return getToken(ExpectedStepParser.EQ, 0); }
		public SetValueContext setValue() {
			return getRuleContext(SetValueContext.class,0);
		}
		public AssignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).enterAssignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).exitAssignment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpectedStepVisitor ) return ((ExpectedStepVisitor<? extends T>)visitor).visitAssignment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignmentContext assignment() throws RecognitionException {
		AssignmentContext _localctx = new AssignmentContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_assignment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(47);
			attributeName();
			setState(48);
			match(EQ);
			setState(49);
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

	public static class ExportExpressionContext extends ParserRuleContext {
		public List<ControlParameterContext> controlParameter() {
			return getRuleContexts(ControlParameterContext.class);
		}
		public ControlParameterContext controlParameter(int i) {
			return getRuleContext(ControlParameterContext.class,i);
		}
		public ExportExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exportExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).enterExportExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).exitExportExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpectedStepVisitor ) return ((ExpectedStepVisitor<? extends T>)visitor).visitExportExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExportExpressionContext exportExpression() throws RecognitionException {
		ExportExpressionContext _localctx = new ExportExpressionContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_exportExpression);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(51);
			match(T__1);
			setState(53); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(52);
					controlParameter();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(55); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
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

	public static class ControlParameterContext extends ParserRuleContext {
		public AttributeNameContext attributeName() {
			return getRuleContext(AttributeNameContext.class,0);
		}
		public TerminalNode EQ() { return getToken(ExpectedStepParser.EQ, 0); }
		public SetValueContext setValue() {
			return getRuleContext(SetValueContext.class,0);
		}
		public ControlParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_controlParameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).enterControlParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).exitControlParameter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpectedStepVisitor ) return ((ExpectedStepVisitor<? extends T>)visitor).visitControlParameter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ControlParameterContext controlParameter() throws RecognitionException {
		ControlParameterContext _localctx = new ControlParameterContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_controlParameter);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(57);
			attributeName();
			setState(58);
			match(EQ);
			setState(59);
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

	public static class OutputAttributeNameContext extends ParserRuleContext {
		public TerminalNode WORD() { return getToken(ExpectedStepParser.WORD, 0); }
		public TerminalNode STRING() { return getToken(ExpectedStepParser.STRING, 0); }
		public OutputAttributeNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_outputAttributeName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).enterOutputAttributeName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).exitOutputAttributeName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpectedStepVisitor ) return ((ExpectedStepVisitor<? extends T>)visitor).visitOutputAttributeName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OutputAttributeNameContext outputAttributeName() throws RecognitionException {
		OutputAttributeNameContext _localctx = new OutputAttributeNameContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_outputAttributeName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(61);
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
		public TerminalNode WORD() { return getToken(ExpectedStepParser.WORD, 0); }
		public AttributeNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attributeName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).enterAttributeName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).exitAttributeName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpectedStepVisitor ) return ((ExpectedStepVisitor<? extends T>)visitor).visitAttributeName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AttributeNameContext attributeName() throws RecognitionException {
		AttributeNameContext _localctx = new AttributeNameContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_attributeName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(63);
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

	public static class SetValueContext extends ParserRuleContext {
		public AttributeNameContext attributeName() {
			return getRuleContext(AttributeNameContext.class,0);
		}
		public TerminalNode STRING() { return getToken(ExpectedStepParser.STRING, 0); }
		public SetValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_setValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).enterSetValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).exitSetValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpectedStepVisitor ) return ((ExpectedStepVisitor<? extends T>)visitor).visitSetValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SetValueContext setValue() throws RecognitionException {
		SetValueContext _localctx = new SetValueContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_setValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(67);
			switch (_input.LA(1)) {
			case WORD:
				{
				setState(65);
				attributeName();
				}
				break;
			case STRING:
				{
				setState(66);
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

	public static class AttributeValueContext extends ParserRuleContext {
		public TerminalNode NUM() { return getToken(ExpectedStepParser.NUM, 0); }
		public TerminalNode STRING() { return getToken(ExpectedStepParser.STRING, 0); }
		public TerminalNode BOOL() { return getToken(ExpectedStepParser.BOOL, 0); }
		public AttributeValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attributeValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).enterAttributeValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ExpectedStepListener ) ((ExpectedStepListener)listener).exitAttributeValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpectedStepVisitor ) return ((ExpectedStepVisitor<? extends T>)visitor).visitAttributeValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AttributeValueContext attributeValue() throws RecognitionException {
		AttributeValueContext _localctx = new AttributeValueContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_attributeValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(69);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << NUM) | (1L << BOOL) | (1L << STRING))) != 0)) ) {
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

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\24J\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t\13\4"+
		"\f\t\f\3\2\3\2\3\2\3\3\3\3\3\3\7\3\37\n\3\f\3\16\3\"\13\3\3\4\3\4\5\4"+
		"&\n\4\3\4\3\4\5\4*\n\4\3\5\3\5\6\5.\n\5\r\5\16\5/\3\6\3\6\3\6\3\6\3\7"+
		"\3\7\6\78\n\7\r\7\16\79\3\b\3\b\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\5\13"+
		"F\n\13\3\f\3\f\3\f\2\2\r\2\4\6\b\n\f\16\20\22\24\26\2\5\3\2\6\17\3\2\22"+
		"\23\4\2\20\21\23\23F\2\30\3\2\2\2\4 \3\2\2\2\6#\3\2\2\2\b+\3\2\2\2\n\61"+
		"\3\2\2\2\f\65\3\2\2\2\16;\3\2\2\2\20?\3\2\2\2\22A\3\2\2\2\24E\3\2\2\2"+
		"\26G\3\2\2\2\30\31\5\4\3\2\31\32\7\2\2\3\32\3\3\2\2\2\33\37\5\6\4\2\34"+
		"\37\5\b\5\2\35\37\5\f\7\2\36\33\3\2\2\2\36\34\3\2\2\2\36\35\3\2\2\2\37"+
		"\"\3\2\2\2 \36\3\2\2\2 !\3\2\2\2!\5\3\2\2\2\" \3\2\2\2#%\5\20\t\2$&\7"+
		"\5\2\2%$\3\2\2\2%&\3\2\2\2&\'\3\2\2\2\')\t\2\2\2(*\5\26\f\2)(\3\2\2\2"+
		")*\3\2\2\2*\7\3\2\2\2+-\7\3\2\2,.\5\n\6\2-,\3\2\2\2./\3\2\2\2/-\3\2\2"+
		"\2/\60\3\2\2\2\60\t\3\2\2\2\61\62\5\22\n\2\62\63\7\6\2\2\63\64\5\24\13"+
		"\2\64\13\3\2\2\2\65\67\7\4\2\2\668\5\16\b\2\67\66\3\2\2\289\3\2\2\29\67"+
		"\3\2\2\29:\3\2\2\2:\r\3\2\2\2;<\5\22\n\2<=\7\6\2\2=>\5\24\13\2>\17\3\2"+
		"\2\2?@\t\3\2\2@\21\3\2\2\2AB\7\22\2\2B\23\3\2\2\2CF\5\22\n\2DF\7\23\2"+
		"\2EC\3\2\2\2ED\3\2\2\2F\25\3\2\2\2GH\t\4\2\2H\27\3\2\2\2\t\36 %)/9E";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}