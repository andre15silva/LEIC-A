#ifndef __OG_TARGETS_TYPE_CHECKER_H__
#define __OG_TARGETS_TYPE_CHECKER_H__

#include "targets/basic_ast_visitor.h"
#include <cdk/types/types.h>

namespace og {

/**
 * Print nodes as XML elements to the output stream.
 */
class type_checker : public basic_ast_visitor {
    cdk::symbol_table<og::symbol> &_symtab;
    std::shared_ptr<og::symbol> _function;
    basic_ast_visitor *_parent;

public:
    type_checker(std::shared_ptr<cdk::compiler> compiler, cdk::symbol_table<og::symbol> &symtab,
                 std::shared_ptr<og::symbol> function, basic_ast_visitor *parent)
        : basic_ast_visitor(compiler), _symtab(symtab), _function(function), _parent(parent) {}

public:
    ~type_checker() { os().flush(); }

protected:
    void processUnaryExpression(cdk::unary_operation_node *const node, int lvl);
    void processBinaryExpression(cdk::binary_operation_node *const node, int lvl);
    template <typename T> void process_literal(cdk::literal_node<T> *const node, int lvl) {}

    bool implicitlyDouble(std::shared_ptr<cdk::basic_type> type) {
        return type->name() == cdk::TYPE_INT || type->name() == cdk::TYPE_DOUBLE;
    }

    bool compatiblePointers(std::shared_ptr<cdk::basic_type> p1, std::shared_ptr<cdk::basic_type> p2) {
        if (p1->name() == cdk::TYPE_POINTER && p2->name() == cdk::TYPE_POINTER) {
            while (p1->name() == cdk::TYPE_POINTER && p2->name() == cdk::TYPE_POINTER) {
                p1 = cdk::reference_type_cast(p1)->referenced();
                p2 = cdk::reference_type_cast(p2)->referenced();
            }
            return p1->name() == p2->name() || p1->name() == cdk::TYPE_VOID || p2->name() == cdk::TYPE_VOID;
        }
        return false;
    }

    bool equalTypes(std::shared_ptr<cdk::basic_type> p1, std::shared_ptr<cdk::basic_type> p2) {
        if (p1->name() == cdk::TYPE_INT && p2->name() == cdk::TYPE_INT)
            return true;
        else if (p1->name() == cdk::TYPE_STRING && p2->name() == cdk::TYPE_STRING)
            return true;
        else if (p1->name() == cdk::TYPE_DOUBLE && p2->name() == cdk::TYPE_DOUBLE)
            return true;
        else if (p1->name() == cdk::TYPE_VOID && p2->name() == cdk::TYPE_VOID)
            return true;
        else if (p1->name() == cdk::TYPE_STRUCT && p2->name() == cdk::TYPE_STRUCT && equalStructs(p1, p2))
            return true;
        else if (p1->name() == cdk::TYPE_POINTER && p2->name() == cdk::TYPE_POINTER && compatiblePointers(p1, p2))
            return true;
        else
            return false;
    }

    bool equalStructs(std::shared_ptr<cdk::basic_type> p1, std::shared_ptr<cdk::basic_type> p2) {
        if (p1->name() == cdk::TYPE_STRUCT && p2->name() == cdk::TYPE_STRUCT) {
            auto p3 = cdk::structured_type_cast(p1);
            auto p4 = cdk::structured_type_cast(p2);
            if (p3->components().size() != p4->components().size()) {
                return false;
            } else {
                for (size_t i = 0; i < p3->components().size(); i++) {
                    if (!equalTypes(p3->component(i), p4->component(i))) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

public:
    // do not edit these lines
#define __IN_VISITOR_HEADER__
#include "ast/visitor_decls.h" // automatically generated
#undef __IN_VISITOR_HEADER__
    // do not edit these lines: end
};

} // namespace og

//---------------------------------------------------------------------------
//     HELPER MACRO FOR TYPE CHECKING
//---------------------------------------------------------------------------

#define CHECK_TYPES(compiler, symtab, function, node)                                                                  \
    {                                                                                                                  \
        try {                                                                                                          \
            og::type_checker checker(compiler, symtab, function, this);                                                \
            (node)->accept(&checker, 0);                                                                               \
        } catch (const std::string &problem) {                                                                         \
            std::cerr << (node)->lineno() << ": " << problem << std::endl;                                             \
            return;                                                                                                    \
        }                                                                                                              \
    }

#define ASSERT_SAFE_EXPRESSIONS CHECK_TYPES(_compiler, _symtab, _function, node)

#endif
