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

        return new Type("", false);
    }

    private Type dealWithThisUnaryOp(JmmNode jmmNode, Object dummy) {
        Type type = new Type("", false);

        switch(jmmNode.getJmmChild(1).getKind()) {
            case "Parenthesis":
                ExpressionVisitor expressionVisitor = new ExpressionVisitor(symbolTable);
                type = expressionVisitor.visit(jmmNode.getJmmChild(1), 0);
                break;
            case "BinaryOp":
                // visit and get type
                break;
            case "UnaryOp":
                type = this.visit(jmmNode.getJmmChild(1), 0);
                break;
            case "MethodCall":
                // visit and get type
                break;
            case "BooleanValue":
                return new Type("boolean", false);
        }
        int line = Integer.valueOf(jmmNode.getJmmChild(1).get("line"));
        int col = Integer.valueOf(jmmNode.getJmmChild(1).get("col"));
        if (!type.getName().equals("boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "This operation is only applicable to boolean values"));
        }
        return new Type("", false);
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