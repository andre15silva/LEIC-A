#include "targets/postfix_writer.h"
#include "ast/all.h" // all.h is automatically generated
#include "og_parser.tab.h"
#include "targets/frame_size_calculator.h"
#include "targets/type_checker.h"
#include <sstream>
#include <string>

//---------------------------------------------------------------------------

void og::postfix_writer::do_nil_node(cdk::nil_node *const node, int lvl) {
    // EMPTY
}
void og::postfix_writer::do_data_node(cdk::data_node *const node, int lvl) {
    // EMPTY
}
void og::postfix_writer::do_double_node(cdk::double_node *const node, int lvl) {
    if (_inFunctionBody) {
        _pf.DOUBLE(node->value());
    } else {
        _pf.SDOUBLE(node->value());
    }
}
void og::postfix_writer::do_not_node(cdk::not_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    node->argument()->accept(this, lvl);
    if (node->argument()->is_typed(cdk::TYPE_DOUBLE)) {
        _pf.DOUBLE(0);
        _pf.DCMP();
        _pf.INT(0);
        _pf.EQ();
    } else {
        _pf.INT(0);
        _pf.EQ();
    }
}

void og::postfix_writer::do_and_node(cdk::and_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    int end_and_lbl;
    node->left()->accept(this, lvl);
    _pf.DUP32();
    _pf.JZ(mklbl(end_and_lbl = ++_lbl));
    _pf.TRASH(4);
    node->right()->accept(this, lvl);
    _pf.ALIGN();
    _pf.LABEL(mklbl(end_and_lbl));
}

void og::postfix_writer::do_or_node(cdk::or_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    int end_or_lbl;
    node->left()->accept(this, lvl);
    _pf.DUP32();
    _pf.JNZ(mklbl(end_or_lbl = ++_lbl));
    _pf.TRASH(4);
    node->right()->accept(this, lvl);
    _pf.ALIGN();
    _pf.LABEL(mklbl(end_or_lbl));
}

//---------------------------------------------------------------------------

void og::postfix_writer::do_sequence_node(cdk::sequence_node *const node, int lvl) {
    for (size_t i = 0; i < node->size(); i++) {
        node->node(i)->accept(this, lvl);
    }
}

//---------------------------------------------------------------------------

void og::postfix_writer::do_integer_node(cdk::integer_node *const node, int lvl) {
    if (_inFunctionBody) {
        _pf.INT(node->value());
    } else {
        _pf.SINT(node->value());
    }
}

void og::postfix_writer::do_string_node(cdk::string_node *const node, int lvl) {
    int lbl1;
    _pf.RODATA();
    _pf.ALIGN();
    _pf.LABEL(mklbl(lbl1 = ++_lbl));
    _pf.SSTRING(node->value());
    if (_inFunctionBody) {
        _pf.TEXT();
        _pf.ADDR(mklbl(lbl1));
    } else {
        _pf.DATA();
        _pf.SADDR(mklbl(lbl1));
    }
}

//---------------------------------------------------------------------------

void og::postfix_writer::do_neg_node(cdk::neg_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    node->argument()->accept(this, lvl);
    if (node->argument()->is_typed(cdk::TYPE_DOUBLE)) {
        _pf.DNEG();
    } else {
        _pf.NEG();
    }
}

//---------------------------------------------------------------------------

