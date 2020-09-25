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
		T__0=1, T__1=2, NOT=3, EQ=4, REGEX=5, CONTAINS=6, BEGINS=7, ENDS=8, WORD=9, 
		STRING=10, SPACE=11;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "NOT", "EQ", "REGEX", "CONTAINS", "BEGINS", "ENDS", "WORD", 
		"STRING", "SPACE"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'Set'", "'Export'", null, "'='", "'~'", "'contains'", "'beginsWith'", 
		"'endsWith'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, "NOT", "EQ", "REGEX", "CONTAINS", "BEGINS", "ENDS", 
		"WORD", "STRING", "SPACE"
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\r_\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3"+
		"\4\5\4)\n\4\3\5\3\5\3\6\3\6\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\b\3"+
		"\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t"+
		"\3\t\3\n\6\nM\n\n\r\n\16\nN\3\13\3\13\3\13\3\13\7\13U\n\13\f\13\16\13"+
		"X\13\13\3\13\3\13\3\f\3\f\3\f\3\f\2\2\r\3\3\5\4\7\5\t\6\13\7\r\b\17\t"+
		"\21\n\23\13\25\f\27\r\3\2\5\b\2\13\f\17\17\"\"$$??\u00a2\u00a2\5\2\f\f"+
		"\17\17$$\6\2\13\f\17\17\"\"\u00a2\u00a2b\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3"+
		"\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2"+
		"\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\3\31\3\2\2\2\5\35\3\2\2\2\7(\3"+
		"\2\2\2\t*\3\2\2\2\13,\3\2\2\2\r.\3\2\2\2\17\67\3\2\2\2\21B\3\2\2\2\23"+
		"L\3\2\2\2\25P\3\2\2\2\27[\3\2\2\2\31\32\7U\2\2\32\33\7g\2\2\33\34\7v\2"+
		"\2\34\4\3\2\2\2\35\36\7G\2\2\36\37\7z\2\2\37 \7r\2\2 !\7q\2\2!\"\7t\2"+
		"\2\"#\7v\2\2#\6\3\2\2\2$%\7p\2\2%&\7q\2\2&)\7v\2\2\')\7#\2\2($\3\2\2\2"+
		"(\'\3\2\2\2)\b\3\2\2\2*+\7?\2\2+\n\3\2\2\2,-\7\u0080\2\2-\f\3\2\2\2./"+
		"\7e\2\2/\60\7q\2\2\60\61\7p\2\2\61\62\7v\2\2\62\63\7c\2\2\63\64\7k\2\2"+
		"\64\65\7p\2\2\65\66\7u\2\2\66\16\3\2\2\2\678\7d\2\289\7g\2\29:\7i\2\2"+
		":;\7k\2\2;<\7p\2\2<=\7u\2\2=>\7Y\2\2>?\7k\2\2?@\7v\2\2@A\7j\2\2A\20\3"+
		"\2\2\2BC\7g\2\2CD\7p\2\2DE\7f\2\2EF\7u\2\2FG\7Y\2\2GH\7k\2\2HI\7v\2\2"+
		"IJ\7j\2\2J\22\3\2\2\2KM\n\2\2\2LK\3\2\2\2MN\3\2\2\2NL\3\2\2\2NO\3\2\2"+
		"\2O\24\3\2\2\2PV\7$\2\2QU\n\3\2\2RS\7$\2\2SU\7$\2\2TQ\3\2\2\2TR\3\2\2"+
		"\2UX\3\2\2\2VT\3\2\2\2VW\3\2\2\2WY\3\2\2\2XV\3\2\2\2YZ\7$\2\2Z\26\3\2"+
		"\2\2[\\\t\4\2\2\\]\3\2\2\2]^\b\f\2\2^\30\3\2\2\2\7\2(NTV\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
