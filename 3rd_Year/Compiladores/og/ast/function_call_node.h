#ifndef __OG_AST_FUNCTION_CALL_NODE_H__
#define __OG_AST_FUNCTION_CALL_NODE_H__

#include "ast/block_node.h"
#include <cdk/ast/basic_node.h>
#include <cdk/ast/sequence_node.h>
#include <cdk/types/basic_type.h>
#include <string>

namespace og {

/**
 * Class for describing function calls.
 */
class function_call_node : public cdk::expression_node {
    std::string _identifier;
    cdk::sequence_node *_arguments;

public:
    inline function_call_node(int lineno, const std::string &identifier)
        : cdk::expression_node(lineno), _identifier(identifier),
          _arguments(new cdk::sequence_node(lineno)) {}

    inline function_call_node(int lineno, const std::string &identifier,
                              cdk::sequence_node *arguments)
        : cdk::expression_node(lineno), _identifier(identifier),
          _arguments(arguments) {}

public:
    inline const std::string &identifier() const { return _identifier; }

    inline cdk::sequence_node *arguments() { return _arguments; }

    void accept(basic_ast_visitor *sp, int level) {
        sp->do_function_call_node(this, level);
    }
};

} // namespace og

#endif
