#include "targets/type_checker.h"
#include "ast/all.h" // automatically generated
#include "og_parser.tab.h"
#include <cdk/types/types.h>
#include <string>

#define ASSERT_UNSPEC                                                                                                  \
    {                                                                                                                  \
        if (node->type() != nullptr && !node->is_typed(cdk::TYPE_UNSPEC))                                              \
            return;                                                                                                    \
    }

//---------------------------------------------------------------------------

void og::type_checker::do_sequence_node(cdk::sequence_node *const node, int lvl) {
    for (size_t i = 0; i < node->size(); i++) {
        node->node(i)->accept(this, lvl);
    }
}

//---------------------------------------------------------------------------

void og::type_checker::do_nil_node(cdk::nil_node *const node, int lvl) {
    // EMPTY
}
void og::type_checker::do_data_node(cdk::data_node *const node, int lvl) {
    // EMPTY
}
void og::type_checker::do_double_node(cdk::double_node *const node, int lvl) {
    ASSERT_UNSPEC;
    node->type(cdk::make_primitive_type(8, cdk::TYPE_DOUBLE));
}
void og::type_checker::do_not_node(cdk::not_node *const node, int lvl) {
    processUnaryExpression(node, lvl);
    if (node->argument()->is_typed(cdk::TYPE_INT)) {
        node->type(cdk::make_primitive_type(4, cdk::TYPE_INT));
    } else {
        throw std::string("wrong type in not expresssion: expected integer but got " +
                          cdk::to_string(node->argument()->type()));
    }
}
void og::type_checker::do_and_node(cdk::and_node *const node, int lvl) {
    processBinaryExpression(node, lvl);
    if (node->left()->is_typed(cdk::TYPE_INT) && node->right()->is_typed(cdk::TYPE_INT)) {
        node->type(cdk::make_primitive_type(4, cdk::TYPE_INT));
    } else if (!node->left()->is_typed(cdk::TYPE_INT)) {
        throw std::string("wrong type in and expression: expected integer in left argument but got " +
                          cdk::to_string(node->left()->type()));
    } else if (!node->right()->is_typed(cdk::TYPE_INT)) {
        throw std::string("wrong type in and expression: expected integer in right argument but got " +
                          cdk::to_string(node->right()->type()));
    }
}
void og::type_checker::do_or_node(cdk::or_node *const node, int lvl) {
    processBinaryExpression(node, lvl);
    if (node->left()->is_typed(cdk::TYPE_INT) && node->right()->is_typed(cdk::TYPE_INT)) {
        node->type(cdk::make_primitive_type(4, cdk::TYPE_INT));
    } else if (!node->left()->is_typed(cdk::TYPE_INT)) {
        throw std::string("wrong type in or expression: expected integer in left argument but got " +
                          cdk::to_string(node->left()->type()));
    } else if (!node->right()->is_typed(cdk::TYPE_INT)) {
        throw std::string("wrong type in or expression: expected integer in right argument but got " +
                          cdk::to_string(node->right()->type()));
    }
}

//---------------------------------------------------------------------------

void og::type_checker::do_integer_node(cdk::integer_node *const node, int lvl) {
    ASSERT_UNSPEC;
    node->type(cdk::make_primitive_type(4, cdk::TYPE_INT));
}

void og::type_checker::do_string_node(cdk::string_node *const node, int lvl) {
    ASSERT_UNSPEC;
    node->type(cdk::make_primitive_type(4, cdk::TYPE_STRING));
}

//---------------------------------------------------------------------------

void og::type_checker::processUnaryExpression(cdk::unary_operation_node *const node, int lvl) {
    ASSERT_UNSPEC;
    node->argument()->accept(this, lvl + 2);
}

