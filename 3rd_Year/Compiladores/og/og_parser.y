%{
//-- don't change *any* of these: if you do, you'll break the compiler.
#include <cdk/compiler.h>
#include "ast/all.h"
#define LINE               compiler->scanner()->lineno()
#define yylex()            compiler->scanner()->scan()
#define yyerror(s)         compiler->scanner()->error(s)
#define YYPARSE_PARAM_TYPE std::shared_ptr<cdk::compiler>
#define YYPARSE_PARAM      compiler
//-- don't change *any* of these --- END!
%}

%union {
  unsigned long         i;	/* integer value */
  double                d;      /* double value */
  std::string           *s;	/* symbol name or string literal */
  cdk::basic_node       *node;	/* node pointer */
  cdk::sequence_node    *sequence;
  cdk::expression_node  *expression; /* expression nodes */
  cdk::lvalue_node      *lvalue;
  cdk::basic_type       *type;
  og::block_node        *block;
  std::vector<std::string> *ss;
};

%token <i> tINTEGER
%token <d> tREAL
%token <s> tIDENTIFIER tSTRING
%token <expression> tNULLPTR

%token tINT_TYPE tREAL_TYPE tSTRING_TYPE tAUTO_TYPE tPTR_TYPE tPUBLIC tPRIVATE tREQUIRE tSIZEOF tINPUT tNULLPTR tPROCEDURE tBREAK tCONTINUE tRETURN tIF tTHEN tELIF tELSE tFOR tDO tWRITE tWRITELN tGE tLE tNE tEQ tAND tOR tUNARY tTUPLE

%type<node> declaration var vardecl vardef fundecl fundef instr ifinstr loopinstr
%type<sequence> file declarations exprs args innerdecls instrs vars
%type<expression> expr integer real funcall tuple
%type<lvalue> lval
%type<type> datatype ptrtype autoptrtype prmtype
%type<block> blck
%type<s> strlit
%type<ss> ids

%right '='
%nonassoc '@'
%left tOR
%left tAND
%nonassoc '~'
%left tEQ tNE
%left '<' '>' tGE tLE
%left '+' '-'
%left '*' '/' '%'
%nonassoc tUNARY '?'
%nonassoc '['

%nonassoc tTUPLE
%nonassoc ','

%nonassoc tIF tFOR
%nonassoc tTHEN tDO
%nonassoc tELIF tELSE

%{
//-- The rules below will be included in yyparse, the main parsing function.
%}
%%
file           : declarations { compiler->ast($$ = $1); }
               ;

declarations   :              declaration { $$ = new cdk::sequence_node(LINE, $1);     }
               | declarations declaration { $$ = new cdk::sequence_node(LINE, $2, $1); }
               ;

declaration    : var ';'     { $$ = $1; }
               | fundecl     { $$ = $1; }
               | fundef      { $$ = $1; }
               ;

var            : vardecl { $$ = $1; }
               | vardef  { $$ = $1; }
               ;

vars           :          var  { $$ = new cdk::sequence_node(LINE, $1); }
               | vars ',' var  { $$ = new cdk::sequence_node(LINE, $3, $1); }
               ;

vardecl        :          datatype tIDENTIFIER { std::vector<std::string> *identifiers = new std::vector<std::string>(); identifiers->push_back(*$2); delete $2;
                                                 $$ = new og::variable_declaration_node(LINE, tPRIVATE, std::shared_ptr<cdk::basic_type>($1), identifiers, nullptr); }
               | tPUBLIC  datatype tIDENTIFIER { std::vector<std::string> *identifiers = new std::vector<std::string>(); identifiers->push_back(*$3); delete $3;
                                                 $$ = new og::variable_declaration_node(LINE, tPUBLIC, std::shared_ptr<cdk::basic_type>($2), identifiers, nullptr); }
               | tREQUIRE datatype tIDENTIFIER { std::vector<std::string> *identifiers = new std::vector<std::string>(); identifiers->push_back(*$3); delete $3;
                                                 $$ = new og::variable_declaration_node(LINE, tREQUIRE, std::shared_ptr<cdk::basic_type>($2), identifiers, nullptr); }
               ;

