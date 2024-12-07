#ifndef __OG_AST_FOR_NODE_H__
#define __OG_AST_FOR_NODE_H__

#include <cdk/ast/basic_node.h>
#include <cdk/ast/sequence_node.h>

namespace og {
/*
   Class for describing for nodes.
 */
class for_node : public cdk::basic_node {
private:
    cdk::sequence_node *_inits;
    cdk::sequence_node *_conditions;
    cdk::sequence_node *_incrs;
    cdk::basic_node *_block;

public:
    for_node(int lineno, cdk::sequence_node *inits, cdk::sequence_node *conditions, cdk::sequence_node *incrs,
             cdk::basic_node *block)
        : cdk::basic_node(lineno), _inits(inits), _conditions(conditions), _incrs(incrs), _block(block) {}

    inline cdk::sequence_node *inits() { return _inits; }
    inline cdk::sequence_node *conditions() { return _conditions; }
    inline cdk::sequence_node *incrs() { return _incrs; }
    inline cdk::basic_node *block() { return _block; }

    void accept(basic_ast_visitor *sp, int level) { sp->do_for_node(this, level); }
};

} // namespace og

#endif
