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

public class ExpressionVisitor extends AJmmVisitor<Object, Type> {

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
        addVisit("Parenthesis", this::dealWithParenthesis);
    }

    private Type dealWithThisExpression(JmmNode jmmNode, Object dummy) {
        JmmNode parentNode = jmmNode.getJmmParent();
        //Integer line = Integer.valueOf(jmmNode.get("line"));
        //Integer col = Integer.valueOf(jmmNode.get("col"));
        while(!parentNode.getKind().equals("methodDeclaration") && !parentNode.getKind().equals("importDeclaration")) {
            if (parentNode.getJmmParent() == null) break;
            parentNode = parentNode.getJmmParent();
        }
        if (parentNode.getKind().equals("methodDeclaration")) {
            if (parentNode.get("methodName").equals("main")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, 0, "THIS keyword cannot be used in static method"));
            }
        }
        return new Type(this.symbolTable.getClassName(), false);
    }

    private Type dealWithParenthesis(JmmNode jmmNode, Object dummy){
        JmmNode expr = jmmNode.getJmmChild(0);
        while (expr.getKind().equals("Parenthesis")){
            expr = expr.getJmmChild(0);
        }
        switch(expr.getKind()) {
            case "DeclarationStatement":
                AssignmentVisitor assignmentVisitor = new AssignmentVisitor(symbolTable);
                return assignmentVisitor.visit(expr, 0);
            case "ArraySubscript":
                ArrayVisitor arrayVisitor = new ArrayVisitor(symbolTable);
                return arrayVisitor.visit(expr, 0);
            case "IntValue":
            case "BooleanValue":
            case "Identifier":
            case "NewObject":
            case "NewArray":
                VariableVisitor variableVisitor = new VariableVisitor(symbolTable);
                return variableVisitor.visit(expr, 0);
            case "Length":
            case "MethodCall":{
                MethodVisitor methodVisitor = new MethodVisitor(symbolTable);
                return methodVisitor.visit(expr, 0);
            }
            case "UnaryOp":
            case "BinaryOp":
                OperationTypeVisitor opVisitor = new OperationTypeVisitor(symbolTable);
                return opVisitor.visit(expr, 0);
        }
        Type type = this.visit(expr, dummy);
        return type;
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