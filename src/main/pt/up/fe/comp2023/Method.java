package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;

public class Method {

    private Type returnType;
    private String name;
    private List<Symbol> parameters = new ArrayList<Symbol>();
    private String scope;
    private List<String> modifiers;
    private List<Symbol> variables = new ArrayList<Symbol>();

    public static String toString(Method object, Object o) {
        return object.name;
    }

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

    public List<Symbol> getParameters() {
        return this.parameters;
    }

    public void setParameters(List<Symbol> parameters) {
        this.parameters = parameters;
    }

    public void addParameter(Symbol parameter) {
        this.parameters.add(parameter);
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public List<String> getModifiers() {
        return modifiers;
    }

    public void setModifiers(List<String> modifiers) {
        this.modifiers = modifiers;
    }

    public List<Symbol> getVariables() {
        return this.variables;
    }

    public void addVariable(Symbol variable) {
        this.variables.add(variable);
    }
}
