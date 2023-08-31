// Generated from ExpectedStep.g4 by ANTLR 4.5.3

    package step.repositories.parser.expected;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class ExpectedStepLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.5.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, NOT=3, EQ=4, REGEX=5, CONTAINS=6, BEGINS=7, ENDS=8, GREATER_THAN_OR_EQUALS=9, 
		GREATER_THAN=10, LESS_THAN_OR_EQUALS=11, LESS_THAN=12, IS_NULL=13, NUM=14, 
		BOOL=15, WORD=16, STRING=17, SPACE=18;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "NOT", "EQ", "REGEX", "CONTAINS", "BEGINS", "ENDS", "GREATER_THAN_OR_EQUALS", 
		"GREATER_THAN", "LESS_THAN_OR_EQUALS", "LESS_THAN", "IS_NULL", "NUM", 
		"DIGIT", "BOOL", "WORD", "STRING", "SPACE"
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


	public ExpectedStepLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "ExpectedStep.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\24\u009e\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\4\3\4\3\4\3\4\5\49\n\4\3\5\3\5\3\6\3\6\3\7\3\7\3\7\3\7\3\7\3\7\3\7"+
		"\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\t\3\t\3\t\3\t\3"+
		"\t\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\13\3\13\3\f\3\f\3\f\3\r\3\r\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\17\5\17n\n\17\3\17\7\17q\n\17\f\17\16\17t"+
		"\13\17\3\17\5\17w\n\17\3\17\6\17z\n\17\r\17\16\17{\3\20\3\20\3\21\3\21"+
		"\3\21\3\21\3\21\3\21\3\21\3\21\3\21\5\21\u0089\n\21\3\22\6\22\u008c\n"+
		"\22\r\22\16\22\u008d\3\23\3\23\3\23\3\23\7\23\u0094\n\23\f\23\16\23\u0097"+
		"\13\23\3\23\3\23\3\24\3\24\3\24\3\24\2\2\25\3\3\5\4\7\5\t\6\13\7\r\b\17"+
		"\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\2!\21#\22%\23\'\24\3\2\b"+
		"\4\2--//\3\2\60\60\3\2\62;\b\2\13\f\17\17\"\"$$??\u00a2\u00a2\5\2\f\f"+
		"\17\17$$\6\2\13\f\17\17\"\"\u00a2\u00a2\u00a5\2\3\3\2\2\2\2\5\3\2\2\2"+
		"\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3"+
		"\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2"+
		"\2\2\35\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\3)\3\2\2"+
		"\2\5-\3\2\2\2\78\3\2\2\2\t:\3\2\2\2\13<\3\2\2\2\r>\3\2\2\2\17G\3\2\2\2"+
		"\21R\3\2\2\2\23[\3\2\2\2\25^\3\2\2\2\27`\3\2\2\2\31c\3\2\2\2\33e\3\2\2"+
		"\2\35m\3\2\2\2\37}\3\2\2\2!\u0088\3\2\2\2#\u008b\3\2\2\2%\u008f\3\2\2"+
		"\2\'\u009a\3\2\2\2)*\7U\2\2*+\7g\2\2+,\7v\2\2,\4\3\2\2\2-.\7G\2\2./\7"+
		"z\2\2/\60\7r\2\2\60\61\7q\2\2\61\62\7t\2\2\62\63\7v\2\2\63\6\3\2\2\2\64"+
		"\65\7p\2\2\65\66\7q\2\2\669\7v\2\2\679\7#\2\28\64\3\2\2\28\67\3\2\2\2"+
		"9\b\3\2\2\2:;\7?\2\2;\n\3\2\2\2<=\7\u0080\2\2=\f\3\2\2\2>?\7e\2\2?@\7"+
		"q\2\2@A\7p\2\2AB\7v\2\2BC\7c\2\2CD\7k\2\2DE\7p\2\2EF\7u\2\2F\16\3\2\2"+
		"\2GH\7d\2\2HI\7g\2\2IJ\7i\2\2JK\7k\2\2KL\7p\2\2LM\7u\2\2MN\7Y\2\2NO\7"+
		"k\2\2OP\7v\2\2PQ\7j\2\2Q\20\3\2\2\2RS\7g\2\2ST\7p\2\2TU\7f\2\2UV\7u\2"+
		"\2VW\7Y\2\2WX\7k\2\2XY\7v\2\2YZ\7j\2\2Z\22\3\2\2\2[\\\7@\2\2\\]\7?\2\2"+
		"]\24\3\2\2\2^_\7@\2\2_\26\3\2\2\2`a\7>\2\2ab\7?\2\2b\30\3\2\2\2cd\7>\2"+
		"\2d\32\3\2\2\2ef\7k\2\2fg\7u\2\2gh\7P\2\2hi\7w\2\2ij\7n\2\2jk\7n\2\2k"+
		"\34\3\2\2\2ln\t\2\2\2ml\3\2\2\2mn\3\2\2\2nv\3\2\2\2oq\5\37\20\2po\3\2"+
		"\2\2qt\3\2\2\2rp\3\2\2\2rs\3\2\2\2su\3\2\2\2tr\3\2\2\2uw\t\3\2\2vr\3\2"+
		"\2\2vw\3\2\2\2wy\3\2\2\2xz\5\37\20\2yx\3\2\2\2z{\3\2\2\2{y\3\2\2\2{|\3"+
		"\2\2\2|\36\3\2\2\2}~\t\4\2\2~ \3\2\2\2\177\u0080\7v\2\2\u0080\u0081\7"+
		"t\2\2\u0081\u0082\7w\2\2\u0082\u0089\7g\2\2\u0083\u0084\7h\2\2\u0084\u0085"+
		"\7c\2\2\u0085\u0086\7n\2\2\u0086\u0087\7u\2\2\u0087\u0089\7g\2\2\u0088"+
		"\177\3\2\2\2\u0088\u0083\3\2\2\2\u0089\"\3\2\2\2\u008a\u008c\n\5\2\2\u008b"+
		"\u008a\3\2\2\2\u008c\u008d\3\2\2\2\u008d\u008b\3\2\2\2\u008d\u008e\3\2"+
		"\2\2\u008e$\3\2\2\2\u008f\u0095\7$\2\2\u0090\u0094\n\6\2\2\u0091\u0092"+
		"\7$\2\2\u0092\u0094\7$\2\2\u0093\u0090\3\2\2\2\u0093\u0091\3\2\2\2\u0094"+
		"\u0097\3\2\2\2\u0095\u0093\3\2\2\2\u0095\u0096\3\2\2\2\u0096\u0098\3\2"+
		"\2\2\u0097\u0095\3\2\2\2\u0098\u0099\7$\2\2\u0099&\3\2\2\2\u009a\u009b"+
		"\t\7\2\2\u009b\u009c\3\2\2\2\u009c\u009d\b\24\2\2\u009d(\3\2\2\2\f\28"+
		"mrv{\u0088\u008d\u0093\u0095\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}