void og::postfix_writer::do_add_node(cdk::add_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;

    if (node->is_typed(cdk::TYPE_POINTER)) {
        if (node->left()->is_typed(cdk::TYPE_POINTER)) {
            node->left()->accept(this, lvl);
            node->right()->accept(this, lvl);
            auto t = cdk::reference_type_cast(node->type());
            _pf.INT(t->referenced()->size());
            _pf.MUL();
            _pf.ADD();
        } else if (node->right()->is_typed(cdk::TYPE_POINTER)) {
            node->right()->accept(this, lvl);
            node->left()->accept(this, lvl);
            auto t = cdk::reference_type_cast(node->type());
            _pf.INT(t->referenced()->size());
            _pf.MUL();
            _pf.ADD();
        }
    } else {
        node->left()->accept(this, lvl);
        if (node->is_typed(cdk::TYPE_DOUBLE) && node->left()->is_typed(cdk::TYPE_INT))
            _pf.I2D();
        node->right()->accept(this, lvl);
        if (node->is_typed(cdk::TYPE_DOUBLE) && node->right()->is_typed(cdk::TYPE_INT))
            _pf.I2D();

        if (node->is_typed(cdk::TYPE_INT))
            _pf.ADD();
        else if (node->is_typed(cdk::TYPE_DOUBLE))
            _pf.DADD();
    }
}
void og::postfix_writer::do_sub_node(cdk::sub_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    if (node->left()->is_typed(cdk::TYPE_POINTER) && node->right()->is_typed(cdk::TYPE_POINTER)) {
        node->left()->accept(this, lvl);
        node->right()->accept(this, lvl);
        _pf.SUB();
        _pf.INT(cdk::reference_type_cast(node->left()->type())->referenced()->size());
        _pf.DIV();
    } else {
        node->left()->accept(this, lvl);
        if (node->is_typed(cdk::TYPE_DOUBLE) && node->left()->is_typed(cdk::TYPE_INT))
            _pf.I2D();
        node->right()->accept(this, lvl);
        if (node->is_typed(cdk::TYPE_DOUBLE) && node->right()->is_typed(cdk::TYPE_INT))
            _pf.I2D();

        if (node->is_typed(cdk::TYPE_INT))
            _pf.SUB();
        else if (node->is_typed(cdk::TYPE_DOUBLE))
            _pf.DSUB();
    }
}
void og::postfix_writer::do_mul_node(cdk::mul_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    node->left()->accept(this, lvl);
    if (node->is_typed(cdk::TYPE_DOUBLE) && node->left()->is_typed(cdk::TYPE_INT))
        _pf.I2D();
    node->right()->accept(this, lvl);
    if (node->is_typed(cdk::TYPE_DOUBLE) && node->right()->is_typed(cdk::TYPE_INT))
        _pf.I2D();

    if (node->is_typed(cdk::TYPE_INT))
        _pf.MUL();
    else if (node->is_typed(cdk::TYPE_DOUBLE))
        _pf.DMUL();
}
void og::postfix_writer::do_div_node(cdk::div_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    node->left()->accept(this, lvl);
    if (node->is_typed(cdk::TYPE_DOUBLE) && node->left()->is_typed(cdk::TYPE_INT))
        _pf.I2D();
    node->right()->accept(this, lvl);
    if (node->is_typed(cdk::TYPE_DOUBLE) && node->right()->is_typed(cdk::TYPE_INT))
        _pf.I2D();

    if (node->is_typed(cdk::TYPE_INT))
        _pf.DIV();
    else if (node->is_typed(cdk::TYPE_DOUBLE))
        _pf.DDIV();
}

void og::postfix_writer::do_mod_node(cdk::mod_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    node->left()->accept(this, lvl);
    node->right()->accept(this, lvl);
    _pf.MOD();
}
void og::postfix_writer::do_lt_node(cdk::lt_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    node->left()->accept(this, lvl);
    if (node->left()->is_typed(cdk::TYPE_INT) && node->right()->is_typed(cdk::TYPE_DOUBLE))
        _pf.I2D();
    node->right()->accept(this, lvl);
    if (node->right()->is_typed(cdk::TYPE_INT) && node->left()->is_typed(cdk::TYPE_DOUBLE))
        _pf.I2D();

    if (node->left()->is_typed(cdk::TYPE_DOUBLE) || node->right()->is_typed(cdk::TYPE_DOUBLE)) {
        _pf.DCMP();
        _pf.INT(0);
    }

    _pf.LT();
}

