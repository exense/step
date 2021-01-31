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
 : outputAttributeName NOT? op=(EQ|REGEX|CONTAINS|BEGINS|ENDS) attributeValue		#checkExpr
 ;    

setExpression : 'Set' assignment+;

assignment : attributeName EQ setValue;

exportExpression : 'Export' controlParameter+;

controlParameter : attributeName EQ setValue;

outputAttributeName : (WORD|STRING);
attributeName : WORD;
setValue : (attributeName|STRING);
attributeValue : STRING;  

NOT : ('not'|'!');
   
EQ : '=';
REGEX : '~';
CONTAINS : 'contains';
BEGINS : 'beginsWith';
ENDS : 'endsWith';

WORD: (~[="\t\r\n \u00A0])+ ;

STRING
 : '"' (~["\r\n] | '""')* '"'
 ;

SPACE
 : [ \u00A0\t\r\n] -> skip
 ;