void og::type_checker::do_neg_node(cdk::neg_node *const node, int lvl) {
    processUnaryExpression(node, lvl);
    if (node->argument()->is_typed(cdk::TYPE_INT)) {
        node->type(cdk::make_primitive_type(4, cdk::TYPE_INT));
    } else if (node->argument()->is_typed(cdk::TYPE_DOUBLE)) {
        node->type(cdk::make_primitive_type(8, cdk::TYPE_DOUBLE));
    } else {
        throw std::string("wrong type in neg expresion: expected integer or double but got " +
                          cdk::to_string(node->argument()->type()));
    }
}

//---------------------------------------------------------------------------

void og::type_checker::processBinaryExpression(cdk::binary_operation_node *const node, int lvl) {
    ASSERT_UNSPEC;
    node->left()->accept(this, lvl + 2);
    node->right()->accept(this, lvl + 2);
}
void og::type_checker::do_add_node(cdk::add_node *const node, int lvl) {
    processBinaryExpression(node, lvl);
    if (node->left()->is_typed(cdk::TYPE_INT) && node->right()->is_typed(cdk::TYPE_INT)) {
        node->type(cdk::make_primitive_type(4, cdk::TYPE_INT));
    } else if (implicitlyDouble(node->left()->type()) && implicitlyDouble(node->right()->type())) {
        node->type(cdk::make_primitive_type(8, cdk::TYPE_DOUBLE));
    } else if (node->left()->is_typed(cdk::TYPE_POINTER) && node->right()->is_typed(cdk::TYPE_INT)) {
        auto t = cdk::reference_type_cast(node->left()->type());
        if (t->referenced()->name() == cdk::TYPE_UNSPEC || t->referenced()->name() == cdk::TYPE_VOID)
            throw std::string("arithmetic operation not supported for unspecified or void types");
        node->type(node->left()->type());
    } else if (node->left()->is_typed(cdk::TYPE_INT) && node->right()->is_typed(cdk::TYPE_POINTER)) {
        auto t = cdk::reference_type_cast(node->right()->type());
        if (t->referenced()->name() == cdk::TYPE_UNSPEC || t->referenced()->name() == cdk::TYPE_VOID)
            throw std::string("arithmetic operation not supported for unspecified or void types");
        node->type(node->right()->type());
    } else {
        throw std::string("wrong types in add expression");
    }
}
void og::type_checker::do_sub_node(cdk::sub_node *const node, int lvl) {
    processBinaryExpression(node, lvl);
    if (node->left()->is_typed(cdk::TYPE_INT) && node->right()->is_typed(cdk::TYPE_INT)) {
        node->type(cdk::make_primitive_type(4, cdk::TYPE_INT));
    } else if (implicitlyDouble(node->left()->type()) && implicitlyDouble(node->right()->type())) {
        node->type(cdk::make_primitive_type(8, cdk::TYPE_DOUBLE));
    } else if (node->left()->is_typed(cdk::TYPE_POINTER) && node->right()->is_typed(cdk::TYPE_INT)) {
        auto t = cdk::reference_type_cast(node->left()->type());
        if (t->referenced()->name() == cdk::TYPE_UNSPEC || t->referenced()->name() == cdk::TYPE_VOID)
            throw std::string("arithmetic operation not supported for unspecified or void types");
        node->type(node->left()->type());
    } else if (node->left()->is_typed(cdk::TYPE_INT) && node->right()->is_typed(cdk::TYPE_POINTER)) {
        auto t = cdk::reference_type_cast(node->right()->type());
        if (t->referenced()->name() == cdk::TYPE_UNSPEC || t->referenced()->name() == cdk::TYPE_VOID)
            throw std::string("arithmetic operation not supported for unspecified or void types");
        node->type(node->right()->type());
    } else if ((node->left()->is_typed(cdk::TYPE_POINTER) && node->right()->is_typed(cdk::TYPE_POINTER)) &&
               compatiblePointers(node->left()->type(), node->right()->type())) {
        node->type(cdk::make_primitive_type(4, cdk::TYPE_INT));
    } else {
        throw std::string("wrong types in sub expression");
    }
}

