package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SymbolTableVisitor extends AJmmVisitor<Object, Boolean> {


    public List<Method> methods = new ArrayList<Method>();
    public List<String> imports = new ArrayList<String>();
    public String className = "";
    public String classExtends = "";
    public List<Symbol> localVariables = new ArrayList<Symbol>();

    @Override
    protected void buildVisitor() {
        setDefaultVisit(this::dealNext);
        addVisit("ImportDeclaration", this::dealWithImport);
        addVisit("ClassDeclaration", this::dealWithClass);
        addVisit("MethodDeclaration", this::dealWithMethod);
        addVisit("Program", this::dealNext);
        addVisit("ClassName", this::dealNext);
        addVisit("Type", this::dealNext);
        addVisit("MethodParam", this::dealNext);
        addVisit("VarDeclaration", this::dealNext);
        addVisit("ImportName", this::dealNext);
    }

    private Boolean dealWithClass(JmmNode jmmNode, Object dummy) {

        this.className = jmmNode.get("className");
        if (jmmNode.hasAttribute("extendName")) {
            this.classExtends = jmmNode.get("extendName");
        }

        for (JmmNode node: jmmNode.getChildren()) {
            if (node.getKind().equals("VarDeclaration")) {
                Symbol var = processVarDeclaration(node);
                localVariables.add(var);
            } else {
                visit(node);
            }
        }

        return true;
    }

    private Boolean dealWithMethod(JmmNode jmmNode, Object dummy) {
        Method method = new Method();
        int iter = 0;
        method.setReturnType(getTypeNode(jmmNode.getJmmChild(iter)));
        iter++;

        String methodName = jmmNode.get("methodName");
        method.setName(methodName);

        for (int i=iter; iter<jmmNode.getNumChildren(); iter++) {
            JmmNode node = jmmNode.getJmmChild(iter);
            switch (node.getKind()) {
                case "MethodParam" -> {
                    Symbol par = processMethodParam(node, method);
                    method.addParameter(par);
                }
                case "VarDeclaration" -> {
                    Symbol var = processVarDeclaration(node);
                    method.addVariable(var);
                }
                default -> {
                    visit(node);
                }
            }
        }

        this.methods.add(method);

        return true;
    }

    private Boolean dealWithImport(JmmNode jmmNode, Object dummy) {
        String importStr = jmmNode.getChildren().stream().map(node -> node.get("value"))
                .collect(Collectors.joining("."));
        this.imports.add(importStr);
        return true;
    }

    private Boolean dealNext(JmmNode jmmNode, Object dummy) {
        for (JmmNode child : jmmNode.getChildren()) {
            visit(child);
        }
        return true;
    }

    private Symbol processVarDeclaration(JmmNode node) {
        Type varType = getTypeNode(node.getJmmChild(0));
        String varName = node.get("value");

        return new Symbol(varType, varName);
    }

    private Symbol processMethodParam(JmmNode methodParam, Method method) {
        Type paramType = getTypeNode(methodParam.getJmmChild(0));
        String paramName = methodParam.get("name");

        return new Symbol(paramType, paramName);
    }

    private Type getTypeNode(JmmNode jmmNode) {
        String typeName = null;
        Boolean isArray = null;
        if (jmmNode.hasAttribute("array")) {
            typeName = jmmNode.get("array");
            isArray = true;
        } else {
            typeName = jmmNode.get("value");
            isArray = false;
        }
        return new Type(typeName, isArray);
    }

}
