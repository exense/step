grammar ExpectedStep;

@header {
    package step.repositories.parser.expected;
}

parse
    : expr EOF
    ;
    
expr
 : (checkExpression|setExpression|exportExpression)*
 ;
 
checkExpression
 : outputAttributeName NOT? op=(EQ|REGEX|CONTAINS|BEGINS|ENDS|GREATER_THAN_OR_EQUALS|GREATER_THAN|LESS_THAN_OR_EQUALS|LESS_THAN|IS_NULL) attributeValue?		#checkExpr
 ;

setExpression : 'Set' assignment+;

assignment : attributeName EQ setValue;

exportExpression : 'Export' controlParameter+;

controlParameter : attributeName EQ setValue;

outputAttributeName : (WORD|STRING);
attributeName : WORD;
setValue : (attributeName|STRING);
attributeValue : (NUM|STRING|BOOL);

NOT : ('not'|'!');
   
EQ : '=';
REGEX : '~';
CONTAINS : 'contains';
BEGINS : 'beginsWith';
ENDS : 'endsWith';
GREATER_THAN_OR_EQUALS : '>=';
GREATER_THAN : '>';
LESS_THAN_OR_EQUALS : '<=';
LESS_THAN : '<';
IS_NULL : 'isNull';

NUM : [+\-]?(DIGIT*[.])?DIGIT+ ;
fragment DIGIT : [0-9] ;

BOOL : 'true' | 'false' ;

WORD: (~[="\t\r\n \u00A0])+ ;

STRING
 : '"' (~["\r\n] | '""')* '"'
 ;

SPACE
 : [ \u00A0\t\r\n] -> skip
 ;