void og::type_checker::do_mul_node(cdk::mul_node *const node, int lvl) {
    processBinaryExpression(node, lvl);
    if (node->left()->is_typed(cdk::TYPE_INT) && node->right()->is_typed(cdk::TYPE_INT)) {
        node->type(cdk::make_primitive_type(4, cdk::TYPE_INT));
    } else if (implicitlyDouble(node->left()->type()) && implicitlyDouble(node->right()->type())) {
        node->type(cdk::make_primitive_type(8, cdk::TYPE_DOUBLE));
    } else {
        throw std::string("wrong types in mul expression");
    }
}

void og::type_checker::do_div_node(cdk::div_node *const node, int lvl) {
    processBinaryExpression(node, lvl);
    if (node->left()->is_typed(cdk::TYPE_INT) && node->right()->is_typed(cdk::TYPE_INT)) {
        node->type(cdk::make_primitive_type(4, cdk::TYPE_INT));
    } else if (implicitlyDouble(node->left()->type()) && implicitlyDouble(node->right()->type())) {
        node->type(cdk::make_primitive_type(8, cdk::TYPE_DOUBLE));
    } else {
        throw std::string("wrong types in div expression");
    }
}
void og::type_checker::do_mod_node(cdk::mod_node *const node, int lvl) {
    processBinaryExpression(node, lvl);
    if (node->left()->is_typed(cdk::TYPE_INT) && node->right()->is_typed(cdk::TYPE_INT)) {
        node->type(cdk::make_primitive_type(4, cdk::TYPE_INT));
    } else {
        throw std::string("wrong types in mod expression");
    }
}
void og::type_checker::do_lt_node(cdk::lt_node *const node, int lvl) {
    processBinaryExpression(node, lvl);
    if ((node->left()->is_typed(cdk::TYPE_INT) && node->right()->is_typed(cdk::TYPE_INT)) ||
        (implicitlyDouble(node->left()->type()) && implicitlyDouble(node->right()->type()))) {
        node->type(cdk::make_primitive_type(4, cdk::TYPE_INT));
    } else {
        throw std::string("wrong types in lt expression");
    }
}
void og::type_checker::do_le_node(cdk::le_node *const node, int lvl) {
    processBinaryExpression(node, lvl);
    if ((node->left()->is_typed(cdk::TYPE_INT) && node->right()->is_typed(cdk::TYPE_INT)) ||
        (implicitlyDouble(node->left()->type()) && implicitlyDouble(node->right()->type()))) {
        node->type(cdk::make_primitive_type(4, cdk::TYPE_INT));
    } else {
        throw std::string("wrong types in le expression");
    }
}
void og::type_checker::do_ge_node(cdk::ge_node *const node, int lvl) {
    processBinaryExpression(node, lvl);
    if ((node->left()->is_typed(cdk::TYPE_INT) && node->right()->is_typed(cdk::TYPE_INT)) ||
        (implicitlyDouble(node->left()->type()) && implicitlyDouble(node->right()->type()))) {
        node->type(cdk::make_primitive_type(4, cdk::TYPE_INT));
    } else {
        throw std::string("wrong types in ge expression");
    }
}
void og::type_checker::do_gt_node(cdk::gt_node *const node, int lvl) {
    processBinaryExpression(node, lvl);
    if ((node->left()->is_typed(cdk::TYPE_INT) && node->right()->is_typed(cdk::TYPE_INT)) ||
        (implicitlyDouble(node->left()->type()) && implicitlyDouble(node->right()->type()))) {
        node->type(cdk::make_primitive_type(4, cdk::TYPE_INT));
    } else {
        throw std::string("wrong types in gt expression");
    }
}
void og::type_checker::do_ne_node(cdk::ne_node *const node, int lvl) {
    processBinaryExpression(node, lvl);
    if ((node->left()->is_typed(cdk::TYPE_INT) && node->right()->is_typed(cdk::TYPE_INT)) ||
        (implicitlyDouble(node->left()->type()) && implicitlyDouble(node->right()->type())) ||
        (node->left()->is_typed(cdk::TYPE_POINTER) && node->right()->is_typed(cdk::TYPE_POINTER))) {
        node->type(cdk::make_primitive_type(4, cdk::TYPE_INT));
    } else {
        throw std::string("wrong types in ne expression");
    }
}