vardef         :          datatype   tIDENTIFIER '=' expr { std::vector<std::string> *identifiers = new std::vector<std::string>(); identifiers->push_back(*$2); delete $2;
                                                            $$ = new og::variable_declaration_node(LINE, tPRIVATE, std::shared_ptr<cdk::basic_type>($1), identifiers, $4); }
               | tPUBLIC  datatype   tIDENTIFIER '=' expr { std::vector<std::string> *identifiers = new std::vector<std::string>(); identifiers->push_back(*$3); delete $3;
                                                            $$ = new og::variable_declaration_node(LINE, tPUBLIC, std::shared_ptr<cdk::basic_type>($2), identifiers, $5); }
               | tREQUIRE datatype   tIDENTIFIER '=' expr { std::vector<std::string> *identifiers = new std::vector<std::string>(); identifiers->push_back(*$3); delete $3;
                                                            $$ = new og::variable_declaration_node(LINE, tPUBLIC, std::shared_ptr<cdk::basic_type>($2), identifiers, $5); }
               |          tAUTO_TYPE ids         '=' tuple { $$ = new og::variable_declaration_node(LINE, tPRIVATE, std::shared_ptr<cdk::basic_type>(new cdk::primitive_type()), $2, $4); }
               | tPUBLIC  tAUTO_TYPE ids         '=' tuple { $$ = new og::variable_declaration_node(LINE, tPUBLIC, std::shared_ptr<cdk::basic_type>(new cdk::primitive_type()), $3, $5); }
               ;
               
datatype       : prmtype     { $$ = $1; } 
               | ptrtype     { $$ = $1; }
               | autoptrtype { $$ = $1; }
               ;

prmtype        : tINT_TYPE    { $$ = new cdk::primitive_type(4, cdk::TYPE_INT); }
               | tREAL_TYPE   { $$ = new cdk::primitive_type(8, cdk::TYPE_DOUBLE); }
               | tSTRING_TYPE { $$ = new cdk::primitive_type(4, cdk::TYPE_STRING); }
               ;

ptrtype        : tPTR_TYPE '<' prmtype '>' { $$ = new cdk::reference_type(4, std::shared_ptr<cdk::basic_type>($3)); }
               | tPTR_TYPE '<' ptrtype '>' { $$ = new cdk::reference_type(4, std::shared_ptr<cdk::basic_type>($3)); }
               ;

autoptrtype    : tPTR_TYPE '<' tAUTO_TYPE '>'  { $$ = new cdk::reference_type(4, std::shared_ptr<cdk::basic_type>(new cdk::primitive_type())); }
               | tPTR_TYPE '<' autoptrtype '>' { $$ = $3; }
               ;

ids            :         tIDENTIFIER { $$ = new std::vector<std::string>(); $$->push_back(*$1); delete $1; }
               | ids ',' tIDENTIFIER { $$ = $1; $$->push_back(*$3); delete $3; }
               ;

