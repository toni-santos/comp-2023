package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.visitors.ImportVisitor;
import pt.up.fe.comp2023.visitors.MethodVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimpleSymbolTable implements SymbolTable {
    private List<String> imports;
    private String className;
    private String superClassName;
    private List<Method> methods;
    private Map<String, List<Symbol>> localVariables;
    private JmmNode root;

    public SimpleSymbolTable(JmmNode root) {
        this.root = root;


        this.imports = populateImports();
        this.methods = populateMethods();

//         this.classes = populateClasses(root);
//         this.fields = populateFields(root);

    }

    private List<Method> populateMethods() {
        List<Method> methods = new ArrayList<Method>();

        MethodVisitor methodVisitor = new MethodVisitor();
        methodVisitor.visit(this.root, methods);

        return methods;
    }

    private List<String> populateImports() {
        List<String> imports = new ArrayList<String>();

        ImportVisitor importVisitor = new ImportVisitor();
        importVisitor.visit(this.root, imports);

        return imports;
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
        return null;
    }

    @Override
    public List<String> getMethods() {
        return null;
    }

    @Override
    public Type getReturnType(String s) {
        return null;
    }

    @Override
    public List<Symbol> getParameters(String s) {
        return null;
    }

    @Override
    public List<Symbol> getLocalVariables(String s) {
        return this.localVariables.get(s);
    }

    @Override
    public String print() {
        return SymbolTable.super.print();
    }

    public void addMethod(Method method) {
        this.methods.add(method);
    }
}
