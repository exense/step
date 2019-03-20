// Generated from OQL.g4 by ANTLR 4.5.3

    package step.core.ql;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class OQLLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.5.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		EQ=1, NEQ=2, REGEX=3, OR=4, AND=5, NOT=6, OPAR=7, CPAR=8, NONQUOTEDSTRING=9, 
		STRING=10, SPACE=11;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"EQ", "NEQ", "REGEX", "OR", "AND", "NOT", "OPAR", "CPAR", "NONQUOTEDSTRING", 
		"STRING", "SPACE"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'='", "'!='", "'~'", "'or'", "'and'", "'not'", "'('", "')'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "EQ", "NEQ", "REGEX", "OR", "AND", "NOT", "OPAR", "CPAR", "NONQUOTEDSTRING", 
		"STRING", "SPACE"
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


	public OQLLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "OQL.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\rC\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\3\2\3\2\3\3\3\3\3\3\3\4\3\4\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3"+
		"\7\3\7\3\7\3\7\3\b\3\b\3\t\3\t\3\n\6\n\61\n\n\r\n\16\n\62\3\13\3\13\3"+
		"\13\3\13\7\139\n\13\f\13\16\13<\13\13\3\13\3\13\3\f\3\f\3\f\3\f\2\2\r"+
		"\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\3\2\5\n\2&&/\60\62"+
		";>>@@C\\aac|\5\2\f\f\17\17$$\5\2\13\f\17\17\"\"E\2\3\3\2\2\2\2\5\3\2\2"+
		"\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21"+
		"\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\3\31\3\2\2\2\5\33\3\2"+
		"\2\2\7\36\3\2\2\2\t \3\2\2\2\13#\3\2\2\2\r\'\3\2\2\2\17+\3\2\2\2\21-\3"+
		"\2\2\2\23\60\3\2\2\2\25\64\3\2\2\2\27?\3\2\2\2\31\32\7?\2\2\32\4\3\2\2"+
		"\2\33\34\7#\2\2\34\35\7?\2\2\35\6\3\2\2\2\36\37\7\u0080\2\2\37\b\3\2\2"+
		"\2 !\7q\2\2!\"\7t\2\2\"\n\3\2\2\2#$\7c\2\2$%\7p\2\2%&\7f\2\2&\f\3\2\2"+
		"\2\'(\7p\2\2()\7q\2\2)*\7v\2\2*\16\3\2\2\2+,\7*\2\2,\20\3\2\2\2-.\7+\2"+
		"\2.\22\3\2\2\2/\61\t\2\2\2\60/\3\2\2\2\61\62\3\2\2\2\62\60\3\2\2\2\62"+
		"\63\3\2\2\2\63\24\3\2\2\2\64:\7$\2\2\659\n\3\2\2\66\67\7$\2\2\679\7$\2"+
		"\28\65\3\2\2\28\66\3\2\2\29<\3\2\2\2:8\3\2\2\2:;\3\2\2\2;=\3\2\2\2<:\3"+
		"\2\2\2=>\7$\2\2>\26\3\2\2\2?@\t\4\2\2@A\3\2\2\2AB\b\f\2\2B\30\3\2\2\2"+
		"\6\2\628:\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}