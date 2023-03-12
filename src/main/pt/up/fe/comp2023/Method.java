package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.List;

public class Method {

    private Type returnType;
    private String name;
    private List<Symbol>  fields;

    public Method() {}

    public Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Symbol> getFields() {
        return fields;
    }

    public void setFields(List<Symbol> fields) {
        this.fields = fields;
    }

    public void addField(Symbol field) {
        this.fields.add(field);
    }
}