void og::postfix_writer::do_le_node(cdk::le_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    node->left()->accept(this, lvl);
    if (node->left()->is_typed(cdk::TYPE_INT) && node->right()->is_typed(cdk::TYPE_DOUBLE))
        _pf.I2D();
    node->right()->accept(this, lvl);
    if (node->right()->is_typed(cdk::TYPE_INT) && node->left()->is_typed(cdk::TYPE_DOUBLE))
        _pf.I2D();

    if (node->left()->is_typed(cdk::TYPE_DOUBLE) || node->right()->is_typed(cdk::TYPE_DOUBLE)) {
        _pf.DCMP();
        _pf.INT(0);
    }

    _pf.LE();
}
void og::postfix_writer::do_ge_node(cdk::ge_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    node->left()->accept(this, lvl);
    if (node->left()->is_typed(cdk::TYPE_INT) && node->right()->is_typed(cdk::TYPE_DOUBLE))
        _pf.I2D();
    node->right()->accept(this, lvl);
    if (node->right()->is_typed(cdk::TYPE_INT) && node->left()->is_typed(cdk::TYPE_DOUBLE))
        _pf.I2D();

    if (node->left()->is_typed(cdk::TYPE_DOUBLE) || node->right()->is_typed(cdk::TYPE_DOUBLE)) {
        _pf.DCMP();
        _pf.INT(0);
    }

    _pf.GE();
}
void og::postfix_writer::do_gt_node(cdk::gt_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    node->left()->accept(this, lvl);
    if (node->left()->is_typed(cdk::TYPE_INT) && node->right()->is_typed(cdk::TYPE_DOUBLE))
        _pf.I2D();
    node->right()->accept(this, lvl);
    if (node->right()->is_typed(cdk::TYPE_INT) && node->left()->is_typed(cdk::TYPE_DOUBLE))
        _pf.I2D();

    if (node->left()->is_typed(cdk::TYPE_DOUBLE) || node->right()->is_typed(cdk::TYPE_DOUBLE)) {
        _pf.DCMP();
        _pf.INT(0);
    }

    _pf.GT();
}
void og::postfix_writer::do_ne_node(cdk::ne_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    node->left()->accept(this, lvl);
    if (node->left()->is_typed(cdk::TYPE_INT) && node->right()->is_typed(cdk::TYPE_DOUBLE))
        _pf.I2D();
    node->right()->accept(this, lvl);
    if (node->right()->is_typed(cdk::TYPE_INT) && node->left()->is_typed(cdk::TYPE_DOUBLE))
        _pf.I2D();

    if (node->left()->is_typed(cdk::TYPE_DOUBLE) || node->right()->is_typed(cdk::TYPE_DOUBLE)) {
        _pf.DCMP();
        _pf.INT(0);
    }

    _pf.NE();
}
void og::postfix_writer::do_eq_node(cdk::eq_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    node->left()->accept(this, lvl);
    if (node->left()->is_typed(cdk::TYPE_INT) && node->right()->is_typed(cdk::TYPE_DOUBLE))
        _pf.I2D();
    node->right()->accept(this, lvl);
    if (node->right()->is_typed(cdk::TYPE_INT) && node->left()->is_typed(cdk::TYPE_DOUBLE))
        _pf.I2D();

    if (node->left()->is_typed(cdk::TYPE_DOUBLE) || node->right()->is_typed(cdk::TYPE_DOUBLE)) {
        _pf.DCMP();
        _pf.INT(0);
    }

    _pf.EQ();
}

//---------------------------------------------------------------------------

void og::postfix_writer::do_variable_node(cdk::variable_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    std::shared_ptr<og::symbol> symbol = _symtab.find(node->name());
    if (symbol->global()) {
        _pf.ADDR(symbol->name());
    } else {
        _pf.LOCAL(symbol->offset());
    }
}

void og::postfix_writer::do_rvalue_node(cdk::rvalue_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    node->lvalue()->accept(this, lvl);
    if (node->lvalue()->is_typed(cdk::TYPE_DOUBLE)) {
        _pf.LDDOUBLE();
    } else if (!node->lvalue()->is_typed(cdk::TYPE_STRUCT)) {
        _pf.LDINT();
    }
}

void og::postfix_writer::do_assignment_node(cdk::assignment_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    node->rvalue()->accept(this, lvl);
    if (node->is_typed(cdk::TYPE_DOUBLE)) {
        if (node->rvalue()->is_typed(cdk::TYPE_INT))
            _pf.I2D();
        _pf.DUP64();
        node->lvalue()->accept(this, lvl);
        _pf.STDOUBLE();
    } else if (node->is_typed(cdk::TYPE_INT) || node->is_typed(cdk::TYPE_POINTER) || node->is_typed(cdk::TYPE_STRING)) {
        _pf.DUP32();
        node->lvalue()->accept(this, lvl);
        _pf.STINT();
    } else if (node->is_typed(cdk::TYPE_STRUCT)) {
        _pf.DUP32();
        node->lvalue()->accept(this, lvl);
        _pf.STINT();
    }
}

//---------------------------------------------------------------------------

