#ifndef __OG_TARGETS_SYMBOL_H__
#define __OG_TARGETS_SYMBOL_H__

#include <cdk/types/basic_type.h>
#include <memory>
#include <string>

namespace og {

class symbol {
    std::shared_ptr<cdk::basic_type> _type;
    std::string _name;
    int _qualifier;
    bool _function;
    std::vector<std::shared_ptr<cdk::basic_type>> _function_params;
    bool _defined;
    int _offset = 0;

public:
    symbol(std::shared_ptr<cdk::basic_type> type, const std::string &name, int qualifier)
        : _type(type), _name(name), _qualifier(qualifier), _function(false), _defined(false) {}

    symbol(std::shared_ptr<cdk::basic_type> type, const std::string &name, int qualifier,
           std::vector<std::shared_ptr<cdk::basic_type>> function_params, bool defined)
        : _type(type), _name(name), _qualifier(qualifier), _function(true), _function_params(function_params),
          _defined(defined) {}

    virtual ~symbol() {
        // EMPTY
    }

    std::shared_ptr<cdk::basic_type> type() const { return _type; }
    void type(std::shared_ptr<cdk::basic_type> type) { _type = type; }
    bool is_typed(cdk::typename_type name) const { return _type->name() == name; }
    const std::string &name() const { return _name; }
    int qualifier() const { return _qualifier; }
    bool isFunction() const { return _function; }
    std::vector<std::shared_ptr<cdk::basic_type>> function_params() { return _function_params; }
    bool isDefined() const { return _defined; };
    void defined(bool defined) { _defined = defined; }
    int offset() const { return _offset; }
    void offset(int offset) { _offset = offset; }
    bool global() const { return _offset == 0; }
};
} // namespace og

#endif