void og::type_checker::do_eq_node(cdk::eq_node *const node, int lvl) {
    processBinaryExpression(node, lvl);
    if ((node->left()->is_typed(cdk::TYPE_INT) && node->right()->is_typed(cdk::TYPE_INT)) ||
        (implicitlyDouble(node->left()->type()) && implicitlyDouble(node->right()->type())) ||
        (node->left()->is_typed(cdk::TYPE_POINTER) && node->right()->is_typed(cdk::TYPE_POINTER))) {
        node->type(cdk::make_primitive_type(4, cdk::TYPE_INT));
    } else {
        throw std::string("wrong types in eq expression");
    }
}

//---------------------------------------------------------------------------

void og::type_checker::do_variable_node(cdk::variable_node *const node, int lvl) {
    ASSERT_UNSPEC;
    const std::string &id = node->name();
    std::shared_ptr<og::symbol> symbol = _symtab.find(id);

    if (symbol && !symbol->isFunction()) {
        node->type(symbol->type());
    } else {
        throw id;
    }
}

void og::type_checker::do_rvalue_node(cdk::rvalue_node *const node, int lvl) {
    ASSERT_UNSPEC;
    try {
        node->lvalue()->accept(this, lvl);
        node->type(node->lvalue()->type());
    } catch (const std::string &id) {
        throw "undeclared variable '" + id + "'";
    }
}

void og::type_checker::do_assignment_node(cdk::assignment_node *const node, int lvl) {
    ASSERT_UNSPEC;
    try {
        node->lvalue()->accept(this, lvl);
    } catch (const std::string &id) {
        throw "undeclared variable '" + id + "'";
    }
    node->rvalue()->accept(this, lvl + 2);
    if (node->lvalue()->is_typed(cdk::TYPE_INT) && node->rvalue()->is_typed(cdk::TYPE_INT)) {
        node->type(cdk::make_primitive_type(4, cdk::TYPE_INT));
    } else if (node->lvalue()->is_typed(cdk::TYPE_STRING) && node->rvalue()->is_typed(cdk::TYPE_STRING)) {
        node->type(cdk::make_primitive_type(4, cdk::TYPE_STRING));
    } else if (implicitlyDouble(node->lvalue()->type()) && implicitlyDouble(node->rvalue()->type())) {
        node->type(cdk::make_primitive_type(8, cdk::TYPE_DOUBLE));
    } else if (compatiblePointers(node->lvalue()->type(), node->rvalue()->type())) {
        node->type(node->lvalue()->type());
    } else if (node->lvalue()->is_typed(cdk::TYPE_UNSPEC)) {
        node->lvalue()->type(node->rvalue()->type());
        node->type(node->rvalue()->type());
    } else if (node->rvalue()->is_typed(cdk::TYPE_UNSPEC)) {
        node->rvalue()->type(node->lvalue()->type());
        node->type(node->lvalue()->type());
    } else if (node->lvalue()->is_typed(cdk::TYPE_POINTER) && node->rvalue()->is_typed(cdk::TYPE_POINTER)) {
        auto t = cdk::reference_type_cast(node->rvalue()->type());
        if (t->referenced()->name() == cdk::TYPE_UNSPEC) {
            node->rvalue()->type(node->lvalue()->type());
            node->type(node->lvalue()->type());
        }
    } else {
        throw std::string("wrong types in assignment expression");
    }
}

//---------------------------------------------------------------------------

