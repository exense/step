grammar DescriptionStep;

@header {
    package step.repositories.parser.description;
}

parse
    : expr EOF
    ;
    
expr
 :  (keywordExpression|setExpression|functionDeclarationExpression|functionDeclarationEndExpression)
 ;

/* Expressions */
keywordExpression : keywordName keywordParameter*;
setExpression : 'Set' assignment+;
functionDeclarationExpression : 'Function' keywordParameter*;
functionDeclarationEndExpression : 'EndFunction';

assignment : attributeName EQ setValue;

keywordParameter
 : attributeName EQ attributeValue 
 ;

keywordName : (STRING|WORD);
 
attributeName : WORD;

attributeValue : STRING;
  
setValue : (attributeName|STRING);
  
EQ : '=';

WORD: (~[=" \u00A0\t\r\n])+ ;

STRING
 : '"' (~["\r\n] | EscapeSequence)* '"'
 ;
 
fragment EscapeSequence
 : '\\' ["]
 ;
 
SPACE
 : [ \u00A0\t\r\n] -> skip
 ;