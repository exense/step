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
		T__0=1, T__1=2, NOT=3, EQ=4, REGEX=5, CONTAINS=6, BEGINS=7, ENDS=8, GREATER_THAN=9, 
		LESS_THAN=10, NUM=11, WORD=12, STRING=13, SPACE=14;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "NOT", "EQ", "REGEX", "CONTAINS", "BEGINS", "ENDS", "GREATER_THAN", 
		"LESS_THAN", "NUM", "DIGIT", "WORD", "STRING", "SPACE"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'Set'", "'Export'", null, "'='", "'~'", "'contains'", "'beginsWith'", 
		"'endsWith'", "'>'", "'<'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, "NOT", "EQ", "REGEX", "CONTAINS", "BEGINS", "ENDS", 
		"GREATER_THAN", "LESS_THAN", "NUM", "WORD", "STRING", "SPACE"
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\20~\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\3\2\3\2\3\2\3\2\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\5\4\61\n\4\3\5\3\5\3\6\3\6\3"+
		"\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b"+
		"\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\n\3\n\3\13\3\13\3\f\5\f"+
		"Y\n\f\3\f\7\f\\\n\f\f\f\16\f_\13\f\3\f\5\fb\n\f\3\f\6\fe\n\f\r\f\16\f"+
		"f\3\r\3\r\3\16\6\16l\n\16\r\16\16\16m\3\17\3\17\3\17\3\17\7\17t\n\17\f"+
		"\17\16\17w\13\17\3\17\3\17\3\20\3\20\3\20\3\20\2\2\21\3\3\5\4\7\5\t\6"+
		"\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\2\33\16\35\17\37\20\3\2\b\4\2-"+
		"-//\3\2\60\60\3\2\62;\b\2\13\f\17\17\"\"$$??\u00a2\u00a2\5\2\f\f\17\17"+
		"$$\6\2\13\f\17\17\"\"\u00a2\u00a2\u0084\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3"+
		"\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2"+
		"\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37"+
		"\3\2\2\2\3!\3\2\2\2\5%\3\2\2\2\7\60\3\2\2\2\t\62\3\2\2\2\13\64\3\2\2\2"+
		"\r\66\3\2\2\2\17?\3\2\2\2\21J\3\2\2\2\23S\3\2\2\2\25U\3\2\2\2\27X\3\2"+
		"\2\2\31h\3\2\2\2\33k\3\2\2\2\35o\3\2\2\2\37z\3\2\2\2!\"\7U\2\2\"#\7g\2"+
		"\2#$\7v\2\2$\4\3\2\2\2%&\7G\2\2&\'\7z\2\2\'(\7r\2\2()\7q\2\2)*\7t\2\2"+
		"*+\7v\2\2+\6\3\2\2\2,-\7p\2\2-.\7q\2\2.\61\7v\2\2/\61\7#\2\2\60,\3\2\2"+
		"\2\60/\3\2\2\2\61\b\3\2\2\2\62\63\7?\2\2\63\n\3\2\2\2\64\65\7\u0080\2"+
		"\2\65\f\3\2\2\2\66\67\7e\2\2\678\7q\2\289\7p\2\29:\7v\2\2:;\7c\2\2;<\7"+
		"k\2\2<=\7p\2\2=>\7u\2\2>\16\3\2\2\2?@\7d\2\2@A\7g\2\2AB\7i\2\2BC\7k\2"+
		"\2CD\7p\2\2DE\7u\2\2EF\7Y\2\2FG\7k\2\2GH\7v\2\2HI\7j\2\2I\20\3\2\2\2J"+
		"K\7g\2\2KL\7p\2\2LM\7f\2\2MN\7u\2\2NO\7Y\2\2OP\7k\2\2PQ\7v\2\2QR\7j\2"+
		"\2R\22\3\2\2\2ST\7@\2\2T\24\3\2\2\2UV\7>\2\2V\26\3\2\2\2WY\t\2\2\2XW\3"+
		"\2\2\2XY\3\2\2\2Ya\3\2\2\2Z\\\5\31\r\2[Z\3\2\2\2\\_\3\2\2\2][\3\2\2\2"+
		"]^\3\2\2\2^`\3\2\2\2_]\3\2\2\2`b\t\3\2\2a]\3\2\2\2ab\3\2\2\2bd\3\2\2\2"+
		"ce\5\31\r\2dc\3\2\2\2ef\3\2\2\2fd\3\2\2\2fg\3\2\2\2g\30\3\2\2\2hi\t\4"+
		"\2\2i\32\3\2\2\2jl\n\5\2\2kj\3\2\2\2lm\3\2\2\2mk\3\2\2\2mn\3\2\2\2n\34"+
		"\3\2\2\2ou\7$\2\2pt\n\6\2\2qr\7$\2\2rt\7$\2\2sp\3\2\2\2sq\3\2\2\2tw\3"+
		"\2\2\2us\3\2\2\2uv\3\2\2\2vx\3\2\2\2wu\3\2\2\2xy\7$\2\2y\36\3\2\2\2z{"+
		"\t\7\2\2{|\3\2\2\2|}\b\20\2\2} \3\2\2\2\13\2\60X]afmsu\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}