fundecl        :            datatype   tIDENTIFIER '('      ')' { $$ = new og::function_declaration_node(LINE, tPRIVATE, std::shared_ptr<cdk::basic_type>($1), *$2, nullptr); delete $2; }
               |            tAUTO_TYPE tIDENTIFIER '('      ')' { $$ = new og::function_declaration_node(LINE, tPRIVATE, std::shared_ptr<cdk::basic_type>(new cdk::primitive_type()), *$2, nullptr); delete $2; }
               |            tPROCEDURE tIDENTIFIER '('      ')' { $$ = new og::function_declaration_node(LINE, tPRIVATE, *$2, nullptr); delete $2; }
               |            datatype   tIDENTIFIER '(' args ')' { $$ = new og::function_declaration_node(LINE, tPRIVATE, std::shared_ptr<cdk::basic_type>($1), *$2, $4); delete $2; }
               |            tAUTO_TYPE tIDENTIFIER '(' args ')' { $$ = new og::function_declaration_node(LINE, tPRIVATE, std::shared_ptr<cdk::basic_type>(new cdk::primitive_type()), *$2, $4); delete $2; }
               |            tPROCEDURE tIDENTIFIER '(' args ')' { $$ = new og::function_declaration_node(LINE, tPRIVATE, *$2, $4); delete $2; }
               | tPUBLIC    datatype   tIDENTIFIER '('      ')' { $$ = new og::function_declaration_node(LINE, tPUBLIC, std::shared_ptr<cdk::basic_type>($2), *$3, nullptr); delete $2; }
               | tPUBLIC    tAUTO_TYPE tIDENTIFIER '('      ')' { $$ = new og::function_declaration_node(LINE, tPUBLIC, std::shared_ptr<cdk::basic_type>(new cdk::primitive_type()), *$3, nullptr); delete $3; }
               | tPUBLIC    tPROCEDURE tIDENTIFIER '('      ')' { $$ = new og::function_declaration_node(LINE, tPUBLIC, *$3, nullptr); delete $3; }
               | tPUBLIC    datatype   tIDENTIFIER '(' args ')' { $$ = new og::function_declaration_node(LINE, tPUBLIC, std::shared_ptr<cdk::basic_type>($2), *$3, $5); delete $3; }
               | tPUBLIC    tAUTO_TYPE tIDENTIFIER '(' args ')' { $$ = new og::function_declaration_node(LINE, tPUBLIC, std::shared_ptr<cdk::basic_type>(new cdk::primitive_type()), *$3, $5);  delete $3; }
               | tPUBLIC    tPROCEDURE tIDENTIFIER '(' args ')' { $$ = new og::function_declaration_node(LINE, tPUBLIC, *$3, $5); delete $3; }
               | tREQUIRE   datatype   tIDENTIFIER '('      ')' { $$ = new og::function_declaration_node(LINE, tREQUIRE, std::shared_ptr<cdk::basic_type>($2), *$3, nullptr); delete $3; }
               | tREQUIRE   tPROCEDURE tIDENTIFIER '('      ')' { $$ = new og::function_declaration_node(LINE, tREQUIRE, *$3, nullptr); delete $3; }
               | tREQUIRE   datatype   tIDENTIFIER '(' args ')' { $$ = new og::function_declaration_node(LINE, tREQUIRE, std::shared_ptr<cdk::basic_type>($2), *$3, $5); delete $3; }
               | tREQUIRE   tPROCEDURE tIDENTIFIER '(' args ')' { $$ = new og::function_declaration_node(LINE, tREQUIRE, *$3, $5); delete $3; }
               ;

fundef         :            datatype   tIDENTIFIER '('      ')' blck { $$ = new og::function_definition_node(LINE, tPRIVATE, std::shared_ptr<cdk::basic_type>($1), *$2, nullptr, $5); delete $2; }
               |            tAUTO_TYPE tIDENTIFIER '('      ')' blck { $$ = new og::function_definition_node(LINE, tPRIVATE, std::shared_ptr<cdk::basic_type>(new cdk::primitive_type()), *$2, nullptr, $5); delete $2; }
               |            tPROCEDURE tIDENTIFIER '('      ')' blck { $$ = new og::function_definition_node(LINE, tPRIVATE, *$2, nullptr, $5); delete $2;}
               |            datatype   tIDENTIFIER '(' args ')' blck { $$ = new og::function_definition_node(LINE, tPRIVATE, std::shared_ptr<cdk::basic_type>($1), *$2, $4, $6); delete $2; }
               |            tAUTO_TYPE tIDENTIFIER '(' args ')' blck { $$ = new og::function_definition_node(LINE, tPRIVATE, std::shared_ptr<cdk::basic_type>(new cdk::primitive_type()), *$2, $4, $6); delete $2; }
               |            tPROCEDURE tIDENTIFIER '(' args ')' blck { $$ = new og::function_definition_node(LINE, tPRIVATE, *$2, $4, $6); delete $2; }
               | tPUBLIC    datatype   tIDENTIFIER '('      ')' blck { $$ = new og::function_definition_node(LINE, tPUBLIC, std::shared_ptr<cdk::basic_type>($2), *$3, nullptr, $6); delete $3; }
               | tPUBLIC    tAUTO_TYPE tIDENTIFIER '('      ')' blck { $$ = new og::function_definition_node(LINE, tPUBLIC, std::shared_ptr<cdk::basic_type>(new cdk::primitive_type()), *$3, nullptr, $6); delete $3; }
               | tPUBLIC    tPROCEDURE tIDENTIFIER '('      ')' blck { $$ = new og::function_definition_node(LINE, tPUBLIC, *$3, nullptr, $6); delete $3; }
               | tPUBLIC    datatype   tIDENTIFIER '(' args ')' blck { $$ = new og::function_definition_node(LINE, tPUBLIC, std::shared_ptr<cdk::basic_type>($2), *$3, $5, $7); delete $3; }
               | tPUBLIC    tAUTO_TYPE tIDENTIFIER '(' args ')' blck { $$ = new og::function_definition_node(LINE, tPUBLIC, std::shared_ptr<cdk::basic_type>(new cdk::primitive_type()), *$3, $5, $7); delete $3; }
               | tPUBLIC    tPROCEDURE tIDENTIFIER '(' args ')' blck { $$ = new og::function_definition_node(LINE, tPUBLIC, *$3, $5, $7); delete $3; }
               ;

