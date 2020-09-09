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

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class DescriptionStepLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.5.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, EQ=4, WORD=5, STRING=6, SPACE=7;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "T__2", "EQ", "WORD", "STRING", "EscapeSequence", "SPACE"
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


	public DescriptionStepLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "DescriptionStep.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\tD\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\3\2\3\2\3\2\3\2"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3"+
		"\4\3\4\3\4\3\4\3\5\3\5\3\6\6\6\60\n\6\r\6\16\6\61\3\7\3\7\3\7\7\7\67\n"+
		"\7\f\7\16\7:\13\7\3\7\3\7\3\b\3\b\3\b\3\t\3\t\3\t\3\t\2\2\n\3\3\5\4\7"+
		"\5\t\6\13\7\r\b\17\2\21\t\3\2\6\b\2\13\f\17\17\"\"$$??\u00a2\u00a2\5\2"+
		"\f\f\17\17$$\3\2$$\6\2\13\f\17\17\"\"\u00a2\u00a2E\2\3\3\2\2\2\2\5\3\2"+
		"\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\21\3\2\2\2\3\23"+
		"\3\2\2\2\5\27\3\2\2\2\7 \3\2\2\2\t,\3\2\2\2\13/\3\2\2\2\r\63\3\2\2\2\17"+
		"=\3\2\2\2\21@\3\2\2\2\23\24\7U\2\2\24\25\7g\2\2\25\26\7v\2\2\26\4\3\2"+
		"\2\2\27\30\7H\2\2\30\31\7w\2\2\31\32\7p\2\2\32\33\7e\2\2\33\34\7v\2\2"+
		"\34\35\7k\2\2\35\36\7q\2\2\36\37\7p\2\2\37\6\3\2\2\2 !\7G\2\2!\"\7p\2"+
		"\2\"#\7f\2\2#$\7H\2\2$%\7w\2\2%&\7p\2\2&\'\7e\2\2\'(\7v\2\2()\7k\2\2)"+
		"*\7q\2\2*+\7p\2\2+\b\3\2\2\2,-\7?\2\2-\n\3\2\2\2.\60\n\2\2\2/.\3\2\2\2"+
		"\60\61\3\2\2\2\61/\3\2\2\2\61\62\3\2\2\2\62\f\3\2\2\2\638\7$\2\2\64\67"+
		"\n\3\2\2\65\67\5\17\b\2\66\64\3\2\2\2\66\65\3\2\2\2\67:\3\2\2\28\66\3"+
		"\2\2\289\3\2\2\29;\3\2\2\2:8\3\2\2\2;<\7$\2\2<\16\3\2\2\2=>\7^\2\2>?\t"+
		"\4\2\2?\20\3\2\2\2@A\t\5\2\2AB\3\2\2\2BC\b\t\2\2C\22\3\2\2\2\6\2\61\66"+
		"8\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
