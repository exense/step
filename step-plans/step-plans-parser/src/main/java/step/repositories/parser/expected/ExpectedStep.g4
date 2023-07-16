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
 : outputAttributeName NOT? op=(EQ|REGEX|CONTAINS|BEGINS|ENDS|GREATER_THAN|LESS_THAN) attributeValue		#checkExpr
 ;

setExpression : 'Set' assignment+;

assignment : attributeName EQ setValue;

exportExpression : 'Export' controlParameter+;

controlParameter : attributeName EQ setValue;

outputAttributeName : (WORD|STRING);
attributeName : WORD;
setValue : (attributeName|STRING);
attributeValue : (NUM|STRING);

NOT : ('not'|'!');
   
EQ : '=';
REGEX : '~';
CONTAINS : 'contains';
BEGINS : 'beginsWith';
ENDS : 'endsWith';
GREATER_THAN : '>';
LESS_THAN : '<';

NUM : [+\-]?(DIGIT*[.])?DIGIT+ ;
fragment DIGIT : [0-9] ;

WORD: (~[="\t\r\n \u00A0])+ ;

STRING
 : '"' (~["\r\n] | '""')* '"'
 ;

SPACE
 : [ \u00A0\t\r\n] -> skip
 ;