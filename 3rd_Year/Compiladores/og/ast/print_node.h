#ifndef __OG_AST_PRINT_NODE_H__
#define __OG_AST_PRINT_NODE_H__

#include <cdk/ast/sequence_node.h>

namespace og {

/**
 * Class for describing print nodes.
 */
class print_node : public cdk::basic_node {
    cdk::sequence_node *_argument;
    bool _endl;

public:
    inline print_node(int lineno, cdk::sequence_node *argument, bool endl)
        : cdk::basic_node(lineno), _argument(argument), _endl(endl) {}

public:
    inline cdk::sequence_node *argument() { return _argument; }

    inline bool endl() { return _endl; }

    void accept(basic_ast_visitor *sp, int level) {
        sp->do_print_node(this, level);
    }
};

} // namespace og

#endif
