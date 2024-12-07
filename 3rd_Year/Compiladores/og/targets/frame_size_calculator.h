#ifndef __OG_TARGETS_FRAME_SIZE_CALC_WRITER_H__
#define __OG_TARGETS_FRAME_SIZE_CALC_WRITER_H__

#include "targets/basic_ast_visitor.h"
#include "targets/type_checker.h"

#include <cstddef>
#include <sstream>

namespace og {

  //!
  //! Traverse syntax tree and generate the corresponding assembly code.
  //!
  class frame_size_calculator: public basic_ast_visitor {
    cdk::symbol_table<og::symbol> &_symtab;
    std::shared_ptr<og::symbol> _function;
    size_t _localsize;

  public:
    frame_size_calculator(std::shared_ptr<cdk::compiler> compiler, cdk::symbol_table<og::symbol> &symtab) :
        basic_ast_visitor(compiler), _symtab(symtab), _localsize(0) {}

  public:
      ~frame_size_calculator();
    size_t localsize() const {
        return _localsize;
    }

  public:
  // do not edit these lines
#define __IN_VISITOR_HEADER__
#include "ast/visitor_decls.h"       // automatically generated
#undef __IN_VISITOR_HEADER__
  // do not edit these lines: end

  };

} // og

#endif
