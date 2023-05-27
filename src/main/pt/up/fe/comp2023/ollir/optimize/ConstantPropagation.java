package pt.up.fe.comp2023.ollir.optimize;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.*;
import java.util.stream.Stream;

public class ConstantPropagation extends AJmmVisitor<String, List<String>> {
    private Map<String, String> varNameValueMap = new HashMap();
    private boolean changed;
    private boolean processWhile;

    @Override
    protected void buildVisitor() {
        setDefaultVisit(this::dealNext);

        addVisit("Program", this::dealWithProgram);
        
        addVisit("MethodDeclaration", this::dealWithMethodDeclaration);

        addVisit("DeclarationStatement", this::dealWithDeclarationStatement);

        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("UnaryOp", this::dealWithUnaryOp);
        addVisit("ReturnStatement", this::dealWithUnaryOp);
        addVisit("MethodCall", this::dealWithMethodCall);
        addVisit("IfElse", this::dealWithIfElse);
        addVisit("While", this::dealWithWhile);
        addVisit("Brackets", this::dealWithBrackets);

        addVisit("IntValue", this::dealWithValue);
        addVisit("BooleanValue", this::dealWithValue);
        addVisit("Identifier", this::dealWithValue);
        addVisit("This", this::dealWithValue);
    }

    private void updateValue(JmmNode oldNode, String value, String kind) {
        if (value.equals("this")) {
            kind = "This";
        } else if (value.equals("true") || value.equals("false")) {
            kind = "BooleanValue";
        } else if (value.matches("[0]|[1-9][0-9]*")) {
            kind = "IntValue";
        } else {
            kind = "Identifier";
        }

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

    private void resetScope() {
        this.varNameValueMap.clear();
    }

    private List<String> dealWithProgram(JmmNode jmmNode, String s) {
        this.changed = false;
        for (JmmNode child : jmmNode.getChildren()) {
            visit(child);
        }
        resetScope();
        return new ArrayList<>();
    }

    private List<String> dealWithBrackets(JmmNode jmmNode, String s) {
        List<String> varList = new ArrayList<>();
        Map<String, String> preBracketMap = this.varNameValueMap;

        // Visit brackets content
        for (int i = 0; i < jmmNode.getNumChildren(); i++) {
            JmmNode statement = jmmNode.getJmmChild(i);
            List<String> visitRet = visit(statement);
            if (statement.getKind().equals("DeclarationStatement") && !varList.contains(visitRet.get(0))) {
                varList.add(visitRet.get(0));
            } else if (statement.getKind().equals("IfElse") || statement.getKind().equals("While")) {
                // visitRet -> variables changed inside if/while
                // add them to the list of changed variables
                varList = mergeLists(varList, visitRet);
            }
        }

        // Return map to previous scope
        if (!jmmNode.getJmmParent().getKind().equals("IfElse"))
            this.varNameValueMap = preBracketMap;

        return varList;
    }

    private List<String> dealWithMethodDeclaration(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren()) {
            visit(child);
        }
        resetScope();
        return new ArrayList<>();
    }

    private List<String> dealWithWhile(JmmNode jmmNode, String s) {
        JmmNode cond = jmmNode.getJmmChild(0);
        // Save original map
        Map<String, String> preStatementMap = new HashMap<>(varNameValueMap);

        // Find variables used in the condition
        List<String> condVars = processWhileCondition(cond);
        // Visit the loop without any knowledge
        resetScope();
        JmmNode statementNode = jmmNode.getJmmChild(1);
        List<String> statementVars = visit(statementNode);

        // Create a disjoint set of the vars used in the condition and inside the loop
        Set<String> disjointVars = new HashSet<>();

        for (String var : condVars)
            if (!statementVars.contains(var)) {
                disjointVars.add(var);
            }


        // Create a new map only with values that may be changed
        Map<String, String> changeableVars = new HashMap<>();

        for (String var : preStatementMap.keySet()) {
            if (disjointVars.contains(var))
                changeableVars.put(var, preStatementMap.get(var));

        }
        // Visit and propagate the condition
        this.varNameValueMap = changeableVars;
        visit(cond);

        // Reset the map to before the while withtout the variables that were (maybe) modified inside the loop
        for (String var : preStatementMap.keySet())
            if (statementVars.contains(var))
                preStatementMap.remove(var);

        this.varNameValueMap = preStatementMap;

        return changeableVars.keySet().stream().toList();
    }