void og::postfix_writer::do_function_definition_node(og::function_definition_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    _inFunction = true;
    _symtab.push();

    _function = new_symbol();
    _functions_to_declare.erase(_function->name());
    reset_new_symbol();

    _offset = 8;
    if (node->arguments()) {
        _inFunctionArgs = true;
        for (auto n : node->arguments()->nodes()) {
            auto arg = dynamic_cast<og::variable_declaration_node *>(n);
            arg->accept(this, lvl);
        }
        _inFunctionArgs = false;
    }
    _returnOffset = _offset;
    _offset = 0;

    _pf.TEXT();
    _pf.ALIGN();
    if (node->qualifier() == tPUBLIC)
        _pf.GLOBAL(_function->name(), _pf.FUNC());
    _pf.LABEL(_function->name());

    og::frame_size_calculator calculator(_compiler, _symtab);
    node->accept(&calculator, lvl);
    _pf.ENTER(calculator.localsize());

    _inFunctionBody = true;
    _returnSeen = false;
    node->block()->accept(this, lvl + 4);
    _inFunctionBody = false;

    if (!_returnSeen && !node->is_typed(cdk::TYPE_VOID)) {
        throw std::string("missing return statement in function body");
    } else if (!_returnSeen && node->is_typed(cdk::TYPE_VOID)) {
        _pf.LEAVE();
        _pf.RET();
    }

    if (node->identifier() == "og")
        for (std::string s : _functions_to_declare)
            _pf.EXTERN(s);

    _symtab.pop();
    _inFunction = false;
    _function = nullptr;
}

//---------------------------------------------------------------------------

void og::postfix_writer::do_evaluation_node(og::evaluation_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    node->argument()->accept(this, lvl);
    if (node->argument()->is_typed(cdk::TYPE_INT) || node->argument()->is_typed(cdk::TYPE_STRING) ||
        node->argument()->is_typed(cdk::TYPE_POINTER)) {
        _pf.TRASH(4);
    } else if (node->argument()->is_typed(cdk::TYPE_DOUBLE)) {
        _pf.TRASH(8);
    } else if (node->argument()->is_typed(cdk::TYPE_STRUCT)) {
        _pf.TRASH(cdk::structured_type_cast(node->argument()->type())->size());
    } else if (node->argument()->is_typed(cdk::TYPE_VOID)) {
        // Nothing to TRASH
    } else {
        throw std::string("unsupported type in evaluation");
    }
}

void og::postfix_writer::do_print_node(og::print_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    for (auto n : node->argument()->nodes()) {
        auto tn = dynamic_cast<cdk::typed_node *>(n);
        tn->accept(this, lvl + 2);
        if (tn->is_typed(cdk::TYPE_INT) || tn->is_typed(cdk::TYPE_UNSPEC)) {
            _pf.CALL("printi");
            _pf.TRASH(4);
            _functions_to_declare.insert("printi");
        } else if (tn->is_typed(cdk::TYPE_DOUBLE)) {
            _pf.CALL("printd");
            _pf.TRASH(8);
            _functions_to_declare.insert("printd");
        } else if (tn->is_typed(cdk::TYPE_STRING)) {
            _pf.CALL("prints");
            _pf.TRASH(4);
            _functions_to_declare.insert("prints");
        } else {
            throw std::string("unsupported type in print");
        }
    }
    if (node->endl()) {
        _pf.CALL("println");
        _functions_to_declare.insert("println");
    }
}

//---------------------------------------------------------------------------

void og::postfix_writer::do_read_node(og::read_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    if (node->is_typed(cdk::TYPE_DOUBLE)) {
        _pf.CALL("readd");
        _functions_to_declare.insert("readd");
        _pf.LDFVAL64();
    } else if (node->is_typed(cdk::TYPE_INT)) {
        _pf.CALL("readi");
        _functions_to_declare.insert("readi");
        _pf.LDFVAL32();
    } else {
        throw std::string("unsupported type in read");
    }
}

//---------------------------------------------------------------------------

