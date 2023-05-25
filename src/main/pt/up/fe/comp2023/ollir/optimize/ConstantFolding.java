package pt.up.fe.comp2023.ollir.optimize;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.Arrays;
import java.util.List;

public class ConstantFolding extends AJmmVisitor<String, String> {

    private List<String> kindList = Arrays.asList("#IntValue", "#BooleanValue");
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

        if (child.getKind().equals("#BooleanValue")) {
            String childVal = visit(child);

            String newVal = childVal.equals("true") ? "false" : "true";
            updateValue(jmmNode, newVal, child.getKind());
        }

        return "";
    }

    private String dealWithBinaryOp(JmmNode jmmNode, String s) {
        JmmNode left = jmmNode.getJmmChild(0);
        JmmNode right = jmmNode.getJmmChild(1);

        if (!(kindList.contains(left.getKind()) && kindList.contains(right.getKind())) && (right.getKind().equals(left.getKind()))) {
            return "";
        }

        String retLeft = visit(left);
        String retRight = visit(right);
        Integer newIntegerValue = null;
        String newBooleanValue  = null;

        if (!retLeft.matches("[0]|[1-9][0-9]*") || !retRight.matches("[0]|[1-9][0-9]*")) {
            return "";
        }

        switch (jmmNode.get("op")) {
            case "*" -> {
                Integer valLeft = Integer.getInteger(retLeft);
                Integer valRight = Integer.getInteger(retRight);
                newIntegerValue = valLeft * valRight;
            }
            case "/" -> {
                Integer valLeft = Integer.valueOf(retLeft);
                Integer valRight = Integer.valueOf(retRight);
                newIntegerValue = valLeft / valRight;
            }
            case "+" -> {
                Integer valLeft = Integer.valueOf(retLeft);
                Integer valRight = Integer.valueOf(retRight);
                newIntegerValue = valLeft + valRight;
            }
            case "-" -> {
                Integer valLeft = Integer.getInteger(retLeft);
                Integer valRight = Integer.getInteger(retRight);
                newIntegerValue = valLeft - valRight;
            }
            case "<" -> {
                Integer valLeft = Integer.getInteger(retLeft);
                Integer valRight = Integer.getInteger(retRight);
                newBooleanValue = valLeft < valRight ? "true" : "false";
            }
            case "&&" -> {
                boolean valLeft = Boolean.getBoolean(retLeft);
                boolean valRight = Boolean.getBoolean(retRight);
                newBooleanValue = valLeft && valRight ? "true" : "false";
            }
        }

        if (newIntegerValue != null) {
            updateValue(jmmNode, String.valueOf(newIntegerValue), left.getKind());
        } else if (newBooleanValue != null) {
            updateValue(jmmNode, newBooleanValue, left.getKind());
        }

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

    public boolean isChanged() {
        return changed;
    }
}
