package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.List;
import java.util.Map;

public class SimpleSymbolTable implements SymbolTable {
    private List<String> imports;
    private String className;
    private String superClassName;
    private List<Symbol> fields;
    private List<String> methods;
    private Map<String, Type> returnType;
    private Map<String, List<Symbol>> parameters;
    private Map<String, List<Symbol>> localVariables;


    @Override
    public List<String> getImports() {
        return this.imports;
    }

    @Override
    public String getClassName() {
        return this.className;
    }

    @Override
    public String getSuper() {
        return this.superClassName;
    }

    @Override
    public List<Symbol> getFields() {
        return this.fields;
    }

    @Override
    public List<String> getMethods() {
        return this.methods;
    }

    @Override
    public Type getReturnType(String s) {
        return this.returnType.get(s);
    }

    @Override
    public List<Symbol> getParameters(String s) {
        return this.parameters.get(s);
    }

    @Override
    public List<Symbol> getLocalVariables(String s) {
        return this.localVariables.get(s);
    }

    @Override
    public String print() {
        return SymbolTable.super.print();
    }
}
