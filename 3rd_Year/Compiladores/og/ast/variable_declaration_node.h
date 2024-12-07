#ifndef __OG_AST_VARIABLE_DECLARATION_NODE_H__
#define __OG_AST_VARIABLE_DECLARATION_NODE_H__

#include <cdk/ast/basic_node.h>
#include <cdk/ast/expression_node.h>
#include <cdk/types/basic_type.h>
#include <cdk/types/primitive_type.h>
#include <cdk/types/typename_type.h>
#include <string>
#include <vector>

namespace og {
class variable_declaration_node : public cdk::typed_node {
private:
    int _qualifier;
    std::vector<std::string> *_identifiers;
    cdk::expression_node *_initializers;

public:
    inline variable_declaration_node(int lineno, int qualifier,
                                     std::shared_ptr<cdk::basic_type> type,
                                     std::vector<std::string> *identifiers,
                                     cdk::expression_node *initializers)
        : cdk::typed_node(lineno), _qualifier(qualifier),
          _identifiers(identifiers), _initializers(initializers) {
        cdk::typed_node::type(type);
    }

public:
    inline int qualifier() { return _qualifier; }
    inline std::vector<std::string> *identifiers() { return _identifiers; }
    inline cdk::expression_node *initializers() { return _initializers; }

    void accept(basic_ast_visitor *sp, int level) {
        sp->do_variable_declaration_node(this, level);
    }
};
} // namespace og

#endif
