package pt.up.fe.comp2023.semanticVisitors;

import jas.Var;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import pt.up.fe.comp2023.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OperationTypeVisitor extends AJmmVisitor<Object, Type> {

    List<Report> reports = new ArrayList<>();
    SimpleSymbolTable symbolTable;

    public OperationTypeVisitor(SimpleSymbolTable symbolTable){
        super();
        this.symbolTable = symbolTable;
    }


    @Override
    protected void buildVisitor() {

        setDefaultVisit(this::dealNext);

        addVisit("BinaryOp", this::dealWithThisBinOp);
        addVisit("UnaryOp", this::dealWithThisUnaryOp);
    }

    private Type dealWithThisBinOp(JmmNode jmmNode, Object dummy) {
        String op = jmmNode.get("op");
        Type lhs = new Type("", false);
        Type rhs = new Type("", false);

        switch(jmmNode.getJmmChild(0).getKind()) {
            case "DeclarationStatement":
                AssignmentVisitor assignmentVisitor = new AssignmentVisitor(symbolTable);
                lhs = assignmentVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "This":
            case "Parenthesis":
                ExpressionVisitor expressionVisitor = new ExpressionVisitor(symbolTable);
                lhs = expressionVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "ArraySubscript":
                ArrayVisitor arrayVisitor = new ArrayVisitor(symbolTable);
                lhs = arrayVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "IntValue":
            case "BooleanValue":
            case "Identifier":
                VariableVisitor variableVisitor = new VariableVisitor(symbolTable);
                lhs = variableVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "LengthMethod":
            case "MethodCall":
                MethodVisitor methodVisitor = new MethodVisitor(symbolTable);
                lhs = methodVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "BinaryOp":
            case "UnaryOp":
                lhs = this.visit(jmmNode.getJmmChild(0));
                break;
        }

        switch(jmmNode.getJmmChild(1).getKind()) {
            case "DeclarationStatement":
                AssignmentVisitor assignmentVisitor = new AssignmentVisitor(symbolTable);
                rhs = assignmentVisitor.visit(jmmNode.getJmmChild(1), 0);
                break;
            case "This":
            case "Parenthesis":
                ExpressionVisitor expressionVisitor = new ExpressionVisitor(symbolTable);
                rhs = expressionVisitor.visit(jmmNode.getJmmChild(1), 0);
                break;
            case "ArraySubscript":
                ArrayVisitor arrayVisitor = new ArrayVisitor(symbolTable);
                rhs = arrayVisitor.visit(jmmNode.getJmmChild(1), 0);
                break;
            case "IntValue":
            case "BooleanValue":
            case "NewObject":
            case "NewArray":
            case "Identifier":
                VariableVisitor variableVisitor = new VariableVisitor(symbolTable);
                rhs = variableVisitor.visit(jmmNode.getJmmChild(1), 0);
                break;
            case "Length":
            case "MethodCall":
                MethodVisitor methodVisitor = new MethodVisitor(symbolTable);
                rhs = methodVisitor.visit(jmmNode.getJmmChild(1), 0);
                break;
            case "BinaryOp":
            case "UnaryOp":
                rhs = this.visit(jmmNode.getJmmChild(1));
                break;
        }
        /*
        int lineLeft = Integer.valueOf(jmmNode.getJmmChild(0).get("line"));
        int colLeft = Integer.valueOf(jmmNode.getJmmChild(0).get("col"));
        int lineRight = Integer.valueOf(jmmNode.getJmmChild(1).get("line"));
        int colRight = Integer.valueOf(jmmNode.getJmmChild(1).get("col"));
         */

        int lineLeft = 0;
        int colLeft = 0;
        int lineRight = 0;
        int colRight = 0;

        if(!lhs.getName().equals(rhs.getName())){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, lineRight, colRight, "Error in operation " + op + " : operands have different types"));
        }
        else if( ( lhs.isArray() || rhs.isArray() ) && ( op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("<") ) ) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, lineLeft, colLeft, "Error in operation " + op + " : array cannot be used in this operation"));
        }
        else if(!lhs.getName().equals("int") && ( op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("<") ) ) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, lineLeft, colLeft, "Error in operation " + op + " : operands have invalid types for this operation. " + op + " expects operands of type integer"));
        }
        else if(!lhs.getName().equals("boolean") && ( op.equals("&&") )) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, lineLeft, lineLeft, "Error in operation "+ op + " : operands have invalid types for this operation. " + op + " expects operands of type boolean"));
        }
        else {
            switch(op) {
                case "<":
                case "&&":
                    return new Type("boolean", false);
                case "+":
                case "-":
                case "/":
                case "*":
                    return new Type("int", false);
                default:
                    return lhs;
            }
        }
        return lhs;
    }

    private Type dealWithThisUnaryOp(JmmNode jmmNode, Object dummy) {
        Type type = new Type("", false);

        switch(jmmNode.getJmmChild(0).getKind()) {
            case "DeclarationStatement":
                AssignmentVisitor assignmentVisitor = new AssignmentVisitor(symbolTable);
                type = assignmentVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "Parenthesis":
                ExpressionVisitor expressionVisitor = new ExpressionVisitor(symbolTable);
                type = expressionVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "ArraySubscript":
                ArrayVisitor arrayVisitor = new ArrayVisitor(symbolTable);
                type = arrayVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "BinaryOp":
            case "UnaryOp":
                type = this.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "Length":
            case "MethodCall":
                MethodVisitor methodVisitor = new MethodVisitor(symbolTable);
                type = methodVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "IntValue":
            case "BooleanValue":
            case "NewObject":
            case "NewArray":
            case "Identifier":
                VariableVisitor variableVisitor = new VariableVisitor(symbolTable);
                type = variableVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            default:
                type = this.visit(jmmNode.getJmmChild(0));
                break;
        }
        //int line = Integer.valueOf(jmmNode.getJmmChild(1).get("line"));
        //int col = Integer.valueOf(jmmNode.getJmmChild(1).get("col"));
        if (!type.getName().equals("boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "This operation is only applicable to boolean values"));
        }
        return new Type("boolean", false);
    }




    private Type dealNext(JmmNode jmmNode, Object dummy) {
        for (JmmNode child : jmmNode.getChildren()) {
            this.visit(child, 0);
        }
        return new Type("", false);
    }

    public List<Report> getReports() {
        return this.reports;
    }
}