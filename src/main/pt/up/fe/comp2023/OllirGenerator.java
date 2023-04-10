package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.stream.Collectors;

public class OllirGenerator extends AJmmVisitor<String, String> {

    private final SymbolTable symbolTable;
    private StringBuilder code = new StringBuilder();
    private int indent = 0;

    OllirGenerator(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    @Override
    protected void buildVisitor() {
        setDefaultVisit(this::dealNext);
        addVisit("ImportDeclaration", this::dealWithImport);
        addVisit("ClassDeclaration", this::dealWithClass);
        addVisit("VarDeclaration", this::dealWithVarDeclaration);
        addVisit("MethodDeclaration", this::dealWithMethodDeclaration);
    }

    private String dealWithMethodDeclaration(JmmNode jmmNode, String s) {
    }

    private String dealNext(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren()) {
            visit(child);
        }
        return "";
    }

    private String dealWithImport(JmmNode jmmNode, String s) {
        for (String importStr : symbolTable.getImports()) {
            code.append("import ").append(importStr).append(";\n");
        }
        code.append("\n");
        return "";
    }

    private String dealWithClass(JmmNode jmmNode, String s) {
        code.append(symbolTable.getClassName());

        if (symbolTable.getSuper() != null) {
            code.append(" extends ").append(symbolTable.getSuper());
        }
        
        code.append("{\n");
        addIndent();

        // Class Fields
        for (Symbol field : symbolTable.getFields()) {
            code.append(getIndent()).append(".field public").append(field.getName()).append(".").append(toOllirType(field.getType())).append(";\n");
        }

        // Defalut constructor
        code.append(getIndent()).append(".construct ").append(symbolTable.getClassName()).append("().V {\n");
        addIndent();
        code.append(getIndent()).append("invokespecial(this, \"<init>\").V;\n");
        removeIndent();
        code.append(getIndent()).append("}\n");

        // Class Methods
        for (JmmNode child : jmmNode.getChildren().subList(symbolTable.getFields().size(), jmmNode.getChildren().size())) {
            visit(child);
        }

        code.append("}\n");
        removeIndent();

        return "";
    }

    private String dealWithVarDeclaration(JmmNode jmmNode, String s) {
        return "";
    }

    private void removeIndent() {
        this.indent++;
    }

    private void addIndent() {
        this.indent--;
    }

    private String getIndent() {
        return "\t".repeat(this.indent);
    }

    private String toOllirType(Type type) {
        StringBuilder result = new StringBuilder();
        if (type.isArray()) {
            result.append(".array");
        }

        switch (type.getName()) {
            case "int" -> result.append(".i32");
            case "boolean" -> result.append(".bool");
            default ->
                // Strings here?
                result.append(type.getName());
        }

        return String.valueOf(result);
    }

}