void og::type_checker::do_function_definition_node(og::function_definition_node *const node, int lvl) {
    std::string id;

    if (node->identifier() == "og")
        id = "_main";
    else
        id = node->identifier();

    std::vector<std::shared_ptr<cdk::basic_type>> function_params;
    if (node->arguments()) {
        for (auto param : node->arguments()->nodes()) {
            auto vn = dynamic_cast<og::variable_declaration_node *>(param);
            if (vn->qualifier() == tPUBLIC || vn->qualifier() == tREQUIRE || vn->type()->name() == cdk::TYPE_UNSPEC ||
                vn->initializers()) {
                throw std::string("function arguments do not support public and require qualifiers or default values");
            }
            function_params.push_back(vn->type());
        }
    }

    _function = std::make_shared<og::symbol>(node->type(), id, node->qualifier(), function_params, true);

    std::shared_ptr<og::symbol> previous = _symtab.find(_function->name());
    if (previous && previous->isFunction()) {
        if (previous->isDefined()) {
            throw std::string("function is already defined");
        } else if (!equalTypes(_function->type(), previous->type())) {
            throw std::string("function type in declaration if different from the definition");
        } else {
            if (previous->function_params().size() != (node->arguments() ? node->arguments()->size() : 0)) {
                throw std::string("function with same identifier but different arguments length");
            } else {
                for (size_t i = 0; i < previous->function_params().size(); i++) {
                    auto tn = dynamic_cast<cdk::typed_node *>(node->arguments()->node(i));
                    if (!equalTypes(previous->function_params()[i], tn->type())) {
                        throw std::string("function with same identifier but different argument type");
                    }
                }
            }
        }
        previous->defined(true);
        _parent->set_new_symbol(previous);
    } else {
        _symtab.insert(_function->name(), _function);
        _parent->set_new_symbol(_function);
    }
}

void og::type_checker::do_evaluation_node(og::evaluation_node *const node, int lvl) {
    node->argument()->accept(this, lvl + 2);
}

void og::type_checker::do_print_node(og::print_node *const node, int lvl) {
    node->argument()->accept(this, lvl + 2);
    for (auto i : node->argument()->nodes()) {
        auto tn = dynamic_cast<cdk::typed_node *>(i);
        if (tn->is_typed(cdk::TYPE_UNSPEC)) {
            tn->type(cdk::make_primitive_type(4, cdk::TYPE_INT));
        } else if (!tn->is_typed(cdk::TYPE_INT) && !tn->is_typed(cdk::TYPE_DOUBLE) && !tn->is_typed(cdk::TYPE_STRING)) {
            throw std::string("wrong type in write argument");
        }
    }
}

//---------------------------------------------------------------------------

void og::type_checker::do_read_node(og::read_node *const node, int lvl) {
    ASSERT_UNSPEC
    node->type(cdk::make_primitive_type(0, cdk::TYPE_UNSPEC));
}

//---------------------------------------------------------------------------

void og::type_checker::do_for_node(og::for_node *const node, int lvl) {
    if (node->inits())
        node->inits()->accept(this, lvl + 4);
    for (auto n : node->conditions()->nodes()) {
        auto tn = dynamic_cast<cdk::typed_node *>(n);
        tn->accept(this, lvl + 4);
        if (!tn->is_typed(cdk::TYPE_INT)) {
            throw std::string("expected integer condition in for conditions");
        }
    }
}

//---------------------------------------------------------------------------

void og::type_checker::do_if_node(og::if_node *const node, int lvl) {
    node->condition()->accept(this, lvl + 4);
    if (!node->condition()->is_typed(cdk::TYPE_INT))
        throw std::string("expected integer condition");
}

void og::type_checker::do_if_else_node(og::if_else_node *const node, int lvl) {
    node->condition()->accept(this, lvl + 4);
    if (!node->condition()->is_typed(cdk::TYPE_INT))
        throw std::string("expected integer condition");
}

