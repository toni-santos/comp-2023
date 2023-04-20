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

public class ArrayVisitor extends AJmmVisitor<Object, Type> {

    List<Report> reports = new ArrayList<>();
    SimpleSymbolTable symbolTable;

    public ArrayVisitor(SimpleSymbolTable symbolTable){
        super();
        this.symbolTable = symbolTable;
    }

    @Override
    protected void buildVisitor() {

        setDefaultVisit(this::dealNext);

        addVisit("ArraySubscript", this::dealWithArray);
    }

    private Type dealWithArray(JmmNode jmmNode, Object dummy) {

        Type type = new Type("", false);
        Type type1 = new Type("", false);

        // check if array access is done over array

        switch(jmmNode.getJmmChild(0).getKind()) {
            case "Parenthesis":
                ExpressionVisitor expressionVisitor = new ExpressionVisitor(symbolTable);
                type1 = expressionVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "IntValue":
            case "BooleanValue":
            case "Identifier":
                VariableVisitor variableVisitor = new VariableVisitor(symbolTable);
                type1 = variableVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "LengthMethod":
            case "MethodCall":
                MethodVisitor methodVisitor = new MethodVisitor(symbolTable);
                type1 = methodVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "BinaryOp":
            case "UnaryOp":
                OperationTypeVisitor opVisitor = new OperationTypeVisitor(symbolTable);
                type1 = opVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "ArraySubscript":
                type1 = this.visit(jmmNode.getJmmChild(0), 0);
                break;
        }

        if (!type1.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Array access on invalid type"));
        }



        // check if index is integer

        switch(jmmNode.getJmmChild(1).getKind()) {
            case "Parenthesis":
                ExpressionVisitor expressionVisitor = new ExpressionVisitor(symbolTable);
                type = expressionVisitor.visit(jmmNode.getJmmChild(1), 0);
                break;
            case "IntValue":
            case "BooleanValue":
            case "Identifier":
                VariableVisitor variableVisitor = new VariableVisitor(symbolTable);
                type = variableVisitor.visit(jmmNode.getJmmChild(1), 0);
                break;
            case "LengthMethod":
            case "MethodCall":
                MethodVisitor methodVisitor = new MethodVisitor(symbolTable);
                type = methodVisitor.visit(jmmNode.getJmmChild(1), 0);
                break;
            case "BinaryOp":
            case "UnaryOp":
                OperationTypeVisitor opVisitor = new OperationTypeVisitor(symbolTable);
                type = opVisitor.visit(jmmNode.getJmmChild(1), 0);
                break;
            case "ArraySubscript":
                type = this.visit(jmmNode.getJmmChild(1), 0);
                break;
        }

        if (!type.getName().equals("int")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "Array index must be an integer"));
        }


        return new Type("int", false);
    }




    private Type dealNext(JmmNode jmmNode, Object dummy) {
        for (JmmNode child : jmmNode.getChildren()) {
            visit(child);
        }
        return new Type("", false);
    }

    public List<Report> getReports() {
        return this.reports;
    }
}
