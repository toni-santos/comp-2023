package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
import java.util.stream.Collectors;

public class OllirGenerator extends AJmmVisitor<String, String> {

    private final SymbolTable symbolTable;
    private final StringBuilder code;
    private int indent = 0;

    OllirGenerator(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.code = new StringBuilder();
    }

    @Override
    protected void buildVisitor() {
        setDefaultVisit(this::dealNext);
        addVisit("ClassDeclaration", this::dealWithClass);
        addVisit("MethodDeclaration", this::dealWithMethodDeclaration);
        addVisit("DeclarationStatement", this::dealWithDeclarationStatement);
        addVisit("RegularStatement", this::dealWithRegularStatement);
        addVisit("ReturnStatement", this::dealWithReturnStatement);
        addVisit("IntValue", this::dealWithValueStatement);
        addVisit("Identifier", this::dealWithValueStatement);
        addVisit("NewObject", this::dealWithNewObject);
        addVisit("This", this::dealWithValueStatement);
    }

    private String dealWithRegularStatement(JmmNode jmmNode, String s) {
        visit(jmmNode.getJmmChild(0));
        code.append(";\n");

        return "";
    }

    private String dealWithNewObject(JmmNode jmmNode, String s) {
        StringBuilder string = new StringBuilder();
        return string.append("new(").append(jmmNode.get("value")).append(").").append(jmmNode.get("value")).toString();
    }

    private String dealWithValueStatement(JmmNode jmmNode, String s) {
        return jmmNode.get("value");
    }

    private String dealWithReturnStatement(JmmNode jmmNode, String s) {
        String retType = toOllirType(getMethodReturnType(jmmNode.getJmmParent()));
        String child = visit(jmmNode.getJmmChild(0));
        int isParam = checkMethodParam(child, jmmNode.getJmmParent());
        //String params = findMethodParams(jmmNode.getJmmParent());

        if (isParam >= 0) {
            code.append(getIndent()).append("ret").append(retType).append(" $").append(isParam).append(".").append(child).append(retType).append(";");
        } else {
            code.append(getIndent()).append("ret").append(retType).append(" ").append(child).append(retType).append(";");
        }

        return "";
    }

    private List<Symbol> getMethodParams(JmmNode jmmParent) {
        return symbolTable.getParameters(jmmParent.get("methodName"));
    }

    private Type getMethodReturnType(JmmNode jmmParent) {
        return symbolTable.getReturnType(jmmParent.get("methodName"));
    }

    private String dealWithDeclarationStatement(JmmNode jmmNode, String s) {
        JmmNode variable = jmmNode.getJmmChild(0);
        String variableName;

        // Get varName and check if is array
        // Should check if type is assignable?
        boolean isArray = variable.hasAttribute("array");
        if (isArray) {
            variableName = variable.get("array");
        } else {
            variableName = variable.get("value");
        }

        // Add $x before declaration
        int isMethodParam = checkMethodParam(variableName, jmmNode.getJmmParent());
        // Should check if not overlapping in scope, as in field in method == field in class
        boolean isClassField = checkClassField(variableName);

        if (isClassField) {
            Type type = symbolTable.getFields().stream().filter(field -> {
                return field.getName().equals(variableName);
            }).toList().get(0).getType();
            String typeStr = toOllirType(type);

            code.append(getIndent()).append("putfield(this, ").append(variableName).append(".").append(typeStr);
        } else {
            if (isMethodParam >= 0) {
                code.append(getIndent()).append("$").append(isMethodParam).append(".");
            }

            visit(jmmNode.getJmmChild(1));
        }

        code.append(";\n");

        return "";
    }

    private boolean checkClassField(String varName) {
        for (Symbol field : symbolTable.getFields())
            if (field.getName().equals(varName))
                return true;

        return false;
    }

    private int checkMethodParam(String varName, JmmNode parent) {
        String methodName = parent.get("methodName");
        List<Symbol> params = symbolTable.getParameters(methodName);

        for(int i = 0; i < params.size(); i++)
            if (params.get(i).getName().equals(varName))
                return i;

        return -1;
    }