void og::type_checker::do_address_of_node(og::address_of_node *const node, int lvl) {
    ASSERT_UNSPEC
    node->lvalue()->accept(this, lvl + 4);
    node->type(cdk::make_reference_type(4, node->lvalue()->type()));
}
void og::type_checker::do_block_node(og::block_node *const node, int lvl) {
    if (node->declarations())
        node->declarations()->accept(this, lvl);
    if (node->instructions())
        node->instructions()->accept(this, lvl);
}
void og::type_checker::do_break_node(og::break_node *const node, int lvl) {
    // EMPTY
}
void og::type_checker::do_continue_node(og::continue_node *const node, int lvl) {
    // EMPTY
}
void og::type_checker::do_function_call_node(og::function_call_node *const node, int lvl) {
    ASSERT_UNSPEC
    const std::string &id = node->identifier();
    std::shared_ptr<og::symbol> symbol = _symtab.find(id);

    if (symbol == nullptr)
        throw std::string("symbol '" + id + "' is undeclared.");
    if (!symbol->isFunction())
        throw std::string("symbol '" + id + "' is not a function.");

    if (node->arguments()->size() != symbol->function_params().size()) {
        throw std::string("incorrect number of arguments");
    }

    node->arguments()->accept(this, lvl + 2);
    for (size_t i = 0; i < node->arguments()->size(); i++) {
        auto tn = dynamic_cast<cdk::typed_node *>(node->arguments()->node(i));

        if (equalTypes(tn->type(), symbol->function_params()[i]) ||
            (implicitlyDouble(tn->type()) && implicitlyDouble(symbol->function_params()[i]))) {
            // No problemo
        } else {
            throw std::string("incorrect argument types");
        }
    }

    node->type(symbol->type());
}
void og::type_checker::do_function_declaration_node(og::function_declaration_node *const node, int lvl) {
    std::string id;

    if (node->identifier() == "og")
        id = "_main";
    else
        id = node->identifier();

    // Build arg vector
    std::vector<std::shared_ptr<cdk::basic_type>> function_params;
    if (node->arguments()) {
        for (auto param : node->arguments()->nodes()) {
            auto vn = dynamic_cast<og::variable_declaration_node *>(param);

            if (vn->qualifier() == tPUBLIC || vn->qualifier() == tREQUIRE || vn->type()->name() == cdk::TYPE_UNSPEC ||
                vn->initializers()) {
                throw std::string("function arguments do not support public and require qualifiers or defualt values");
            }

            function_params.push_back(vn->type());
        }
    }

    // Create symbol
    std::shared_ptr<og::symbol> function =
        std::make_shared<og::symbol>(node->type(), id, node->qualifier(), function_params, false);

    // Insert in symtab
    std::shared_ptr<og::symbol> previous = _symtab.find(function->name());

    if (previous && previous->isFunction()) {
        if (!equalTypes(function->type(), previous->type())) {
            throw std::string("function types of declaration mismatch");
        } else {
            if (previous->function_params().size() != (node->arguments() ? node->arguments()->size() : 0)) {
                throw std::string("function with same identifier but different arguments length");
            } else {
                for (size_t i = 0; i < previous->function_params().size(); i++) {
                    auto tn = dynamic_cast<cdk::typed_node *>(node->arguments()->node(i));
                    if (!equalTypes(previous->function_params()[i], tn->type())) {
                        throw std::string("function with same identifier but different argument type");
                    }
                }
            }
        }
    } else {
        _symtab.insert(function->name(), function);
        _parent->set_new_symbol(function);
    }
}
void og::type_checker::do_identity_node(og::identity_node *const node, int lvl) {
    processUnaryExpression(node, lvl);
    if (node->argument()->is_typed(cdk::TYPE_INT)) {
        node->type(cdk::make_primitive_type(4, cdk::TYPE_INT));
    } else if (node->argument()->is_typed(cdk::TYPE_DOUBLE)) {
        node->type(cdk::make_primitive_type(8, cdk::TYPE_DOUBLE));
    } else {
        throw std::string("wrong type in unary expression");
    }
}
void og::type_checker::do_pointer_index_node(og::pointer_index_node *const node, int lvl) {
    ASSERT_UNSPEC

    node->base()->accept(this, lvl + 2);
    if (!node->base()->is_typed(cdk::TYPE_POINTER)) {
        throw std::string("pointer expected in pointer indexation base");
    }
    node->index()->accept(this, lvl + 2);
    if (!node->index()->is_typed(cdk::TYPE_INT)) {
        throw std::string("integer expected in pointer indexation index");
    }

    node->type(cdk::reference_type_cast(node->base()->type())->referenced());
}
void og::type_checker::do_nullptr_node(og::nullptr_node *const node, int lvl) {
    ASSERT_UNSPEC
    node->type(cdk::make_reference_type(4, cdk::make_primitive_type(0, cdk::TYPE_UNSPEC)));
}
void og::type_checker::do_return_node(og::return_node *const node, int lvl) {
    if (_function) {
        std::shared_ptr<cdk::basic_type> ret_type;

        if (node->retval() != nullptr) {
            node->retval()->accept(this, lvl + 2);
            ret_type = node->retval()->type();
        } else {
            ret_type = make_primitive_type(0, cdk::TYPE_VOID);
        }

        if (equalTypes(_function->type(), ret_type) ||
            (_function->type()->name() == cdk::TYPE_DOUBLE && implicitlyDouble(ret_type))) {
            // All gucci
        } else if (_function->type()->name() == cdk::TYPE_UNSPEC &&
                   (ret_type->name() == cdk::TYPE_INT || ret_type->name() == cdk::TYPE_DOUBLE ||
                    ret_type->name() == cdk::TYPE_POINTER || ret_type->name() == cdk::TYPE_STRING ||
                    ret_type->name() == cdk::TYPE_STRUCT)) {
            _function->type(ret_type);
        } else if (_function->type()->name() == cdk::TYPE_STRUCT && ret_type->name() == cdk::TYPE_STRUCT) {
            auto t1 = cdk::structured_type_cast(_function->type());
            auto t2 = cdk::structured_type_cast(ret_type);
            std::vector<std::shared_ptr<cdk::basic_type>> components;
            if (t1->length() == t2->length()) {
                for (size_t i = 0; i < t1->length(); i++) {
                    if (equalTypes(t1->component(i), t2->component(i))) {
                        components.push_back(t1->component(i));
                    } else if (implicitlyDouble(t1->component(i)) && implicitlyDouble(t2->component(i))) {
                        components.push_back(cdk::make_primitive_type(8, cdk::TYPE_DOUBLE));
                    }
                }
                _function->type(cdk::make_structured_type(components));
            } else {
                throw std::string("return value type and function type mismatch");
            }
        } else {
            throw std::string("return value type and function type mismatch");
        }
    } else {
        throw std::string("can't have return outside function");
    }
}
void og::type_checker::do_size_of_node(og::size_of_node *const node, int lvl) {
    ASSERT_UNSPEC
    node->argument()->accept(this, lvl + 4);
    node->type(cdk::make_primitive_type(4, cdk::TYPE_INT));
}
void og::type_checker::do_stack_alloc_node(og::stack_alloc_node *const node, int lvl) {
    ASSERT_UNSPEC
    node->argument()->accept(this, lvl + 4);
    if (node->argument()->is_typed(cdk::TYPE_INT)) {
        node->type(cdk::make_reference_type(4, cdk::make_primitive_type(0, cdk::TYPE_UNSPEC)));
    } else {
        throw std::string("argument of stack allocation must be integer");
    }
}
void og::type_checker::do_tuple_node(og::tuple_node *const node, int lvl) {
    ASSERT_UNSPEC
    if (node->values()->size() == 1) {
        node->values()->node(0)->accept(this, lvl + 2);
        auto tn = dynamic_cast<cdk::typed_node *>(node->values()->node(0));
        node->type(tn->type());
    } else {
        std::vector<std::shared_ptr<cdk::basic_type>> components;
        node->values()->accept(this, lvl + 2);
        for (auto bn : node->values()->nodes()) {
            auto tn = dynamic_cast<cdk::typed_node *>(bn);
            components.push_back(tn->type());
        }
        node->type(cdk::make_structured_type(components));
    }
}
void og::type_checker::do_variable_declaration_node(og::variable_declaration_node *const node, int lvl) {
    if (_function && (node->qualifier() == tPUBLIC || node->qualifier() == tREQUIRE)) {
        throw std::string("can't define public or required variables inside function");
    }

    if (node->initializers()) {
        node->initializers()->accept(this, lvl + 2);

        if (node->identifiers()->size() == 1) {
            std::string id = (*node->identifiers())[0];

            if (equalTypes(node->type(), node->initializers()->type()) ||
                (node->is_typed(cdk::TYPE_DOUBLE) && implicitlyDouble(node->initializers()->type()))) {
                // Everything good
            } else if (node->is_typed(cdk::TYPE_POINTER) && node->initializers()->is_typed(cdk::TYPE_POINTER)) {
                auto t = cdk::reference_type_cast(node->initializers()->type());
                if (t->referenced()->name() == cdk::TYPE_UNSPEC)
                    node->initializers()->type(node->type());
            } else if (node->is_typed(cdk::TYPE_UNSPEC) && (node->initializers()->is_typed(cdk::TYPE_INT) ||
                                                            node->initializers()->is_typed(cdk::TYPE_DOUBLE) ||
                                                            node->initializers()->is_typed(cdk::TYPE_POINTER) ||
                                                            node->initializers()->is_typed(cdk::TYPE_STRING) ||
                                                            node->initializers()->is_typed(cdk::TYPE_STRUCT))) {
                node->type(node->initializers()->type());
            } else {
                throw std::string("incompatible type in initializer");
            }

            std::shared_ptr<og::symbol> symbol = std::make_shared<og::symbol>(node->type(), id, node->qualifier());
            symbol->defined(true);

            if (_symtab.insert(id, symbol)) {
                _parent->set_new_symbol(symbol);
            } else {
                throw std::string("variable '" + id + "' redeclared");
            }

        } else if (node->identifiers()->size() > 1) {
            if (node->initializers()->is_typed(cdk::TYPE_STRUCT)) {
                auto tn = dynamic_cast<og::tuple_node *>(node->initializers());
                if (node->identifiers()->size() == tn->values()->size()) {
                    node->type(node->initializers()->type());
                } else if (tn->values()->size() == 1) {
                    auto ttn = dynamic_cast<cdk::typed_node *>(tn->values()->node(0));
                    if (ttn->is_typed(cdk::TYPE_STRUCT)) {
                        auto t = cdk::structured_type_cast(ttn->type());
                        if (t->components().size() != node->identifiers()->size())
                            throw std::string("can't declare a sequence of different lengths in struct");
                        node->type(ttn->type());
                    } else {
                        throw std::string("can't assign a non struct type to a sequence");
                    }
                } else {
                    throw std::string("can't declare a sequence of different lengths");
                }
            } else {
                throw std::string("a tuple as to be a struct");
            }
        } else {
            throw std::string("can't declare a variable with no identifier");
        }

    } else {
        if (node->identifiers()->size() == 1) {
            std::string id = (*node->identifiers())[0];
            std::shared_ptr<og::symbol> symbol = std::make_shared<og::symbol>(node->type(), id, node->qualifier());
            if (_symtab.insert(id, symbol)) {
                _parent->set_new_symbol(symbol);
            } else {
                throw std::string("variable '" + id + "' redeclared");
            }
        } else {
            throw std::string("multiple variable declaration without initializers not supported");
        }
    }
}
void og::type_checker::do_tuple_index_node(og::tuple_index_node *const node, int lvl) {
    ASSERT_UNSPEC

    node->base()->accept(this, lvl + 2);
    if (!node->base()->is_typed(cdk::TYPE_STRUCT)) {
        throw std::string("cannot tuple index an expression that is not a tuple");
    }

    node->index()->accept(this, lvl + 2);
    if (!node->index()->is_typed(cdk::TYPE_INT)) {
        throw std::string("tuple indexation index must be an integer");
    }

    auto st = cdk::structured_type_cast(node->base()->type());
    node->type(st->component(node->index()->value() - 1));
}
