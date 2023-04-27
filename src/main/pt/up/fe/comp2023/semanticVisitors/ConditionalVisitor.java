package pt.up.fe.comp2023.semanticVisitors;

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

public class ConditionalVisitor extends AJmmVisitor<Object, Boolean> {

    List<Report> reports = new ArrayList<>();
    SimpleSymbolTable symbolTable;

    public ConditionalVisitor(SimpleSymbolTable symbolTable){
        super();
        this.symbolTable = symbolTable;
    }


    @Override
    protected void buildVisitor() {

        setDefaultVisit(this::dealNext);

        addVisit("IfElse", this::dealWithIfElse);
        addVisit("While", this::dealWithWhile);
    }

    private Boolean dealWithIfElse(JmmNode jmmNode, Object dummy) {

        Type type = new Type("", false);

        switch(jmmNode.getJmmChild(0).getKind()) {
            case "ArrayStatement":
            case "DeclarationStatement":
                AssignmentVisitor assignmentVisitor = new AssignmentVisitor(symbolTable);
                type = assignmentVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "This":
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
                OperationTypeVisitor opVisitor = new OperationTypeVisitor(symbolTable);
                type = opVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "Length":
            case "MethodCall":
                MethodVisitor methodVisitor = new MethodVisitor(symbolTable);
                type = methodVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "IntValue":
            case "NewObject":
            case "NewArray":
            case "BooleanValue":
            case "Identifier":
                VariableVisitor variableVisitor = new VariableVisitor(symbolTable);
                type = variableVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            default:
                this.visit(jmmNode.getJmmChild(0));
                break;
        }
        if (!type.getName().equals("boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "If Statement: condition must be a boolean"));
        }
        return true;
    }

    private Boolean dealWithWhile(JmmNode jmmNode, Object dummy) {

        Type type = new Type("", false);

        switch(jmmNode.getJmmChild(0).getKind()) {
            case "ArrayStatement":
            case "DeclarationStatement":
                AssignmentVisitor assignmentVisitor = new AssignmentVisitor(symbolTable);
                type = assignmentVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "This":
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
                OperationTypeVisitor opVisitor = new OperationTypeVisitor(symbolTable);
                type = opVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "LengthMethod":
            case "MethodCall":
                MethodVisitor methodVisitor = new MethodVisitor(symbolTable);
                type = methodVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "IntValue":
            case "NewObject":
            case "NewArray":
            case "BooleanValue":
            case "Identifier":
                VariableVisitor variableVisitor = new VariableVisitor(symbolTable);
                type = variableVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            default:
                this.visit(jmmNode.getJmmChild(0));
                break;
        }
        if (!type.getName().equals("boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "While Statement: condition must be a boolean"));
        }

        return true;
    }




    private Boolean dealNext(JmmNode jmmNode, Object dummy) {
        for (JmmNode child : jmmNode.getChildren()) {
            this.visit(child, 0);
        }
        return true;
    }

    public List<Report> getReports() {
        return this.reports;
    }
}