void og::postfix_writer::do_for_node(og::for_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;

    _forIni.push(++_lbl);
    _forStep.push(++_lbl);
    _forEnd.push(++_lbl);

    _inForIni = true;
    if (node->inits())
        node->inits()->accept(this, lvl + 2);
    _inForIni = false;

    _pf.ALIGN();
    _pf.LABEL(mklbl(_forIni.top()));
    if (node->conditions()) {
        for (auto n : node->conditions()->nodes()) {
            n->accept(this, lvl + 2);
            _pf.JZ(mklbl(_forEnd.top()));
        }
    } else {
        _pf.INT(1);
        _pf.JZ(mklbl(_forEnd.top()));
    }

    node->block()->accept(this, lvl + 2);

    _pf.ALIGN();
    _pf.LABEL(mklbl(_forStep.top()));
    _inForIncr = true;
    if (node->incrs())
        node->incrs()->accept(this, lvl + 2);
    _inForIncr = false;
    _pf.JMP(mklbl(_forIni.top()));

    _pf.ALIGN();
    _pf.LABEL(mklbl(_forEnd.top()));

    _forIni.pop();
    _forStep.pop();
    _forEnd.pop();
}

//---------------------------------------------------------------------------

void og::postfix_writer::do_if_node(og::if_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    int lbl1;
    node->condition()->accept(this, lvl);
    _pf.JZ(mklbl(lbl1 = ++_lbl));
    node->block()->accept(this, lvl + 2);
    _pf.LABEL(mklbl(lbl1));
}

//---------------------------------------------------------------------------

void og::postfix_writer::do_if_else_node(og::if_else_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    int lbl1, lbl2;
    node->condition()->accept(this, lvl);
    _pf.JZ(mklbl(lbl1 = ++_lbl));
    node->thenblock()->accept(this, lvl + 2);
    _pf.JMP(mklbl(lbl2 = ++_lbl));
    _pf.LABEL(mklbl(lbl1));
    node->elseblock()->accept(this, lvl + 2);
    _pf.LABEL(mklbl(lbl1 = lbl2));
}

void og::postfix_writer::do_address_of_node(og::address_of_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    node->lvalue()->accept(this, lvl);
}
void og::postfix_writer::do_block_node(og::block_node *const node, int lvl) {
    _symtab.push();
    if (node->declarations())
        node->declarations()->accept(this, lvl + 2);
    if (node->instructions())
        node->instructions()->accept(this, lvl + 2);
    _symtab.pop();
}

