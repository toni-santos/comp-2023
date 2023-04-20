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
            case "MethodCall":
                // visit and get type
                break;
            case "BooleanValue":
            case "Identifier":
                VariableVisitor variableVisitor = new VariableVisitor(symbolTable);
                type = variableVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            default:
                this.visit(jmmNode.getJmmChild(0));
                break;
        }
        //int line = Integer.valueOf(jmmNode.getJmmChild(0).get("line"));
        //int col = Integer.valueOf(jmmNode.getJmmChild(0).get("col"));
        if (!type.getName().equals("boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "If Statement: condition must be a boolean"));
        }
        return true;
    }

    private Boolean dealWithWhile(JmmNode jmmNode, Object dummy) {

        Type type = new Type("", false);

        switch(jmmNode.getJmmChild(0).getKind()) {
            case "Parenthesis":
                ExpressionVisitor expressionVisitor = new ExpressionVisitor(symbolTable);
                type = expressionVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "ArraySubscript":
                ArrayVisitor arrayVisitor = new ArrayVisitor(symbolTable);
                type = arrayVisitor.visit(jmmNode.getJmmChild(1), 0);
                break;
            case "BinaryOp":
            case "UnaryOp":
                OperationTypeVisitor opVisitor = new OperationTypeVisitor(symbolTable);
                type = opVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "MethodCall":
                // visit and get type
                break;
            case "BooleanValue":
            case "Identifier":
                VariableVisitor variableVisitor = new VariableVisitor(symbolTable);
                type = variableVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            default:
                this.visit(jmmNode.getJmmChild(0));
                break;
        }
        //int line = Integer.valueOf(jmmNode.getJmmChild(0).get("line"));
        //int col = Integer.valueOf(jmmNode.getJmmChild(0).get("col"));
        if (!type.getName().equals("boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "While Statement: condition must be a boolean"));
        }

        return true;
    }




    private Boolean dealNext(JmmNode jmmNode, Object dummy) {
        for (JmmNode child : jmmNode.getChildren()) {
            visit(child);
        }
        return true;
    }

    public List<Report> getReports() {
        return this.reports;
    }
}