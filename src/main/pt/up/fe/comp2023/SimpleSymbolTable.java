package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
import java.util.Map;

public class SimpleSymbolTable implements SymbolTable {
    private List<String> imports;
    private String className;
    private String superClassName;
    private MethodMap methods;
    private List<Symbol> localVariables;
    private JmmNode root;

    public SimpleSymbolTable(JmmNode root) {
        this.root = root;
        SymbolTableVisitor visitor = new SymbolTableVisitor();
        visitor.visit(root);

        this.imports = visitor.imports;
        this.methods = new MethodMap(visitor.methods);
        this.className = visitor.className;
        this.superClassName = visitor.classExtends;
        this.localVariables = visitor.localVariables;

    }

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
        return this.localVariables;
    }

    @Override
    public List<String> getMethods() { return this.methods.getMethodsName(); }

    @Override
    public Type getReturnType(String s) {
        return this.methods.returnType(s);
    }

    @Override
    public List<Symbol> getParameters(String s) {
        return this.methods.getParameters(s);
    }

    @Override
    public List<Symbol> getLocalVariables(String s) { return null; }

    @Override
    public String print() {
        return SymbolTable.super.print();
    }

}
