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

public class AssignmentVisitor extends AJmmVisitor<Object, Type> {

    List<Report> reports = new ArrayList<>();
    SimpleSymbolTable symbolTable;

    public AssignmentVisitor(SimpleSymbolTable symbolTable){
        super();
        this.symbolTable = symbolTable;
    }

    @Override
    protected void buildVisitor() {

        setDefaultVisit(this::dealNext);

        addVisit("DeclarationStatement", this::dealWithAssignment);
    }

    private Type dealWithAssignment(JmmNode jmmNode, Object dummy) {

        Type lhs = new Type("", false);
        Type rhs = new Type("", false);

        IdentifierDeclarationVisitor idVisitor = new IdentifierDeclarationVisitor(symbolTable);
        lhs = idVisitor.visit(jmmNode.getJmmChild(0), 0);

        switch(jmmNode.getJmmChild(1).getKind()) {
            case "This":
            case "Parenthesis":
                ExpressionVisitor expressionVisitor = new ExpressionVisitor(symbolTable);
                rhs = expressionVisitor.visit(jmmNode.getJmmChild(1), 0);
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
                OperationTypeVisitor opVisitor = new OperationTypeVisitor(symbolTable);
                rhs = opVisitor.visit(jmmNode.getJmmChild(1), 0);
                break;
            case "ArraySubscript":
                ArrayVisitor arrayVisitor = new ArrayVisitor(symbolTable);
                rhs = arrayVisitor.visit(jmmNode.getJmmChild(1), 0);
                break;
            default:
                rhs = this.visit(jmmNode.getJmmChild(1));
                break;
        }
        //int line = Integer.valueOf(jmmNode.getJmmChild(0).get("line"));
        //int col = Integer.valueOf(jmmNode.getJmmChild(0).get("col"));
        int line = 0;
        int col = 0;

        if (lhs.isArray() && !rhs.isArray() && !jmmNode.getJmmChild(1).getKind().equals("NewArray"))
        {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Arrays can only be assigned array values"));
        }

        if(!lhs.getName().equals(rhs.getName()) &&
                ( !symbolTable.getImports().contains(lhs.getName()) || !symbolTable.getImports().contains(rhs.getName())) &&
                (!lhs.getName().equals(symbolTable.getSuper()) || !rhs.getName().equals(symbolTable.getClassName())))
        {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Error in attribuition: imcompatible types -> " + lhs.getName() + " : " + rhs.getName()));
        }
        return new Type(lhs.getName(), lhs.isArray());
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