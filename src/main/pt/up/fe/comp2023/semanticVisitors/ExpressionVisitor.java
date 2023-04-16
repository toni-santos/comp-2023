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

public class ExpressionVisitor extends AJmmVisitor<Object, Boolean> {

    List<Report> reports = new ArrayList<>();
    SimpleSymbolTable symbolTable;

    public ExpressionVisitor(SimpleSymbolTable symbolTable){
        super();
        this.symbolTable = symbolTable;
    }


    @Override
    protected void buildVisitor() {

        setDefaultVisit(this::dealNext);

        addVisit("This", this::dealWithThisExpression);
    }

    private Boolean dealWithThisExpression(JmmNode jmmNode, Object dummy) {
        JmmNode parentNode = jmmNode.getJmmParent();
        Integer line = Integer.valueOf(jmmNode.get("line"));
        Integer col = Integer.valueOf(jmmNode.get("col"));
        while(!parentNode.getKind().equals("methodDeclaration") && !parentNode.getKind().equals("importDeclaration")) {
            parentNode = parentNode.getJmmParent();
        }
        if (parentNode.getKind().equals("methodDeclaration")) {
            if (parentNode.get("methodName").equals("main")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "'this' keyword cannot be used in static method"));
            }
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