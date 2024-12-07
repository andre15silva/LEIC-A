#ifndef __OG_TARGETS_POSTFIX_WRITER_H__
#define __OG_TARGETS_POSTFIX_WRITER_H__

#include "targets/basic_ast_visitor.h"

#include "targets/symbol.h"
#include <cdk/emitters/basic_postfix_emitter.h>
#include <set>
#include <sstream>
#include <stack>

namespace og {

//!
//! Traverse syntax tree and generate the corresponding assembly code.
//!
class postfix_writer : public basic_ast_visitor {
    cdk::symbol_table<og::symbol> &_symtab;

    std::set<std::string> _functions_to_declare;

    // semantic analysis
    bool _inFunction, _inFunctionArgs, _inFunctionBody;
    bool _returnSeen;
    std::stack<int> _forIni, _forStep, _forEnd;
    bool _inForIni, _inForIncr;
    std::shared_ptr<og::symbol> _function;

    int _offset = 0;
    int _returnOffset = 0;

    // code generation
    cdk::basic_postfix_emitter &_pf;
    int _lbl;

public:
    postfix_writer(std::shared_ptr<cdk::compiler> compiler, cdk::symbol_table<og::symbol> &symtab,
                   cdk::basic_postfix_emitter &pf)
        : basic_ast_visitor(compiler), _symtab(symtab), _inFunction(false), _inFunctionArgs(false),
          _inFunctionBody(false), _inForIni(false), _inForIncr(false), _function(nullptr), _pf(pf), _lbl(0) {}

public:
    ~postfix_writer() { os().flush(); }

private:
    /** Method used to generate sequential labels. */
    inline std::string mklbl(int lbl) {
        std::ostringstream oss;
        if (lbl < 0)
            oss << ".L" << -lbl;
        else
            oss << "_L" << lbl;
        return oss.str();
    }

public:
    // do not edit these lines
#define __IN_VISITOR_HEADER__
#include "ast/visitor_decls.h" // automatically generated
#undef __IN_VISITOR_HEADER__
    // do not edit these lines: end
};

} // namespace og

#endif
