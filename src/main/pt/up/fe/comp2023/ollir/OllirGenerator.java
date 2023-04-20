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
    private Integer auxNum = 0;
    private List<String> primitiveTypes = Arrays.asList(".bool", ".i32");

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
        addVisit("Parenthesis", this::dealWithParenthesis);
        addVisit("BinaryOp", this::dealWithBinaryOpExpression);
        addVisit("UnaryOp", this::dealWithUnaryOpExpression);
        addVisit("MethodCall", this::dealWithMethodCallExpression);
        addVisit("Length", this::dealWithLengthExpression);
        addVisit("ArraySubscript", this::dealWithArraySubscriptExpression);
    }

    private String dealWithParenthesis(JmmNode jmmNode, OllirTemp temp) {
        return visit(jmmNode.getJmmChild(0), new OllirTemp(temp.getType(), true));
    }

    private String dealWithArraySubscriptExpression(JmmNode jmmNode, OllirTemp temp) {
        String arrayName = visit(jmmNode.getJmmChild(0), new OllirTemp());
        String arrayType = Arrays.stream(arrayName.split("\\.")).toList().get(0);
        if (arrayType.equals("array")) {
            arrayType = Arrays.stream(arrayName.split("\\.")).toList().get(1);
        }
        String arrayAccess = visit(jmmNode.getJmmChild(0), new OllirTemp(arrayType, true));

        return arrayName + "[" + arrayAccess + "]." + arrayType;
    }

    private String dealWithLengthExpression(JmmNode jmmNode, OllirTemp temp) {

        String arrayName = visit(jmmNode.getJmmChild(0), new OllirTemp());

        return "arraylength(" + arrayName + ").i32";
    }

    private String dealWithMethodCallExpression(JmmNode jmmNode, OllirTemp temp) {
        String callerName = visit(jmmNode.getJmmChild(0), new OllirTemp(null, true));
        String methodName = jmmNode.get("value");
        String callerType, invokeMethod, returnType, argsString;
        ArrayList<String> args = new ArrayList<String>();

        if (callerName == null) {
            code.append("ERROR: VARIABLE DEFINITION OVERLAPPING OR UNDEFINED");
            return "";
        } else {
            if (!callerName.equals("this")) {
                if (this.symbolTable.getImports().contains(callerName)) {
                    callerType = ".V";
                } else {
                    callerType = callerName.split("\\.")[1];
                }
            } else {
                callerType = "this";
            }

        }

        if (callerType.equals(symbolTable.getClassName()) || callerType.equals("this")) {
            invokeMethod = "invokevirtual";
            returnType = toOllirType(symbolTable.getReturnType(methodName));
        } else {
            invokeMethod = "invokestatic";
            returnType = ".V";
        }

        // get arguments
        for (int i = 1; i < jmmNode.getNumChildren(); i++) {
            String arg = visit(jmmNode.getJmmChild(i), new OllirTemp(returnType, true));
            args.add(arg);
        }

        if (args.size() > 0) {
             argsString = ", " + String.join(", ", args);
        } else {
            argsString = "";
        }

        String string = invokeMethod + "(" + callerName + ", " + "\"" + methodName + "\"" + argsString + ")"+ returnType;

        if (temp.isTemp()) {
            if (callerType.equals(".V")) {
                return string;
            } else {
                this.auxNum++;
                String auxNumber = this.auxNum.toString();
                String auxType;
                if (temp.getType().startsWith(".")) {
                    auxType = temp.getType();
                } else {
                    auxType = "." + temp.getType();
                }
                String auxString = "aux" + auxNumber + returnType;

                code.append(getIndent()).append(auxString).append(" :=").append(returnType).append(" ").append(string).append(";\n");

                return auxString;
            }
        }

        return string;
    }

    private String dealWithUnaryOpExpression(JmmNode jmmNode, OllirTemp temp) {
        String child = visit(jmmNode.getJmmChild(0), new OllirTemp(".bool", true));

        String string = "!.bool " + child;

        if (temp.isTemp()) {
            this.auxNum++;
            String auxNumber = this.auxNum.toString();
            String auxString = "aux" + auxNumber + ".bool";

            code.append(getIndent()).append(auxString).append(" :=.bool ").append(string).append(";\n");
            return auxString;
        }

        return string;
    }

    private String dealWithBinaryOpExpression(JmmNode jmmNode, OllirTemp temp) {

        String binOp = jmmNode.get("op");
        String string, retType = null, childType = null;

        switch (binOp) {
            case "*", "/", "+", "-" -> {
                retType = ".i32";
                childType = ".i32";
            }
            case "<" -> {
                retType = ".bool";
                childType = ".i32";
            }
            case "&&" -> {
                retType = ".bool";
                childType = ".bool";
            }
            default -> {}
        }

        String left = visit(jmmNode.getJmmChild(0), new OllirTemp(childType, true));
        String right = visit(jmmNode.getJmmChild(1), new OllirTemp(childType, true));

        string = left + " " + binOp + retType + " " + right;

        if (temp.isTemp()) {
            this.auxNum++;
            String auxNumber = this.auxNum.toString();
            String auxString = "aux"+ auxNumber + retType;

            code.append(getIndent()).append(auxString).append(" :=").append(retType).append(" ").append(string).append(";\n");
            return auxString;
        }

        return string;
    }


    private String dealWithThisExpression(JmmNode jmmNode, OllirTemp temp) {
        return "this";
    }

    private String dealWithIdentifierExpression(JmmNode jmmNode, OllirTemp temp) {
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
        String child = visit(jmmNode.getJmmChild(0), new OllirTemp());
        code.append(getIndent()).append(child).append(";\n");

        return "";
    }

    private String dealWithNewObject(JmmNode jmmNode, OllirTemp temp) {
        String value = jmmNode.get("value");
        String retType = "." + value;
        String newString = "new(" + value + ")" + retType;

        this.auxNum++;
        String auxNumber = this.auxNum.toString();
        String auxString = "aux" + auxNumber + retType;
        code.append(getIndent()).append(auxString).append(" :=").append(retType).append(" ").append(newString).append(";\n");
        code.append(getIndent()).append("invokespecial(").append(auxString).append(",\"<init>\").V;\n");
        return auxString;
    }

    private String dealWithIntValueExpression(JmmNode jmmNode, OllirTemp temp) {
        return jmmNode.get("value") + ".i32";
    }

    private String dealWithReturnStatement(JmmNode jmmNode, OllirTemp temp) {
        String child = visit(jmmNode.getJmmChild(0), new OllirTemp(this.methodReturn, true));

        // method return type
        String retType = this.methodReturn;
        boolean isParam = this.methodParamsMap.containsKey(child);

        if (isParam) {
            code.append("\n").append(getIndent()).append("ret").append(retType).append(" ").append(this.methodParamsMap.get(child)).append(";");
        } else {
            code.append("\n").append(getIndent()).append("ret").append(retType).append(" ").append(child).append(";");
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
        String type = getTypeFromString(variableString);

        if (isClassField) {
            String child = visit(jmmNode.getJmmChild(1), new OllirTemp(type, true));

            code.append(getIndent()).append("putfield(this, ").append(variableString).append(", ").append(child).append(").").append(type).append(";\n");
        } else {
            String child = visit(jmmNode.getJmmChild(1), new OllirTemp(type, false));

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
            String type = toOllirType(variable.getType());
            code.append(getIndent()).append(variable.getName()).append(type).append(" :=").append(type).append(" ").append(getDefaultValueFromType(type)).append(";\n");

            if (!primitiveTypes.contains(type)) {
                code.append(getIndent()).append("invokespecial(").append(variable.getName()).append(type).append(",\"<init>\").V;\n");
            }

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
        this.auxNum = 0;

        return "";
    }

    private String getDefaultValueFromType(String type) {
        switch (type) {
            case ".bool" -> {
                return "1.bool";
            }
            case ".i32" -> {
                return "0.i32";
            }
            default -> {
                return "new(" + type.substring(1) + ")" + type;
            }
        }
    }

    private String dealWithClass(JmmNode jmmNode, OllirTemp temp) {
        for (String importStr : symbolTable.getImports()) {
            code.append("import ").append(importStr).append(";\n");
        }
        code.append("\n");

        code.append(symbolTable.getClassName());

        if (symbolTable.getSuper() != null && !symbolTable.getSuper().equals("")) {
            code.append(" extends ").append(symbolTable.getSuper());
        }
        
        code.append(" {\n");
        incrementIndent();

        // Class Fields
        for (Symbol field : symbolTable.getFields()) {
            code.append(getIndent()).append(".field public ").append(field.getName()).append(toOllirType(field.getType())).append(";\n");
            classFieldsMap.put(field.getName(), field.getName() + toOllirType(field.getType()));
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
            visit(child, new OllirTemp());
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
        boolean isImport = this.symbolTable.getImports().contains(variable);

        if ((isMethodParam && isClassField) || (isClassField && isMethodField) || (isMethodParam && isMethodField) || (isImport && isClassField) || (isImport && isMethodParam) || (isImport && isMethodField)) {
            return null;
        }

        if (isClassField) {
            return this.classFieldsMap.get(variable);
        } else if (isMethodParam) {
            return this.methodParamsMap.get(variable);
        } else if (isMethodField) {
            return this.methodFieldsMap.get(variable);
        } else if (isImport) {
            return this.symbolTable.getImports().stream().filter(importStr -> importStr.equals(variable)).collect(Collectors.joining());
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
