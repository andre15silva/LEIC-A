#ifndef __OG_AST_IDENTITY_NODE_H__
#define __OG_AST_IDENTITY_NODE_H__

#include <cdk/ast/unary_operation_node.h>

namespace og {

/**
 * Class for describing the indentity operator
 */
class identity_node : public cdk::unary_operation_node {

public:
    identity_node(int lineno, expression_node *argument)
        : unary_operation_node(lineno, argument) {}

    void accept(basic_ast_visitor *av, int level) {
        av->do_identity_node(this, level);
    }
};

} // namespace og

#endif
