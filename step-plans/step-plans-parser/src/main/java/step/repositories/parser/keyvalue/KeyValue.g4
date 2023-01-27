grammar KeyValue;

@header {
    package step.repositories.parser.keyvalue;
}

parse
    : keyValueList EOF
    ;
    
keyValueList
 : keyValue* 
 ;

keyValue
 : key EQ value 
 ;

key : WORD|STRING;

value : WORD|STRING|DYNAMIC_EXPRESSION;
  
EQ : '=';

WORD: (~[|=" \u00A0\t\r\n] | '\'' ~[']* '\'' | STRING )+ ;

STRING
 : '"' (~["\r\n] | '""')* '"'
 ;
 
DYNAMIC_EXPRESSION
 : '|' (~[|] | '||')* '|'
 ; 

SPACE
 : [ \u00A0\t\r\n] -> skip
 ;