args           :          vardecl { $$ = new cdk::sequence_node(LINE, $1); }
               | args ',' vardecl { $$ = new cdk::sequence_node(LINE, $3, $1); }
               ;

blck           : '{' innerdecls        '}' { $$ = new og::block_node(LINE, $2, nullptr); }
               | '{'            instrs '}' { $$ = new og::block_node(LINE, nullptr, $2); }
               | '{' innerdecls instrs '}' { $$ = new og::block_node(LINE, $2, $3); }
               ;

innerdecls     :            var ';' { $$ = new cdk::sequence_node(LINE, $1); }
               | innerdecls var ';' { $$ = new cdk::sequence_node(LINE, $2, $1); }
               ;

instrs         :        instr { $$ = new cdk::sequence_node(LINE, $1); }
               | instrs instr { $$ = new cdk::sequence_node(LINE, $2, $1); }
               ;

instr          : expr ';'           { $$ = new og::evaluation_node(LINE, $1); }
               | tWRITE exprs   ';' { $$ = new og::print_node(LINE, $2, false); }
               | tWRITELN exprs ';' { $$ = new og::print_node(LINE, $2, true); }
               | tBREAK             { $$ = new og::break_node(LINE); }
               | tCONTINUE          { $$ = new og::continue_node(LINE); }
               | tRETURN       ';'  { $$ = new og::return_node(LINE, nullptr); }
               | tRETURN tuple ';'  { $$ = new og::return_node(LINE, $2); }
               | tIF ifinstr        { $$ = $2; }
               | loopinstr          { $$ = $1; }
               | blck               { $$ = $1; }
               ; 

ifinstr        : expr tTHEN instr               { $$ = new og::if_node(LINE, $1, $3); }
               | expr tTHEN instr tELSE instr   { $$ = new og::if_else_node(LINE, $1, $3, $5); }
               | expr tTHEN instr tELIF ifinstr { $$ = new og::if_else_node(LINE, $1, $3, $5); }
               ;

loopinstr      : tFOR       ';'       ';'       tDO instr { $$ = new og::for_node(LINE, nullptr, nullptr, nullptr, $5); }
               | tFOR vars  ';'       ';'       tDO instr { $$ = new og::for_node(LINE, $2, nullptr, nullptr, $6); }
               | tFOR exprs ';'       ';'       tDO instr { $$ = new og::for_node(LINE, $2, nullptr, nullptr, $6); }
               | tFOR       ';' exprs ';'       tDO instr { $$ = new og::for_node(LINE, nullptr, $3, nullptr, $6); }
               | tFOR vars  ';' exprs ';'       tDO instr { $$ = new og::for_node(LINE, $2, $4, nullptr, $7); }
               | tFOR exprs ';' exprs ';'       tDO instr { $$ = new og::for_node(LINE, $2, $4, nullptr, $7); }
               | tFOR       ';'       ';' exprs tDO instr { $$ = new og::for_node(LINE, nullptr, nullptr, $4, $6); }
               | tFOR vars  ';'       ';' exprs tDO instr { $$ = new og::for_node(LINE, $2, nullptr, $5, $7); }
               | tFOR exprs ';'       ';' exprs tDO instr { $$ = new og::for_node(LINE, $2, nullptr, $5, $7); }
               | tFOR       ';' exprs ';' exprs tDO instr { $$ = new og::for_node(LINE, nullptr, $3, $5, $7); }
               | tFOR vars  ';' exprs ';' exprs tDO instr { $$ = new og::for_node(LINE, $2, $4, $6, $8); }
               | tFOR exprs ';' exprs ';' exprs tDO instr { $$ = new og::for_node(LINE, $2, $4, $6, $8); }
               ;

