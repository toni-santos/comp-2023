package pt.up.fe.comp2023.ollir.optimize;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.Arrays;
import java.util.List;

public class ConstantFolding extends AJmmVisitor<String, String> {

    private List<String> kindList = Arrays.asList("IntValue", "BooleanValue");
    private boolean changed;

    @Override
    protected void buildVisitor() {
        setDefaultVisit(this::dealNext);

        addVisit("Program", this::dealWithProgram);

        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("UnaryOp", this::dealWithUnaryOp);

        addVisit("IntValue", this::dealWithValue);
        addVisit("BooleanValue", this::dealWithValue);
    }

    private String dealWithProgram(JmmNode jmmNode, String s) {
        this.changed = false;
        for (JmmNode child : jmmNode.getChildren()) {
            visit(child);
        }
        return "";
    }

    private void updateValue(JmmNode oldNode, String value, String kind) {
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
        this.changed = true;
    }

    private String dealWithUnaryOp(JmmNode jmmNode, String s) {
        JmmNode child = jmmNode.getJmmChild(0);

        if (child.getKind().equals("BooleanValue")) {
            String childVal = visit(child);

            String newVal = childVal.equals("true") ? "false" : "true";
            updateValue(jmmNode, newVal, "BooleanValue");
        }

        return "";
    }

    private String dealWithBinaryOp(JmmNode jmmNode, String s) {
        JmmNode left = jmmNode.getJmmChild(0);
        JmmNode right = jmmNode.getJmmChild(1);
        String retLeft = visit(left, "fromOp");
        String retRight = visit(right, "fromOp");

        if ( (!isInteger(retLeft) && !isInteger(retRight)) && (!isBoolean(retLeft) && !isBoolean(retRight)) ) {
            return "";
        }

        Integer newIntegerValue = null;
        String newBooleanValue  = null;
        String newKind = "";

        switch (jmmNode.get("op")) {
            case "*" -> {
                Integer valLeft = Integer.valueOf(retLeft);
                Integer valRight = Integer.valueOf(retRight);
                newIntegerValue = valLeft * valRight;
                newKind = "IntValue";
            }
            case "/" -> {
                Integer valLeft = Integer.valueOf(retLeft);
                Integer valRight = Integer.valueOf(retRight);
                newIntegerValue = valLeft / valRight;
                newKind = "IntValue";

            }
            case "+" -> {
                Integer valLeft = Integer.valueOf(retLeft);
                Integer valRight = Integer.valueOf(retRight);
                newIntegerValue = valLeft + valRight;
                newKind = "IntValue";

            }
            case "-" -> {
                Integer valLeft = Integer.valueOf(retLeft);
                Integer valRight = Integer.valueOf(retRight);
                newIntegerValue = valLeft - valRight;
                newKind = "IntValue";

            }
            case "<" -> {
                Integer valLeft = Integer.getInteger(retLeft);
                Integer valRight = Integer.getInteger(retRight);
                newBooleanValue = valLeft < valRight ? "true" : "false";
                newKind = "BooleanValue";

            }
            case "&&" -> {
                boolean valLeft = Boolean.getBoolean(retLeft);
                boolean valRight = Boolean.getBoolean(retRight);
                newBooleanValue = valLeft && valRight ? "true" : "false";
                newKind = "BooleanValue";

            }
        }

        if (newIntegerValue != null) {
            updateValue(jmmNode, String.valueOf(newIntegerValue), newKind);
            if (s != null && s.equals("fromOp")) {
                return String.valueOf(newIntegerValue);
            }
        } else if (newBooleanValue != null) {
            updateValue(jmmNode, newBooleanValue, newKind);
            if (s != null && s.equals("fromOp")) {
                return newBooleanValue;
            }
        }

        return "";
    }

    private boolean isBoolean(String str) {
        return str.matches("true|false");
    }

    private boolean isInteger(String str) {
        return str.matches("[0]|[1-9][0-9]*");
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

    public boolean isChanged() {
        return changed;
    }
}