    private String dealWithMethodDeclaration(JmmNode jmmNode, String s) {
        String methodName = jmmNode.get("methodName");

        List<Symbol> methodParameters = symbolTable.getParameters(methodName);
        String methodReturnType = toOllirType(symbolTable.getReturnType(methodName));
        List<Symbol> localVariables = symbolTable.getLocalVariables(methodName);

        if (methodName.equals("main")) {
            code.append(getIndent()).append(".method public static ").append(methodName).append("(");
        } else {
            code.append(getIndent()).append(".method public ").append(methodName).append("(");
        }

        List<String> parametersString = methodParameters.stream().map((param) -> {
            StringBuilder string = new StringBuilder();
            return string.append(param.getName()).append(toOllirType(param.getType())).toString();
        }).toList();

        String stringMethods = parametersString.stream().collect(Collectors.joining(", "));

        code.append(stringMethods).append(")").append(methodReturnType).append(" {\n");
        incrementIndent();

        List<String> variablesString = localVariables.stream().map((variable) -> {
            StringBuilder string = new StringBuilder();
            return string.append(variable.getName()).append(toOllirType(variable.getType())).toString();
        }).toList();

        // Method Local Variables
        for (Symbol variable : localVariables) {
            code.append(getIndent()).append(".field public ").append(variable.getName()).append(toOllirType(variable.getType())).append(";\n");
        }
        code.append("\n");
        // Variable assignment
        // for (int i = 0; i < localVariables.size(); i++) {
        //     code.append(getIndent()).append(variablesString.get(i)).append(" :=").append(toOllirType(localVariables.get(i).getType())).append(" ");
        // }

        // Statements & Return
        for (JmmNode child : jmmNode.getChildren().subList(localVariables.size(), jmmNode.getChildren().size())) {
            visit(child);
        }

        decrementIndent();
        code.append("\n").append(getIndent()).append("}\n");

        return "";
    }

    private String dealNext(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren()) {
            visit(child);
        }
        return "";
    }

    private String dealWithClass(JmmNode jmmNode, String s) {
        for (String importStr : symbolTable.getImports()) {
            code.append("import ").append(importStr).append(";\n");
        }
        code.append("\n");

        code.append(symbolTable.getClassName());

        if (symbolTable.getSuper() != null) {
            code.append(" extends ").append(symbolTable.getSuper());
        }
        
        code.append(" {\n");
        incrementIndent();

        // Class Fields
        for (Symbol field : symbolTable.getFields()) {
            code.append(getIndent()).append(".field public ").append(field.getName()).append(toOllirType(field.getType())).append(";\n");
        }

        // Defalut constructor
        code.append("\n").append(getIndent()).append(".construct ").append(symbolTable.getClassName()).append("().V {\n");
        incrementIndent();
        code.append(getIndent()).append("invokespecial(this, \"<init>\").V;\n");
        decrementIndent();
        code.append(getIndent()).append("}\n\n");

        // Class Methods
        for (JmmNode child : jmmNode.getChildren().subList(symbolTable.getFields().size(), jmmNode.getChildren().size())) {
            visit(child);
            code.append("\n");
        }

        decrementIndent();
        code.append("}\n");

        return "";
    }

    private void decrementIndent() {
        this.indent--;
    }

    private void incrementIndent() {
        this.indent++;
    }

    private String getIndent() {
        return "\t".repeat(this.indent);
    }

    private String toOllirType(Type type) {
        StringBuilder result = new StringBuilder();
        if (type.isArray() || type.getName().equals("String")) {
            result.append(".array");
        }

        switch (type.getName()) {
            case "int" -> result.append(".i32");
            case "boolean" -> result.append(".bool");
            case "void" -> result.append(".V");
            default ->
                // Strings here?
                result.append(".").append(type.getName());
        }

        return String.valueOf(result);
    }

    public String getCode() {
        return code.toString();
    }
}