expr           : integer                 { $$ = $1; }
               | real                    { $$ = $1; }
               | strlit                  { $$ = new cdk::string_node(LINE, $1); delete $1; }
               | tNULLPTR                { $$ = new og::nullptr_node(LINE); }
               /* LEFT VALUES */
               | lval                    { $$ = new cdk::rvalue_node(LINE, $1); }
               /* ARITHMETIC EXPRESSIONS */
               | expr '+' expr           { $$ = new cdk::add_node(LINE, $1, $3); }
               | expr '-' expr           { $$ = new cdk::sub_node(LINE, $1, $3); }
               | expr '*' expr           { $$ = new cdk::mul_node(LINE, $1, $3); }
               | expr '/' expr           { $$ = new cdk::div_node(LINE, $1, $3); }
               | expr '%' expr           { $$ = new cdk::mod_node(LINE, $1, $3); }
               /* LOGICAL EXPRESSIONS */
               | expr '<' expr	         { $$ = new cdk::lt_node(LINE, $1, $3); }
               | expr '>' expr	         { $$ = new cdk::gt_node(LINE, $1, $3); }
               | expr tGE expr	         { $$ = new cdk::ge_node(LINE, $1, $3); }
               | expr tLE expr           { $$ = new cdk::le_node(LINE, $1, $3); }
               | expr tNE expr	         { $$ = new cdk::ne_node(LINE, $1, $3); }
               | expr tEQ expr	         { $$ = new cdk::eq_node(LINE, $1, $3); }
               | expr tAND expr          { $$ = new cdk::and_node(LINE, $1, $3); }
               | expr tOR expr           { $$ = new cdk::or_node(LINE, $1, $3); }
               /* UNARY EXPRESSION */
               | '-' expr %prec tUNARY   { $$ = new cdk::neg_node(LINE, $2); }
               | '+' expr %prec tUNARY   { $$ = new og::identity_node(LINE, $2); }
               | '~' expr                { $$ = new cdk::not_node(LINE, $2); }
               /* OTHER EXPRESSION */
               | lval '=' expr           { $$ = new cdk::assignment_node(LINE, $1, $3); }
               | funcall                 { $$ = $1; }
               | tINPUT                  { $$ = new og::read_node(LINE); }
               | '[' expr ']'            { $$ = new og::stack_alloc_node(LINE, $2); }
               | lval '?'                { $$ = new og::address_of_node(LINE, $1); }
               | tSIZEOF '(' tuple ')'   { $$ = new og::size_of_node(LINE, $3); }
               | '(' expr ')'            { $$ = $2; }
               ;

exprs          :           expr { $$ = new cdk::sequence_node(LINE, $1); }
               | exprs ',' expr { $$ = new cdk::sequence_node(LINE, $3, $1); }
               ;

tuple          : exprs %prec tTUPLE { $$ = new og::tuple_node(LINE, $1); }

integer        : tINTEGER       { $$ = new cdk::integer_node(LINE, $1); }
real           : tREAL          { $$ = new cdk::double_node(LINE, $1); }
strlit         :        tSTRING { $$ = $1; }
               | strlit tSTRING { $$ = new std::string(*$1 + *$2); delete $1; delete $2; }
               ;

lval           : tIDENTIFIER       { $$ = new cdk::variable_node(LINE, $1); delete $1; }
               | expr '[' expr ']' { $$ = new og::pointer_index_node(LINE, $1, $3); }
               | expr '@' tINTEGER { $$ = new og::tuple_index_node(LINE, $1, new cdk::integer_node(LINE, $3)); }
               ;

funcall        : tIDENTIFIER '('       ')' { $$ = new og::function_call_node(LINE, *$1); delete $1; }
               | tIDENTIFIER '(' exprs ')' { $$ = new og::function_call_node(LINE, *$1, $3); delete $1; }
               ;

%%
