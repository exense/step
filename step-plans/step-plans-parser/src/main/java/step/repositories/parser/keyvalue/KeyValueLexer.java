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
		EQ=1, WORD=2, STRING=3, STRING2=4, DYNAMIC_EXPRESSION=5, SPACE=6;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"EQ", "WORD", "STRING", "STRING2", "DYNAMIC_EXPRESSION", "SPACE"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'='"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "EQ", "WORD", "STRING", "STRING2", "DYNAMIC_EXPRESSION", "SPACE"
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\b=\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\3\2\3\2\3\3\3\3\3\3\3\3\3\3\6\3"+
		"\27\n\3\r\3\16\3\30\3\4\3\4\3\4\3\4\7\4\37\n\4\f\4\16\4\"\13\4\3\4\3\4"+
		"\3\5\3\5\7\5(\n\5\f\5\16\5+\13\5\3\5\3\5\3\6\3\6\3\6\3\6\7\6\63\n\6\f"+
		"\6\16\6\66\13\6\3\6\3\6\3\7\3\7\3\7\3\7\2\2\b\3\3\5\4\7\5\t\6\13\7\r\b"+
		"\3\2\7\t\2\13\f\17\17\"\"$$??~~\u00a2\u00a2\5\2\f\f\17\17$$\5\2\f\f\17"+
		"\17))\3\2~~\6\2\13\f\17\17\"\"\u00a2\u00a2D\2\3\3\2\2\2\2\5\3\2\2\2\2"+
		"\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\3\17\3\2\2\2\5\26\3\2"+
		"\2\2\7\32\3\2\2\2\t%\3\2\2\2\13.\3\2\2\2\r9\3\2\2\2\17\20\7?\2\2\20\4"+
		"\3\2\2\2\21\27\n\2\2\2\22\23\n\2\2\2\23\27\5\t\5\2\24\25\n\2\2\2\25\27"+
		"\5\7\4\2\26\21\3\2\2\2\26\22\3\2\2\2\26\24\3\2\2\2\27\30\3\2\2\2\30\26"+
		"\3\2\2\2\30\31\3\2\2\2\31\6\3\2\2\2\32 \7$\2\2\33\37\n\3\2\2\34\35\7$"+
		"\2\2\35\37\7$\2\2\36\33\3\2\2\2\36\34\3\2\2\2\37\"\3\2\2\2 \36\3\2\2\2"+
		" !\3\2\2\2!#\3\2\2\2\" \3\2\2\2#$\7$\2\2$\b\3\2\2\2%)\7)\2\2&(\n\4\2\2"+
		"\'&\3\2\2\2(+\3\2\2\2)\'\3\2\2\2)*\3\2\2\2*,\3\2\2\2+)\3\2\2\2,-\7)\2"+
		"\2-\n\3\2\2\2.\64\7~\2\2/\63\n\5\2\2\60\61\7~\2\2\61\63\7~\2\2\62/\3\2"+
		"\2\2\62\60\3\2\2\2\63\66\3\2\2\2\64\62\3\2\2\2\64\65\3\2\2\2\65\67\3\2"+
		"\2\2\66\64\3\2\2\2\678\7~\2\28\f\3\2\2\29:\t\6\2\2:;\3\2\2\2;<\b\7\2\2"+
		"<\16\3\2\2\2\n\2\26\30\36 )\62\64\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}