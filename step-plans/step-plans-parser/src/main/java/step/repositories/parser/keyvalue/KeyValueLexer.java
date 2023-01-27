// Generated from KeyValue.g4 by ANTLR 4.5.3

    package step.repositories.parser.keyvalue;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class KeyValueLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.5.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		EQ=1, WORD=2, STRING=3, DYNAMIC_EXPRESSION=4, SPACE=5;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"EQ", "WORD", "STRING", "DYNAMIC_EXPRESSION", "SPACE"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'='"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "EQ", "WORD", "STRING", "DYNAMIC_EXPRESSION", "SPACE"
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


	public KeyValueLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "KeyValue.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\7\67\b\1\4\2\t\2"+
		"\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\3\2\3\2\3\3\3\3\3\3\7\3\23\n\3\f\3\16"+
		"\3\26\13\3\3\3\3\3\6\3\32\n\3\r\3\16\3\33\3\4\3\4\3\4\3\4\7\4\"\n\4\f"+
		"\4\16\4%\13\4\3\4\3\4\3\5\3\5\3\5\3\5\7\5-\n\5\f\5\16\5\60\13\5\3\5\3"+
		"\5\3\6\3\6\3\6\3\6\2\2\7\3\3\5\4\7\5\t\6\13\7\3\2\7\t\2\13\f\17\17\"\""+
		"$$??~~\u00a2\u00a2\3\2))\5\2\f\f\17\17$$\3\2~~\6\2\13\f\17\17\"\"\u00a2"+
		"\u00a2>\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\3"+
		"\r\3\2\2\2\5\31\3\2\2\2\7\35\3\2\2\2\t(\3\2\2\2\13\63\3\2\2\2\r\16\7?"+
		"\2\2\16\4\3\2\2\2\17\32\n\2\2\2\20\24\7)\2\2\21\23\n\3\2\2\22\21\3\2\2"+
		"\2\23\26\3\2\2\2\24\22\3\2\2\2\24\25\3\2\2\2\25\27\3\2\2\2\26\24\3\2\2"+
		"\2\27\32\7)\2\2\30\32\5\7\4\2\31\17\3\2\2\2\31\20\3\2\2\2\31\30\3\2\2"+
		"\2\32\33\3\2\2\2\33\31\3\2\2\2\33\34\3\2\2\2\34\6\3\2\2\2\35#\7$\2\2\36"+
		"\"\n\4\2\2\37 \7$\2\2 \"\7$\2\2!\36\3\2\2\2!\37\3\2\2\2\"%\3\2\2\2#!\3"+
		"\2\2\2#$\3\2\2\2$&\3\2\2\2%#\3\2\2\2&\'\7$\2\2\'\b\3\2\2\2(.\7~\2\2)-"+
		"\n\5\2\2*+\7~\2\2+-\7~\2\2,)\3\2\2\2,*\3\2\2\2-\60\3\2\2\2.,\3\2\2\2."+
		"/\3\2\2\2/\61\3\2\2\2\60.\3\2\2\2\61\62\7~\2\2\62\n\3\2\2\2\63\64\t\6"+
		"\2\2\64\65\3\2\2\2\65\66\b\6\2\2\66\f\3\2\2\2\n\2\24\31\33!#,.\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}