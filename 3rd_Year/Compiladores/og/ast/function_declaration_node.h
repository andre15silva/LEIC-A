#ifndef __OG_AST_FUNCTION_DECLARATION_NODE_H__
#define __OG_AST_FUNCTION_DECLARATION_NODE_H__

#include <cdk/ast/basic_node.h>
#include <cdk/ast/sequence_node.h>
#include <cdk/types/basic_type.h>
#include <cdk/types/primitive_type.h>
#include <cdk/types/typename_type.h>
#include <string>

namespace og {

/**
 * Class for describing function declarations.
 */
class function_declaration_node : public cdk::typed_node {
    int _qualifier;
    std::string _identifier;
    cdk::sequence_node *_arguments;

public:
    inline function_declaration_node(int lineno, int qualifier,
                                     const std::string &identifier,
                                     cdk::sequence_node *arguments)
        : cdk::typed_node(lineno), _qualifier(qualifier),
          _identifier(identifier), _arguments(arguments) {
        cdk::typed_node::type(std::shared_ptr<cdk::basic_type>(new cdk::primitive_type(0, cdk::TYPE_VOID)));
    }

    inline function_declaration_node(int lineno, int qualifier,
                                     std::shared_ptr<cdk::basic_type> type,
                                     const std::string &identifier,
                                     cdk::sequence_node *arguments)
        : cdk::typed_node(lineno), _qualifier(qualifier),
          _identifier(identifier), _arguments(arguments) {

        cdk::typed_node::type(type);
    }

public:
    inline int qualifier() { return _qualifier; }

    inline const std::string &identifier() const { return _identifier; }

    inline cdk::sequence_node *arguments() { return _arguments; }

    void accept(basic_ast_visitor *sp, int level) {
        sp->do_function_declaration_node(this, level);
    }
};

} // namespace og

#endif
