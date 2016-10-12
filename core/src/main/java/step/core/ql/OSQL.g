grammar OSQL;

options { 
	output = AST; 
} 

@lexer::header {
    package io.djigger.ql;
}

@header {
    package io.djigger.ql;
}

INT: ('0'..'9')+;

// Words, which include our operators
WORD: ('a'..'z' | 'A'..'Z' | '0'..'9' |'.'|'$')+ ;

ESCAPED_QUOTE: '\\"';
QUOTED_STRING: '"' ( ESCAPED_QUOTE | ~('\n'|'\r'|'"') )* '"' {setText(getText().substring(1,getText().length()-1));};

// Grouping
LEFT_PAREN: '(';
RIGHT_PAREN: ')';

OPERATOR: ('='|'~'|'>'|'<');

WHITESPACE
    : (' ' | '\t' | '\r' | '\n') { $channel=HIDDEN; }
    ;

compilationUnit : orexpression EOF;

orexpression
    :   andexpression ('or'^ andexpression)*
    ;

andexpression
    : notexpression ('and'^ notexpression)*
    ;

notexpression
    : ('not'^)? atom
    ;

atom
    : (WORD|QUOTED_STRING) (OPERATOR^ (INT|WORD|QUOTED_STRING))?
    | LEFT_PAREN! orexpression RIGHT_PAREN!
    ;