    private List<String> processWhileCondition(JmmNode node) {
        switch (node.getKind()) {
            case "BinaryOp" -> {
                JmmNode left = node.getJmmChild(0);
                JmmNode right = node.getJmmChild(1);

                List<String> leftVal = processWhileCondition(left);
                List<String> rightVal = processWhileCondition(right);

                return mergeLists(leftVal, rightVal);
            }
            case "Identifier" -> {
                return Arrays.asList(node.get("value"));
            }
            default -> {
                if (node.getChildren().size() > 0)
                    return processWhileCondition(node.getJmmChild(0));
                else
                    return new ArrayList<>();
            }
        }
    }

    private List<String> dealWithIfElse(JmmNode jmmNode, String s) {
        // If condition
        JmmNode child = jmmNode.getJmmChild(0);

        List<String> retChild = visit(child);

        if (!retChild.isEmpty() && varNameValueMap.containsKey(retChild.get(0))) {
            String kind = child.getKind();
            String value = varNameValueMap.get(retChild.get(0));
            updateValue(child, value, kind);
        }
        Map<String, String> preIfMap = new HashMap<>(this.varNameValueMap);

        // Then
        JmmNode thenNode = jmmNode.getJmmChild(1);
        List<String> thenVars = visit(thenNode);

        // Else
        JmmNode elseNode = jmmNode.getJmmChild(2);
        List<String> elseVars = visit(elseNode);

        // Returning map to stable state
        for (String varName : preIfMap.keySet()) {
            if (thenVars.contains(varName) || elseVars.contains(varName))
                preIfMap.remove(varName);
        }

        // Update the variables map so that only the unaltered ones remain
        this.varNameValueMap = preIfMap;

        return mergeLists(thenVars, elseVars);
    }

    private List<String> dealWithMethodCall(JmmNode jmmNode, String s) {
        if (jmmNode.getNumChildren() >= 2) {
            for (int i = 1; i < jmmNode.getNumChildren(); i++) {
                JmmNode child = jmmNode.getJmmChild(i);
                List<String> retChild = visit(child);
                if (!retChild.isEmpty() && varNameValueMap.containsKey(retChild.get(0))) {
                    String kind = child.getKind();
                    String value = varNameValueMap.get(retChild.get(0));
                    updateValue(child, value, kind);
                }
            }
        }

        return new ArrayList<>();
    }

    private List<String> dealWithUnaryOp(JmmNode jmmNode, String s) {
        JmmNode child = jmmNode.getJmmChild(0);

        List<String> retChild = visit(child);

        if (!retChild.isEmpty() && varNameValueMap.containsKey(retChild.get(0))) {
            String kind = child.getKind();
            String value = varNameValueMap.get(retChild.get(0));
            updateValue(child, value, kind);
        }

        return new ArrayList<>();
    }

    private List<String> dealWithBinaryOp(JmmNode jmmNode, String s) {
        JmmNode left = jmmNode.getJmmChild(0);
        JmmNode right = jmmNode.getJmmChild(1);

        List<String> retLeft = visit(left);
        List<String> retRight = visit(right);

        if (!retLeft.isEmpty() && varNameValueMap.containsKey(retLeft.get(0))) {
            String kind = left.getKind();
            String value = varNameValueMap.get(retLeft.get(0));
            updateValue(left, value, kind);
        }

        if (!retRight.isEmpty() && varNameValueMap.containsKey(retRight.get(0))) {
            String kind = right.getKind();
            String value = varNameValueMap.get(retRight.get(0));
            updateValue(right, value, kind);
        }

        return new ArrayList<>();
    }

    private List<String> dealWithDeclarationStatement(JmmNode jmmNode, String s) {
        JmmNode childVar = jmmNode.getJmmChild(0);
        JmmNode childVal = jmmNode.getJmmChild(1);

        if (childVar.hasAttribute("array")) {
            return new ArrayList<>();
        }

        String varName = childVar.get("value");
        List<String> varValue = visit(childVal);

        if (!varValue.isEmpty())
            varNameValueMap.put(varName, varValue.get(0));

        return Arrays.asList(varName);
    }

    private List<String> dealWithValue(JmmNode jmmNode, String s) {
        return Arrays.asList(jmmNode.get("value"));
    }

    private List<String> dealNext(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren()) {
            visit(child);
        }
        return new ArrayList<>();
    }

    public boolean isChanged() {
        return changed;
    }

    private List<String> mergeLists(List<String> l1, List<String> l2) {
        List<String> combinedList = new ArrayList<>();
        combinedList.addAll(l1);
        combinedList.addAll(l2);

        Set<String> combinedSet = new LinkedHashSet<>(combinedList);
        List<String> result = new ArrayList<>(combinedSet);

        return result;
    }

    public void setProcessWhile(boolean processWhile) {
        this.processWhile = processWhile;
    }
}
