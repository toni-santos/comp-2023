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
    private Integer whileCount = 0;
    private Integer ifElseCount = 0;
    private List<String> primitiveTypes = Arrays.asList(".bool", ".i32");
    private String methodName;

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
        addVisit("NewArray", this::dealWithNewArray);
        addVisit("This", this::dealWithThisExpression);
        addVisit("Parenthesis", this::dealWithParenthesis);
        addVisit("BinaryOp", this::dealWithBinaryOpExpression);
        addVisit("UnaryOp", this::dealWithUnaryOpExpression);
        addVisit("MethodCall", this::dealWithMethodCallExpression);
        addVisit("Length", this::dealWithLengthExpression);
        addVisit("ArraySubscript", this::dealWithArraySubscriptExpression);
        addVisit("While", this::dealWithWhileStatement);
        addVisit("IfElse", this::dealWithIfElseStatement);
        addVisit("BooleanValue", this::dealWithBooleanValue);
    }

    private String dealWithBooleanValue(JmmNode jmmNode, OllirTemp ollirTemp) {
        switch (jmmNode.get("value")) {
            case "true" -> {
                return "1.bool";
            }
            case "false" -> {
                return "0.bool";
            }
        }

        return "";
    }

    private String dealWithNewArray(JmmNode jmmNode, OllirTemp ollirTemp) {
        String arrayInit = visit(jmmNode.getJmmChild(0), new OllirTemp("", true));

        return "new(array," + arrayInit + ")." + ollirTemp.getType();
    }

    private String dealWithParenthesis(JmmNode jmmNode, OllirTemp temp) {
        return visit(jmmNode.getJmmChild(0), new OllirTemp(temp.getType(), true));
    }

    private String dealWithArraySubscriptExpression(JmmNode jmmNode, OllirTemp temp) {
        String arrayString = visit(jmmNode.getJmmChild(0), new OllirTemp());

        String arrayType = getTypeFromString(arrayString);
        String arrayElementType = Arrays.stream(arrayType.split("\\.")).toList().get(1);

        String arrayAccess = visit(jmmNode.getJmmChild(1), new OllirTemp(arrayType, true));

        String string = arrayString + "[" + arrayAccess + "]." + arrayElementType;

        if (temp.isTemp()) {
            String retType = arrayElementType;
            String auxNumber = this.auxNum.toString();
            String auxString = "aux" + auxNumber + "." + retType;
            code.append(getIndent()).append(auxString).append(" :=.").append(retType).append(" ").append(string).append(";\n");
            auxNum++;
            return auxString;
        }

        return string;
    }

    private String dealWithLengthExpression(JmmNode jmmNode, OllirTemp temp) {

        String arrayName = visit(jmmNode.getJmmChild(0), new OllirTemp());
        String string = "arraylength(" + arrayName + ").i32";
        if (temp.isTemp()) {
            String auxString = "aux" + this.auxNum.toString() + ".i32";

            code.append(getIndent()).append(auxString).append(" :=").append(".i32").append(" ").append(string).append(";\n");
            this.auxNum++;
            return auxString;
        }
        return string;
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
                    String[] callerSplit = callerName.split("\\.");
                    callerType = callerSplit[callerSplit.length-1];
                }
            } else {
                callerType = "this";
            }

        }

        if (callerType.equals(symbolTable.getClassName()) || callerType.equals("this")) {
            invokeMethod = "invokevirtual";
            returnType = toOllirType(symbolTable.getReturnType(methodName));
        } else {
            if (symbolTable.getImports().contains(callerType)) {
                invokeMethod = "invokevirtual";
            } else {
                invokeMethod = "invokestatic";
            }
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

        String string;

        if (temp.isTemp()) {

            if (temp.getType() == null) {
                if (symbolTable.getMethods().contains(methodName)) {
                    returnType = toOllirType(symbolTable.getReturnType(methodName));
                } else {
                    returnType = ".V";
                }
            } else {
                List<String> typeList = Arrays.stream(temp.getType().split("\\.")).toList();
                if (typeList.get(0).equals("array")) {
                    returnType = addDot(typeList.get(1));
                } else {
                    returnType = addDot(temp.getType());
                }
            }

            string = invokeMethod + "(" + callerName + ", " + "\"" + methodName + "\"" + argsString + ")"+ returnType;

            String auxString = "aux" + this.auxNum.toString() + returnType;

            code.append(getIndent()).append(auxString).append(" :=").append(returnType).append(" ").append(string).append(";\n");
            this.auxNum++;
            return auxString;
        }

        string = invokeMethod + "(" + callerName + ", " + "\"" + methodName + "\"" + argsString + ")"+ returnType;

        return string;
    }

    private String addDot(String type) {
        if (type.charAt(0) != '.') {
            return "." + type;
        }
        return type;
    }

    private String dealWithUnaryOpExpression(JmmNode jmmNode, OllirTemp temp) {
        String child = visit(jmmNode.getJmmChild(0), new OllirTemp(".bool", true));

        String string = "!.bool " + child;

        if (temp.isTemp()) {
            String auxNumber = this.auxNum.toString();
            String auxString = "aux" + auxNumber + ".bool";

            code.append(getIndent()).append(auxString).append(" :=.bool ").append(string).append(";\n");
            this.auxNum++;

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
            String auxNumber = this.auxNum.toString();
            String auxString = "aux"+ auxNumber + retType;

            code.append(getIndent()).append(auxString).append(" :=").append(retType).append(" ").append(string).append(";\n");
            this.auxNum++;

            return auxString;
        }

        return string;
    }


    private String dealWithThisExpression(JmmNode jmmNode, OllirTemp temp) {
        return "this";//+symbolTable.getClassName();
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
        String bracketChild = visit(jmmNode.getJmmChild(0), new OllirTemp(".i32", true));

        String child = visit(jmmNode.getJmmChild(1), new OllirTemp(arrayType, true));

        code.append(getIndent()).append(arrayName).append("[").append(bracketChild).append("].").append(arrayElementType).append(" :=.").append(arrayElementType).append(" ").append(child).append(";\n");

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

        String auxNumber = this.auxNum.toString();
        String auxString = "aux" + auxNumber + retType;
        code.append(getIndent()).append(auxString).append(" :=").append(retType).append(" ").append(newString).append(";\n");
        code.append(getIndent()).append("invokespecial(").append(auxString).append(",\"<init>\").V;\n");
        this.auxNum++;

        return auxString;
    }

    private String dealWithIntValueExpression(JmmNode jmmNode, OllirTemp temp) {
        return jmmNode.get("value") + ".i32";
    }

    private String dealWithReturnStatement(JmmNode jmmNode, OllirTemp temp) {
        if (methodName.equals("main")) {
            code.append("\n").append(getIndent()).append("ret.V;");
            return "";
        }

        String child = visit(jmmNode.getJmmChild(0), new OllirTemp(this.methodReturn, true));

        // method return type
        String retType = this.methodReturn;
        boolean isParam = this.methodParamsMap.containsKey(child);

        if (isParam) {
            code.append("\n").append(getIndent()).append("ret").append(retType).append(" ").append(this.methodParamsMap.get(child)).append(";");
        } else {
            if (child.equals("this")) {
                code.append("\n").append(getIndent()).append("ret").append(retType).append(" ").append(child).append(retType).append(";");
            } else {
                code.append("\n").append(getIndent()).append("ret").append(retType).append(" ").append(child).append(";");
            }
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

        if (jmmNode.getJmmChild(1).getKind().equals("Identifier")) {
            String childValue = jmmNode.getJmmChild(1).get("value");
            if (this.classFieldsMap.containsKey(childValue)) {
                String childOllir = variableToOllirString(childValue);
                if (this.classFieldsMap.containsKey(variableName)) {
                    String auxString = "aux" + auxNum + "." + type;
                    code.append(getIndent()).append(auxString).append(" :=.").append(type).append(" getfield(this, ").append(childOllir).append(").").append(type).append(";\n");
                    code.append(getIndent()).append("putfield(this, ").append(variableString).append(", ").append(auxString).append(").V").append(";");
                } else {
                    code.append(getIndent()).append(variableString).append(" :=.").append(type).append(" getfield(this, ").append(childOllir).append(").V").append(";");
                }
                return "";
            }
        }

        if (isClassField && !(this.methodFieldsMap.containsKey(variableName) || this.methodParamsMap.containsKey(variableName))) {
            String child = visit(jmmNode.getJmmChild(1), new OllirTemp(type, true));

            code.append(getIndent()).append("putfield(this, ").append(variableString).append(", ").append(child).append(").V").append(";\n");
        } else {
            String child;

            if (jmmNode.getJmmChild(1).getKind().equals("MethodCall")) {
                child = visit(jmmNode.getJmmChild(1), new OllirTemp(type, true));
            } else {
                child = visit(jmmNode.getJmmChild(1), new OllirTemp(type, true));
            }

            code.append(getIndent()).append(variableString).append(" :=.").append(type).append(" ").append(child).append(";\n");
        }

        return "";
    }

    private String dealWithMethodDeclaration(JmmNode jmmNode, OllirTemp temp) {
        String methodName = jmmNode.get("methodName");
        this.methodName = methodName;

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
            methodFieldsMap.put(variable.getName(), variable.getName() + toOllirType(variable.getType()));
        }
        code.append("\n");

        // Statements & Return
        for (JmmNode child : jmmNode.getChildren().subList(localVariables.size(), jmmNode.getChildren().size())) {
            visit(child);
        }

        if (methodName.equals("main")) {
            code.append(getIndent()).append("ret.V;\n");
        }

        decrementIndent();
        code.append("\n").append(getIndent()).append("}\n");

        // Reset method information for next method
        this.methodParamsMap.clear();
        this.methodFieldsMap.clear();
        this.methodReturn = "";
        this.methodName = "";

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

        // Default constructor
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

    private String dealWithWhileStatement(JmmNode jmmNode, OllirTemp temp) {

        code.append(getIndent()).append("goto while_cond_").append(getWhileCount()).append(";\n");
        code.append(getIndent()).append("while_body_").append(getWhileCount()).append(":\n");

        incrementWhileCount();
        // while loop body
        incrementIndent();
        String whileStatement = visit(jmmNode.getJmmChild(1), new OllirTemp());
        decrementIndent();
        decrementWhileCount();

        code.append(getIndent()).append("while_cond_").append(getWhileCount()).append(":\n");

        String whileCondition = visit(jmmNode.getJmmChild(0), new OllirTemp("bool", true)); // possibly move to temp variable

        // check condition (if expression goto while_body;)
        incrementIndent();
        code.append(getIndent()).append("if(").append(whileCondition).append(") goto while_body").append(getWhileCount()).append(";\n");
        decrementIndent();



        return "";
    }


    private String dealWithIfElseStatement(JmmNode jmmNode, OllirTemp temp) {

        String ifCondition = visit(jmmNode.getJmmChild(0), new OllirTemp("bool", true)); // possibly move to temp variable

        // check condition (if expression goto if_then;)
        code.append(getIndent()).append("if(").append(ifCondition).append(") goto if_then_").append(getIfElseCount()).append(";\n");
        incrementIfElseCount();

        // else body
        incrementIndent();
        String elseStatement = visit(jmmNode.getJmmChild(2), new OllirTemp());
        decrementIndent();
        decrementIfElseCount();

        code.append(getIndent()).append("goto if_end_").append(getIfElseCount()).append(";\n");
        code.append(getIndent()).append("if_then_").append(getIfElseCount()).append(":\n");

        // if then body
        incrementIndent();
        String ifStatement = visit(jmmNode.getJmmChild(1), new OllirTemp());
        decrementIndent();

        code.append(getIndent()).append("if_end_").append(getIfElseCount()).append(":\n");
        incrementIndent();


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

            methodParamsMap.put(param.getName(), "$" + (i+1) + "." + param.getName() + toOllirType(param.getType()));
        }
    }

    private String variableToOllirString(String variable) {
        if (variable.equals(symbolTable.getClassName())) {
            return "this";
        }

        boolean isMethodParam = this.methodParamsMap.containsKey(variable);
        boolean isClassField = this.classFieldsMap.containsKey(variable);
        boolean isMethodField = this.methodFieldsMap.containsKey(variable);
        boolean isImport = this.symbolTable.getImports().contains(variable);

        /*if ((isMethodParam && isClassField) || (isClassField && isMethodField) || (isMethodParam && isMethodField) || (isImport && isClassField) || (isImport && isMethodParam) || (isImport && isMethodField)) {
            return null;
        }*/

        if (isMethodField) {
            return this.methodFieldsMap.get(variable);
        } else if (isMethodParam) {
            return this.methodParamsMap.get(variable);
        } else if (isClassField) {
            return this.classFieldsMap.get(variable);
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

    private void decrementWhileCount() {
        this.whileCount--;
    }

    private void incrementWhileCount() {
        this.whileCount++;
    }

    private String getWhileCount(){
        return this.whileCount.toString();
    }

    private void decrementIfElseCount() {
        this.ifElseCount--;
    }

    private void incrementIfElseCount() {
        this.ifElseCount++;
    }

    private String getIfElseCount(){
        return this.ifElseCount.toString();
    }


    private String toOllirType(Type type) {
        StringBuilder result = new StringBuilder();
        if (type.isArray()) {
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
            return stringList.get(1) + "." + stringList.get(2);
        } else if (stringList.size() >= 3 && stringList.get(2).equals("array")) {
            return stringList.get(2) + "." + stringList.get(3);
        }
        return stringList.get(1);
    }


    public String getCode() {
        return code.toString();
    }
}