void og::postfix_writer::do_break_node(og::break_node *const node, int lvl) {
    if (_forIni.size() != 0) {
        _pf.JMP(mklbl(_forEnd.top()));
    } else {
        throw std::string("break outside for");
    }
}
void og::postfix_writer::do_continue_node(og::continue_node *const node, int lvl) {
    if (_forIni.size() != 0) {
        _pf.JMP(mklbl(_forStep.top()));
    } else {
        throw std::string("continue outside for");
    }
}
void og::postfix_writer::do_function_call_node(og::function_call_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;

    std::string name = node->identifier();
    if (name == "og")
        name = "_main";

    std::shared_ptr<og::symbol> symbol = _symtab.find(name);

    size_t arguments_size = 0;
    if (symbol->type()->name() == cdk::TYPE_STRUCT) {
        _pf.INT(symbol->type()->size());
        _pf.ALLOC();
        _pf.SP();
        arguments_size += 4;
    }
    if (node->arguments()) {
        for (int i = node->arguments()->size() - 1; i >= 0; i--) {
            auto tn = dynamic_cast<cdk::typed_node *>(node->arguments()->node(i));
            tn->accept(this, lvl + 4);
            if (symbol->function_params()[i]->name() == cdk::TYPE_DOUBLE && tn->is_typed(cdk::TYPE_INT))
                _pf.I2D();
            arguments_size += symbol->function_params()[i]->size();
        }
    }

    _pf.CALL(node->identifier());
    if (arguments_size != 0)
        _pf.TRASH(arguments_size);

    if (symbol->type()->name() == cdk::TYPE_INT || symbol->type()->name() == cdk::TYPE_POINTER ||
        symbol->type()->name() == cdk::TYPE_STRING) {
        _pf.LDFVAL32();
    } else if (symbol->type()->name() == cdk::TYPE_DOUBLE) {
        _pf.LDFVAL64();
    } else if (symbol->type()->name() == cdk::TYPE_STRUCT) {
        _pf.LDFVAL32();
    } else if (symbol->type()->name() == cdk::TYPE_VOID) {
        // Do nothing
    } else {
        throw std::string("Unsupported function call type");
    }
}
void og::postfix_writer::do_function_declaration_node(og::function_declaration_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    if (!new_symbol())
        return;
    std::shared_ptr<og::symbol> function = new_symbol();
    _functions_to_declare.insert(function->name());
    reset_new_symbol();
}
void og::postfix_writer::do_identity_node(og::identity_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    node->argument()->accept(this, lvl);
}
void og::postfix_writer::do_pointer_index_node(og::pointer_index_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    node->base()->accept(this, lvl);
    node->index()->accept(this, lvl);
    _pf.INT(node->type()->size());
    _pf.MUL();
    _pf.ADD();
}
void og::postfix_writer::do_nullptr_node(og::nullptr_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    if (_inFunctionBody) {
        _pf.INT(0);
    } else {
        _pf.SINT(0);
    }
}
void og::postfix_writer::do_return_node(og::return_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    if (node->retval()) {
        if (_function->type()->name() == cdk::TYPE_DOUBLE && node->retval()->is_typed(cdk::TYPE_INT)) {
            node->retval()->accept(this, lvl);
            _pf.I2D();
            _pf.STFVAL64();
        } else if (node->retval()->is_typed(cdk::TYPE_DOUBLE)) {
            node->retval()->accept(this, lvl);
            _pf.STFVAL64();
        } else if (node->retval()->is_typed(cdk::TYPE_INT) || node->retval()->is_typed(cdk::TYPE_POINTER) ||
                   node->retval()->is_typed(cdk::TYPE_STRING)) {
            node->retval()->accept(this, lvl);
            _pf.STFVAL32();
        } else if (node->retval()->is_typed(cdk::TYPE_STRUCT)) {
            int offset = 0;
            auto tn = dynamic_cast<og::tuple_node *>(node->retval());
            auto ret_type = cdk::structured_type_cast(tn->type());
            auto func_type = cdk::structured_type_cast(_function->type());
            for (size_t i = 0; i < ret_type->components().size(); i++) {
                auto ret_c = ret_type->component(i);
                auto func_c = func_type->component(i);
                if (func_c->name() == cdk::TYPE_DOUBLE && ret_c->name() == cdk::TYPE_INT) {
                    tn->values()->node(i)->accept(this, lvl);
                    _pf.I2D();
                    _pf.LOCAL(_returnOffset);
                    _pf.LDINT();
                    _pf.INT(offset);
                    _pf.ADD();
                    _pf.STDOUBLE();
                    offset += 8;
                } else if (ret_c->name() == cdk::TYPE_INT || ret_c->name() == cdk::TYPE_STRING ||
                           ret_c->name() == cdk::TYPE_POINTER) {
                    tn->values()->node(i)->accept(this, lvl);
                    _pf.LOCAL(_returnOffset);
                    _pf.LDINT();
                    _pf.INT(offset);
                    _pf.ADD();
                    _pf.STINT();
                    offset += 4;
                } else if (ret_c->name() == cdk::TYPE_DOUBLE) {
                    tn->values()->node(i)->accept(this, lvl);
                    _pf.LOCAL(_returnOffset);
                    _pf.LDINT();
                    _pf.INT(offset);
                    _pf.ADD();
                    _pf.STDOUBLE();
                    offset += 8;
                } else {
                    throw std::string("unsupported operation");
                }
            }
            _pf.LOCAL(_returnOffset);
            _pf.LDINT();
            _pf.STFVAL32();
        }
    }
    _pf.LEAVE();
    _pf.RET();
    _returnSeen = true;
}
void og::postfix_writer::do_size_of_node(og::size_of_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    _pf.INT(node->argument()->type()->size());
}
void og::postfix_writer::do_stack_alloc_node(og::stack_alloc_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    node->argument()->accept(this, lvl);
    auto t = cdk::reference_type_cast(node->type());
    _pf.INT(t->referenced()->size());
    _pf.MUL();
    _pf.ALLOC();
    _pf.SP();
}
void og::postfix_writer::do_tuple_node(og::tuple_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    node->values()->accept(this, lvl);
}

