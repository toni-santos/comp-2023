package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class OllirGenerator extends AJmmVisitor<OllirTemp, String> {

    private final SymbolTable symbolTable;
    private final StringBuilder code;
    private int indent = 0;
    private HashMap<String, String> methodParamsMap = new HashMap<String, String>();
    private HashMap<String, String> methodFieldsMap = new HashMap<String, String>();
    private HashMap<String, String> classFieldsMap = new HashMap<String, String>();
    private String methodReturn;

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
        addVisit("ArrayStatement", this::dealWithArrayStatement);
        addVisit("IntValue", this::dealWithIntValueExpression);
        addVisit("Identifier", this::dealWithIdentifierExpression);
        addVisit("NewObject", this::dealWithNewObject);
        addVisit("This", this::dealWithThisExpression);
    }

    private String dealWithThisExpression(JmmNode jmmNode, OllirTemp ollirTemp) {
        return "this";
    }

    private String dealWithIdentifierExpression(JmmNode jmmNode, OllirTemp ollirTemp) {
        return variableToOllirString(jmmNode.get("value"));
    }

    private String dealWithArrayStatement(JmmNode jmmNode, OllirTemp temp) {

        String arrayName = jmmNode.get("value");
        String arrayString = variableToOllirString(arrayName);

        if (arrayString == null) {
            code.append("ERROR: VARIABLE DEFINITION OVERLAPPING OR UNDEFINED");
            return "";
        }

        String arrayType = getTypeFromString(arrayString);
        String arrayElementType = Arrays.stream(arrayType.split("\\.")).toList().get(1);
        String bracketChild = visit(jmmNode.getJmmChild(0));

        String child = visit(jmmNode.getJmmChild(1));

        code.append(arrayName).append("[").append(bracketChild).append("].").append(arrayElementType).append(" :=.").append(arrayElementType).append(" ").append(child).append(";\n");

        return "";
    }

    private String dealWithRegularStatement(JmmNode jmmNode, OllirTemp temp) {
        String child = visit(jmmNode.getJmmChild(0));
        code.append(child).append(";\n");

        return "";
    }

    private String dealWithNewObject(JmmNode jmmNode, OllirTemp temp) {
        return "new(" + jmmNode.get("value") + ")." + jmmNode.get("value");
    }

    private String dealWithIntValueExpression(JmmNode jmmNode, OllirTemp temp) {
        return jmmNode.get("value") + ".i32";
    }

    private String dealWithReturnStatement(JmmNode jmmNode, OllirTemp temp) {
        String child = visit(jmmNode.getJmmChild(0));

        // method return type
        String retType = this.methodReturn;
        boolean isParam = this.methodParamsMap.containsKey(child);

        if (isParam) {
            code.append(getIndent()).append("ret").append(retType).append(" ").append(this.methodParamsMap.get(child)).append(";");
        } else {
            code.append(getIndent()).append("ret").append(retType).append(" ").append(child).append(retType).append(";");
        }

        return "";
    }

    private String dealWithDeclarationStatement(JmmNode jmmNode, OllirTemp temp) {
        JmmNode variable = jmmNode.getJmmChild(0);
        String variableName;

        boolean isArray = variable.hasAttribute("array");
        if (isArray) {
            variableName = variable.get("array");
        } else {
            variableName = variable.get("value");
        }

        String variableString = variableToOllirString(variableName);
        if (variableString == null) {
            code.append("ERROR: VARIABLE DEFINITION OVERLAPPING OR UNDEFINED");
            return "";
        }

        boolean isClassField = this.classFieldsMap.containsKey(variableName);

        if (isClassField) {
            String type = getTypeFromString(variableString);
            String child = visit(jmmNode.getJmmChild(1), new OllirTemp(type, true));

            code.append(getIndent()).append("putfield(this, ").append(variableString).append(child).append(";\n");
        } else {
            String type = getTypeFromString(variableString);
            String child = visit(jmmNode.getJmmChild(1), new OllirTemp());

            code.append(getIndent()).append(variableString).append(" :=.").append(type).append(" ").append(child).append(";\n");
        }

        return "";
    }

    private String dealWithMethodDeclaration(JmmNode jmmNode, OllirTemp temp) {
        String methodName = jmmNode.get("methodName");

        List<Symbol> methodParameters = symbolTable.getParameters(methodName);
        String methodReturnType = toOllirType(symbolTable.getReturnType(methodName));
        List<Symbol> localVariables = symbolTable.getLocalVariables(methodName);
        this.methodReturn = methodReturnType;

        if (methodName.equals("main")) {
            code.append(getIndent()).append(".method public static ").append(methodName).append("(");
        } else {
            code.append(getIndent()).append(".method public ").append(methodName).append("(");
        }

        List<String> parametersString = methodParameters.stream().map((param) -> {
            return param.getName() + toOllirType(param.getType());
        }).toList();
        setMethodParamsMap(methodParameters);

        String stringMethods = String.join(", ", parametersString);

        code.append(stringMethods).append(")").append(methodReturnType).append(" {\n");
        incrementIndent();

        // Method Local Variables
        for (Symbol variable : localVariables) {
            code.append(getIndent()).append(".field public ").append(variable.getName()).append(toOllirType(variable.getType())).append(";\n");
            methodFieldsMap.put(variable.getName(), variable.getName() + toOllirType(variable.getType()));
        }
        code.append("\n");

        // Statements & Return
        for (JmmNode child : jmmNode.getChildren().subList(localVariables.size(), jmmNode.getChildren().size())) {
            visit(child);
        }

        decrementIndent();
        code.append("\n").append(getIndent()).append("}\n");

        // Reset method information for next method
        this.methodParamsMap.clear();
        this.methodFieldsMap.clear();
        this.methodReturn = "";

        return "";
    }

    private String dealWithClass(JmmNode jmmNode, OllirTemp temp) {
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
            methodParamsMap.put(field.getName(), field.getName() + toOllirType(field.getType()));
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

    private String dealNext(JmmNode jmmNode, OllirTemp temp) {
        for (JmmNode child : jmmNode.getChildren()) {
            visit(child);
        }
        return "";
    }

    private void setMethodParamsMap(List<Symbol> methodParameters) {
        for (int i = 0; i < methodParameters.size(); i++) {
            Symbol param = methodParameters.get(i);

            methodParamsMap.put(param.getName(), "$" + i + "." + param.getName() + toOllirType(param.getType()));
        }
    }

    private String variableToOllirString(String variable) {
        boolean isMethodParam = this.methodParamsMap.containsKey(variable);
        boolean isClassField = this.classFieldsMap.containsKey(variable);
        boolean isMethodField = this.methodFieldsMap.containsKey(variable);

        if ((isMethodParam && isClassField) || (isClassField && isMethodField) || (isMethodParam && isMethodField)) {
            return null;
        }

        if (isClassField) {
            return this.classFieldsMap.get(variable);
        } else if (isMethodParam) {
            return this.methodParamsMap.get(variable);
        } else if (isMethodField){
            return this.methodFieldsMap.get(variable);
        } else {
            return null;
        }
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

    private String getTypeFromString(String string) {
        List<String> stringList = Arrays.stream(string.split("\\.")).toList();

        if (stringList.get(1).equals("array")) {
            return stringList.get(1) + stringList.get(2);
        }
        return stringList.get(1);
    }


    public String getCode() {
        return code.toString();
    }
}
