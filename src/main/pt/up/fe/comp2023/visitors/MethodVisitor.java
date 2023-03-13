package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmVisitor;
import pt.up.fe.comp2023.Method;
import pt.up.fe.comp2023.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MethodVisitor extends AJmmVisitor<List<Method>, Boolean> implements VisitorInterface {

    @Override
    protected void buildVisitor() {
        addVisit("methodDeclaration", this::dealWithMethod);
    }

    private Boolean dealWithMethod(JmmNode jmmNode, List<Method> methods) {
        Method method = new Method();

        int iter = 0;
        String methodScope = jmmNode.getJmmChild(iter).get("text");
        iter++;
        method.setScope(methodScope);
        if (jmmNode.getJmmChild(iter).getKind().equals("mod")) {
            method.setModifiers(new ArrayList<String>(Arrays.asList(jmmNode.getJmmChild(1).get("text"))));
            iter++;
        }

        method.setReturnType(getTypeNode(jmmNode.getJmmChild(iter)));
        iter++;

        String methodName = jmmNode.getJmmChild(iter).get("text");
        method.setName(methodName);
        iter++;

        for (int i=iter; iter<jmmNode.getNumChildren(); iter++) {
            JmmNode node = jmmNode.getJmmChild(iter);
            switch (node.getKind()) {
                case "methodParam":
                    processMethodParam(node, method);
                    break;
                case "varDeclaration":
                    processVarDeclaration(node, method);
                    break;
                case "statement":
                    // TODO: make processStatementDeclaration
                    break;
                default:
                    break;
            }
        }

        return true;
    }

    private void processVarDeclaration(JmmNode node, Method method) {
        Type varType = getTypeNode(node);
        String varName = node.getJmmChild(1).get("text");

        Variable var = new Variable(varType, varName);
    }

    private void processMethodParam(JmmNode methodParam, Method method) {
        Type paramType = getTypeNode(methodParam.getJmmChild(0));
        String paramName = methodParam.getJmmChild(1).get("text");

        method.addField(new Symbol(paramType, paramName));
    }

    private Type getTypeNode(JmmNode jmmNode) {
        String methodReturn = jmmNode.get("text");
        Boolean isArray = Boolean.parseBoolean(jmmNode.get("isArray"));

        return new Type(methodReturn, isArray);
    }

}