package pt.up.fe.comp2023.ollir.optimize;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.*;

public class ConstantPropagation extends AJmmVisitor<String, String> {
    private Map<String, String> varNameValueMap = new HashMap();

    @Override
    protected void buildVisitor() {
        setDefaultVisit(this::dealNext);

        addVisit("DeclarationStatement", this::dealWithDeclarationStatement);
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("UnaryOp", this::dealWithUnaryOp);
        addVisit("ReturnStatement", this::dealWithUnaryOp);
        addVisit("MethodCall", this::dealWithMethodCall);
        addVisit("IfElse", this::dealWithIfElse);
        addVisit("While", this::dealWithWhile);

        addVisit("IntValue", this::dealWithValue);
        addVisit("BooleanValue", this::dealWithValue);
        addVisit("Identifier", this::dealWithValue);
        addVisit("This", this::dealWithValue);
    }

    private static void updateValue(JmmNode oldNode, String value, String kind) {
        // create new node
        JmmNode newNode = new JmmNodeImpl(kind);
        newNode.put("value", value);

        // replace old node
        JmmNode parent = oldNode.getJmmParent();
        if (parent == null) return;
        int idx = parent.getChildren().indexOf(oldNode);
        parent.removeJmmChild(oldNode);
        parent.add(newNode, idx);
        newNode.setParent(parent);
    }

    private String dealWithWhile(JmmNode jmmNode, String s) {
        JmmNode child = jmmNode.getJmmChild(0);

        String retChild = visit(child);

        if (varNameValueMap.containsKey(retChild)) {
            String kind = child.getKind();
            String value = retChild;
            updateValue(jmmNode, value, kind);
        }

        return "";
    }

    private String dealWithIfElse(JmmNode jmmNode, String s) {
        JmmNode child = jmmNode.getJmmChild(0);

        String retChild = visit(child);

        if (varNameValueMap.containsKey(retChild)) {
            String kind = child.getKind();
            String value = retChild;
            updateValue(jmmNode, value, kind);
        }

        return "";
    }

    private String dealWithMethodCall(JmmNode jmmNode, String s) {
        if (jmmNode.getNumChildren() >= 2) {
            for (int i = 1; i < jmmNode.getNumChildren(); i++) {
                JmmNode child = jmmNode.getJmmChild(i);
                String retChild = visit(child);
                if (varNameValueMap.containsKey(retChild)) {
                    String kind = child.getKind();
                    String value = retChild;
                    updateValue(jmmNode, value, kind);
                }
            }
        }

        return "";
    }

    private String dealWithUnaryOp(JmmNode jmmNode, String s) {
        JmmNode child = jmmNode.getJmmChild(0);

        String retChild = visit(child);

        if (varNameValueMap.containsKey(retChild)) {
            String kind = child.getKind();
            String value = retChild;
            updateValue(jmmNode, value, kind);
        }

        return "";
    }

    private String dealWithBinaryOp(JmmNode jmmNode, String s) {
        JmmNode left = jmmNode.getJmmChild(0);
        JmmNode right = jmmNode.getJmmChild(1);

        String retLeft = visit(left);
        String retRight = visit(right);

        if (varNameValueMap.containsKey(retLeft)) {
            String kind = left.getKind();
            String value = retLeft;
            updateValue(jmmNode, value, kind);
        }

        if (varNameValueMap.containsKey(retRight)) {
            String kind = right.getKind();
            String value = retRight;
            updateValue(jmmNode, value, kind);
        }

        return "";
    }

    private String dealWithDeclarationStatement(JmmNode jmmNode, String s) {
        JmmNode childVar = jmmNode.getJmmChild(0);
        JmmNode childVal = jmmNode.getJmmChild(1);

        if (childVar.hasAttribute("array")) {
            return "";
        }

        String varName = childVar.get("value");
        String varValue = visit(childVal);

        if (!varValue.equals(""))
            varNameValueMap.put(varName, varValue);

        return "";
    }

    private String dealWithValue(JmmNode jmmNode, String s) {
        return jmmNode.get("value");
    }

    private String dealNext(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren()) {
            visit(child);
        }
        return "";
    }

}
