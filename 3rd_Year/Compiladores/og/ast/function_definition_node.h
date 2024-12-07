#ifndef __OG_AST_FUNCTION_DEFINITION_NODE_H__
#define __OG_AST_FUNCTION_DEFINITION_NODE_H__

#include "ast/block_node.h"
#include <cdk/ast/basic_node.h>
#include <cdk/ast/sequence_node.h>
#include <cdk/types/basic_type.h>
#include <cdk/types/primitive_type.h>
#include <cdk/types/typename_type.h>
#include <string>
#include <memory>

namespace og {

/**
 * Class for describing function definitions.
 */
class function_definition_node : public cdk::typed_node {
    int _qualifier;
    std::string _identifier;
    cdk::sequence_node *_arguments;
    og::block_node *_block;

public:
    inline function_definition_node(int lineno, int qualifier,
                                    const std::string &identifier,
                                    cdk::sequence_node *arguments,
                                    og::block_node *block)
        : cdk::typed_node(lineno), _qualifier(qualifier),
          _identifier(identifier), _arguments(arguments), _block(block) {

        cdk::typed_node::type(
            std::shared_ptr<cdk::basic_type>(new cdk::primitive_type(0, cdk::TYPE_VOID))
            );
    }

    inline function_definition_node(int lineno, int qualifier,
                                    std::shared_ptr<cdk::basic_type> type,
                                    const std::string &identifier,
                                    cdk::sequence_node *arguments,
                                    og::block_node *block)
        : cdk::typed_node(lineno), _qualifier(qualifier),
          _identifier(identifier), _arguments(arguments), _block(block) {
        cdk::typed_node::type(type);
    }

public:
    inline int qualifier() { return _qualifier; }

    inline const std::string &identifier() const { return _identifier; }

    inline cdk::sequence_node *arguments() { return _arguments; }

    inline og::block_node *block() { return _block; }

    void accept(basic_ast_visitor *sp, int level) {
        sp->do_function_definition_node(this, level);
    }
};

} // namespace og

#endif
