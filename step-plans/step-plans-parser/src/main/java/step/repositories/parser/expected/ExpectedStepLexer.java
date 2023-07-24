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
		WORD=15, STRING=16, SPACE=17;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "NOT", "EQ", "REGEX", "CONTAINS", "BEGINS", "ENDS", "GREATER_THAN_OR_EQUALS", 
		"GREATER_THAN", "LESS_THAN_OR_EQUALS", "LESS_THAN", "IS_NULL", "NUM", 
		"DIGIT", "WORD", "STRING", "SPACE"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'Set'", "'Export'", null, "'='", "'~'", "'contains'", "'beginsWith'", 
		"'endsWith'", "'>='", "'>'", "'<='", "'<'", "'isNull'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, "NOT", "EQ", "REGEX", "CONTAINS", "BEGINS", "ENDS", 
		"GREATER_THAN_OR_EQUALS", "GREATER_THAN", "LESS_THAN_OR_EQUALS", "LESS_THAN", 
		"IS_NULL", "NUM", "WORD", "STRING", "SPACE"
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\23\u0091\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3"+
		"\4\3\4\5\4\67\n\4\3\5\3\5\3\6\3\6\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7"+
		"\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\t\3"+
		"\t\3\t\3\t\3\n\3\n\3\n\3\13\3\13\3\f\3\f\3\f\3\r\3\r\3\16\3\16\3\16\3"+
		"\16\3\16\3\16\3\16\3\17\5\17l\n\17\3\17\7\17o\n\17\f\17\16\17r\13\17\3"+
		"\17\5\17u\n\17\3\17\6\17x\n\17\r\17\16\17y\3\20\3\20\3\21\6\21\177\n\21"+
		"\r\21\16\21\u0080\3\22\3\22\3\22\3\22\7\22\u0087\n\22\f\22\16\22\u008a"+
		"\13\22\3\22\3\22\3\23\3\23\3\23\3\23\2\2\24\3\3\5\4\7\5\t\6\13\7\r\b\17"+
		"\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\2!\21#\22%\23\3\2\b\4\2-"+
		"-//\3\2\60\60\3\2\62;\b\2\13\f\17\17\"\"$$??\u00a2\u00a2\5\2\f\f\17\17"+
		"$$\6\2\13\f\17\17\"\"\u00a2\u00a2\u0097\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3"+
		"\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2"+
		"\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35"+
		"\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\3\'\3\2\2\2\5+\3\2\2\2\7\66"+
		"\3\2\2\2\t8\3\2\2\2\13:\3\2\2\2\r<\3\2\2\2\17E\3\2\2\2\21P\3\2\2\2\23"+
		"Y\3\2\2\2\25\\\3\2\2\2\27^\3\2\2\2\31a\3\2\2\2\33c\3\2\2\2\35k\3\2\2\2"+
		"\37{\3\2\2\2!~\3\2\2\2#\u0082\3\2\2\2%\u008d\3\2\2\2\'(\7U\2\2()\7g\2"+
		"\2)*\7v\2\2*\4\3\2\2\2+,\7G\2\2,-\7z\2\2-.\7r\2\2./\7q\2\2/\60\7t\2\2"+
		"\60\61\7v\2\2\61\6\3\2\2\2\62\63\7p\2\2\63\64\7q\2\2\64\67\7v\2\2\65\67"+
		"\7#\2\2\66\62\3\2\2\2\66\65\3\2\2\2\67\b\3\2\2\289\7?\2\29\n\3\2\2\2:"+
		";\7\u0080\2\2;\f\3\2\2\2<=\7e\2\2=>\7q\2\2>?\7p\2\2?@\7v\2\2@A\7c\2\2"+
		"AB\7k\2\2BC\7p\2\2CD\7u\2\2D\16\3\2\2\2EF\7d\2\2FG\7g\2\2GH\7i\2\2HI\7"+
		"k\2\2IJ\7p\2\2JK\7u\2\2KL\7Y\2\2LM\7k\2\2MN\7v\2\2NO\7j\2\2O\20\3\2\2"+
		"\2PQ\7g\2\2QR\7p\2\2RS\7f\2\2ST\7u\2\2TU\7Y\2\2UV\7k\2\2VW\7v\2\2WX\7"+
		"j\2\2X\22\3\2\2\2YZ\7@\2\2Z[\7?\2\2[\24\3\2\2\2\\]\7@\2\2]\26\3\2\2\2"+
		"^_\7>\2\2_`\7?\2\2`\30\3\2\2\2ab\7>\2\2b\32\3\2\2\2cd\7k\2\2de\7u\2\2"+
		"ef\7P\2\2fg\7w\2\2gh\7n\2\2hi\7n\2\2i\34\3\2\2\2jl\t\2\2\2kj\3\2\2\2k"+
		"l\3\2\2\2lt\3\2\2\2mo\5\37\20\2nm\3\2\2\2or\3\2\2\2pn\3\2\2\2pq\3\2\2"+
		"\2qs\3\2\2\2rp\3\2\2\2su\t\3\2\2tp\3\2\2\2tu\3\2\2\2uw\3\2\2\2vx\5\37"+
		"\20\2wv\3\2\2\2xy\3\2\2\2yw\3\2\2\2yz\3\2\2\2z\36\3\2\2\2{|\t\4\2\2| "+
		"\3\2\2\2}\177\n\5\2\2~}\3\2\2\2\177\u0080\3\2\2\2\u0080~\3\2\2\2\u0080"+
		"\u0081\3\2\2\2\u0081\"\3\2\2\2\u0082\u0088\7$\2\2\u0083\u0087\n\6\2\2"+
		"\u0084\u0085\7$\2\2\u0085\u0087\7$\2\2\u0086\u0083\3\2\2\2\u0086\u0084"+
		"\3\2\2\2\u0087\u008a\3\2\2\2\u0088\u0086\3\2\2\2\u0088\u0089\3\2\2\2\u0089"+
		"\u008b\3\2\2\2\u008a\u0088\3\2\2\2\u008b\u008c\7$\2\2\u008c$\3\2\2\2\u008d"+
		"\u008e\t\7\2\2\u008e\u008f\3\2\2\2\u008f\u0090\b\23\2\2\u0090&\3\2\2\2"+
		"\13\2\66kpty\u0080\u0086\u0088\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}