void og::postfix_writer::do_variable_declaration_node(og::variable_declaration_node *const node, int lvl) {
    if (!_inForIni)
        ASSERT_SAFE_EXPRESSIONS;

    std::string id = (*node->identifiers())[0];
    int offset = 0, typesize = node->type()->size();
    if (_inFunctionBody && !node->is_typed(cdk::TYPE_STRUCT)) {
        _offset -= typesize;
        offset = _offset;
    } else if (_inFunctionArgs) {
        offset = _offset;
        _offset += typesize;
    } else {
        offset = 0;
    }

    std::shared_ptr<og::symbol> symbol = new_symbol();
    if (symbol) {
        symbol->offset(offset);
        reset_new_symbol();
    }

    if (_inFunctionBody) {
        if (node->initializers()) {
            if (node->is_typed(cdk::TYPE_INT) || node->is_typed(cdk::TYPE_STRING) ||
                node->is_typed(cdk::TYPE_POINTER)) {
                node->initializers()->accept(this, lvl);
                _pf.LOCAL(symbol->offset());
                _pf.STINT();
            } else if (node->is_typed(cdk::TYPE_DOUBLE)) {
                node->initializers()->accept(this, lvl);
                if (node->initializers()->is_typed(cdk::TYPE_INT))
                    _pf.I2D();
                _pf.LOCAL(symbol->offset());
                _pf.STDOUBLE();
            } else if (node->is_typed(cdk::TYPE_STRUCT)) {
                if (node->identifiers()->size() == 1) {
                    _offset -= typesize;
                    offset = _offset;
                    symbol->offset(offset);
                    auto tn = dynamic_cast<og::tuple_node *>(node->initializers());
                    auto st = cdk::structured_type_cast(tn->type());
                    if (tn->values()->size() == st->components().size()) {
                        for (size_t i = 0; i < st->components().size(); i++) {
                            auto c = st->component(i);
                            if (c->name() == cdk::TYPE_INT || c->name() == cdk::TYPE_STRING ||
                                c->name() == cdk::TYPE_POINTER) {
                                tn->values()->node(i)->accept(this, lvl);
                                _pf.LOCAL(offset);
                                _pf.STINT();
                                offset += 4;
                            } else if (c->name() == cdk::TYPE_DOUBLE) {
                                tn->values()->node(i)->accept(this, lvl);
                                _pf.LOCAL(offset);
                                _pf.STDOUBLE();
                                offset += 8;
                            } else {
                                throw std::string("unsupported operation");
                            }
                        }
                    } else if (tn->values()->size() == 1) {
                        tn->values()->accept(this, lvl);
                        int currentOffset = 0;
                        for (size_t i = 0; i < st->components().size(); i++) {
                            auto c = st->component(i);
                            if (c->name() == cdk::TYPE_INT || c->name() == cdk::TYPE_STRING ||
                                c->name() == cdk::TYPE_POINTER) {
                                _pf.DUP32();
                                _pf.INT(currentOffset);
                                _pf.ADD();
                                _pf.LDINT();
                                _pf.LOCAL(offset);
                                _pf.STINT();
                                currentOffset += 4;
                                offset += 4;
                            } else if (c->name() == cdk::TYPE_DOUBLE) {
                                _pf.DUP32();
                                _pf.INT(currentOffset);
                                _pf.ADD();
                                _pf.LDDOUBLE();
                                _pf.LOCAL(offset);
                                _pf.STDOUBLE();
                                currentOffset += 8;
                                offset += 8;
                            } else {
                                throw std::string("unsupported operation");
                            }
                        }
                    } else {
                        throw std::string("unsupported operation");
                    }
                } else if (node->identifiers()->size() > 1) {
                    auto tn = dynamic_cast<og::tuple_node *>(node->initializers());
                    if (tn->values()->size() > 1) {
                        for (size_t i = 0; i < node->identifiers()->size(); i++) {
                            auto identifier = new std::vector<std::string>();
                            identifier->push_back(node->identifiers()->at(i));
                            auto en = dynamic_cast<cdk::expression_node *>(tn->values()->node(i));
                            auto vdn = new og::variable_declaration_node(node->lineno(), node->qualifier(), en->type(),
                                                                         identifier, en);
                            vdn->accept(this, lvl);
                        }
                    } else if (tn->values()->size() == 1) {
                        tn->values()->accept(this, lvl);
                        auto st = cdk::structured_type_cast(tn->type());
                        int currentOffset = 0;
                        for (size_t i = 0; i < st->components().size(); i++) {
                            auto identifier = new std::vector<std::string>();
                            identifier->push_back(node->identifiers()->at(i));
                            auto vdn = new og::variable_declaration_node(node->lineno(), node->qualifier(),
                                                                         st->component(i), identifier, nullptr);
                            vdn->accept(this, lvl);
                            std::shared_ptr<og::symbol> symbol = _symtab.find(node->identifiers()->at(i));
                            if (symbol->type()->name() == cdk::TYPE_INT || symbol->type()->name() == cdk::TYPE_STRING ||
                                symbol->type()->name() == cdk::TYPE_POINTER) {
                                _pf.DUP32();
                                _pf.INT(currentOffset);
                                _pf.ADD();
                                _pf.LDINT();
                                _pf.LOCAL(symbol->offset());
                                _pf.STINT();
                                currentOffset += 4;
                            } else if (symbol->type()->name() == cdk::TYPE_DOUBLE) {
                                _pf.DUP32();
                                _pf.INT(currentOffset);
                                _pf.ADD();
                                _pf.LDDOUBLE();
                                _pf.LOCAL(symbol->offset());
                                _pf.STDOUBLE();
                                currentOffset += 8;
                            } else {
                                throw std::string("unsupported operation");
                            }
                        }
                    } else {
                        throw std::string("unsupported operation");
                    }
                } else {
                    throw std::string("can't declare a variable with no identifier");
                }
            }
        } else {
            // No action required as the space is already reserved on the stack
        }
    } else if (!_inFunctionArgs) {
        if (!node->initializers()) {
            if (node->qualifier() == tREQUIRE) {
                _pf.EXTERN(id);
            } else {
                _pf.BSS();
                _pf.ALIGN();
                if (node->qualifier() == tPUBLIC)
                    _pf.GLOBAL(id, _pf.OBJ());
                _pf.LABEL(id);
                _pf.SALLOC(typesize);
            }
        } else {
            _pf.DATA();
            _pf.ALIGN();
            if (node->is_typed(cdk::TYPE_INT) || node->is_typed(cdk::TYPE_DOUBLE) ||
                node->is_typed(cdk::TYPE_POINTER)) {
                _pf.ALIGN();
                if (node->qualifier() == tPUBLIC)
                    _pf.GLOBAL(id, _pf.OBJ());
                _pf.LABEL(id);

                if (node->is_typed(cdk::TYPE_INT)) {
                    node->initializers()->accept(this, lvl);
                } else if (node->is_typed(cdk::TYPE_DOUBLE)) {
                    if (node->initializers()->is_typed(cdk::TYPE_DOUBLE)) {
                        node->initializers()->accept(this, lvl);
                    } else if (node->initializers()->is_typed(cdk::TYPE_INT)) {
                        auto dclini = dynamic_cast<cdk::integer_node *>(node->initializers());
                        cdk::double_node ddi(dclini->lineno(), dclini->value());
                        ddi.accept(this, lvl);
                    } else {
                        throw std::string("bad initializer for real value");
                    }
                }
            } else if (node->is_typed(cdk::TYPE_STRING)) {
                if (node->qualifier() == tPUBLIC)
                    _pf.GLOBAL(id, _pf.OBJ());
                _pf.LABEL(id);
                node->initializers()->accept(this, lvl);

            } else if (node->is_typed(cdk::TYPE_STRUCT)) {
                if (node->identifiers()->size() == 1) {
                    _pf.ALIGN();
                    _pf.LABEL(node->identifiers()->at(0));
                    node->initializers()->accept(this, lvl);
                } else if (node->identifiers()->size() > 1) {
                    auto tn = dynamic_cast<og::tuple_node *>(node->initializers());
                    for (size_t i = 0; i < node->identifiers()->size(); i++) {
                        auto identifier = new std::vector<std::string>();
                        identifier->push_back(node->identifiers()->at(i));
                        auto en = dynamic_cast<cdk::expression_node *>(tn->values()->node(i));
                        auto vdn = new og::variable_declaration_node(node->lineno(), node->qualifier(), en->type(),
                                                                     identifier, en);
                        vdn->accept(this, lvl);
                    }
                } else {
                    throw std::string("can't declare a variable with no identifier");
                }
            } else {
                throw std::string("unexpected type");
            }
        }
    }
}

void og::postfix_writer::do_tuple_index_node(og::tuple_index_node *const node, int lvl) {
    ASSERT_SAFE_EXPRESSIONS;
    size_t size_to_add = 0;
    auto st = cdk::structured_type_cast(node->base()->type());
    for (int i = 0; i < node->index()->value() - 1; i++) {
        size_to_add += st->component(i)->size();
    }
    node->base()->accept(this, lvl);
    _pf.INT(size_to_add);
    _pf.ADD();
}
