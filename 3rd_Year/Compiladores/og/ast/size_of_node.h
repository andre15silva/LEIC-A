#ifndef __OG_AST_SIZE_OF_NODE_H__
#define __OG_AST_SIZE_OF_NODE_H__

#include <cdk/ast/expression_node.h>

namespace og {

/**
 * Class for describing the sizeof operator
 */
class size_of_node : public cdk::expression_node {
    cdk::expression_node *_argument;

public:
    size_of_node(int lineno, cdk::expression_node *argument)
        : expression_node(lineno), _argument(argument) {}

    cdk::expression_node *argument() {
        return _argument;
    }

    void accept(basic_ast_visitor *av, int level) {
        av->do_size_of_node(this, level);
    }
};

} // namespace og

#endif
