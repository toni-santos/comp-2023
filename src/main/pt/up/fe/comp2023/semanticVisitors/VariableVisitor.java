package pt.up.fe.comp2023.semanticVisitors;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.SimpleSymbolTable;

import java.util.ArrayList;
import java.util.List;

public class VariableVisitor extends AJmmVisitor<Object, Type>{
    List<Report> reports = new ArrayList<>();
    SimpleSymbolTable symbolTable;

    public VariableVisitor(SimpleSymbolTable symbolTable){
        super();
        this.symbolTable = symbolTable;
    }


    @Override
    protected void buildVisitor() {

        setDefaultVisit(this::dealNext);

        addVisit("IntValue", this::dealWithInt);
        addVisit("BooleanValue", this::dealWithBoolean);
        addVisit("NewObject", this::dealWithNewObject);
        addVisit("NewArray", this::dealWithNewArray);
        addVisit("Identifier", this::dealWithId);
    }

    private Type dealWithInt(JmmNode jmmNode, Object dummy) {

        return new Type("int", false);
    }

    private Type dealWithBoolean(JmmNode jmmNode, Object dummy) {

        return new Type("boolean", false);
    }

    private Type dealWithNewObject(JmmNode jmmNode, Object dummy) {

        return new Type(jmmNode.get("value"), false);
    }

    private Type dealWithNewArray(JmmNode jmmNode, Object dummy) {

        Type type = new Type("", false);

        switch(jmmNode.getJmmChild(0).getKind()) {
            case "ArrayStatement":
            case "DeclarationStatement":
                AssignmentVisitor assignmentVisitor = new AssignmentVisitor(symbolTable);
                type = assignmentVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "Parenthesis":
                ExpressionVisitor expressionVisitor = new ExpressionVisitor(symbolTable);
                type = expressionVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "IntValue":
            case "BooleanValue":
            case "NewObject":
            case "NewArray":
            case "Identifier":
                VariableVisitor variableVisitor = new VariableVisitor(symbolTable);
                type = variableVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "Length":
            case "MethodCall":
                MethodVisitor methodVisitor = new MethodVisitor(symbolTable);
                type = methodVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "BinaryOp":
            case "UnaryOp":
                OperationTypeVisitor opVisitor = new OperationTypeVisitor(symbolTable);
                type = opVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "ArraySubscript":
                type = this.visit(jmmNode.getJmmChild(0), 0);
                break;
        }

        if (!type.getName().equals("int") || type.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Array index must be an integer"));
        }
        return new Type("array", true);
    }

    private Type dealWithId(JmmNode jmmNode, Object dummy) {
        IdentifierDeclarationVisitor idVisitor = new IdentifierDeclarationVisitor(symbolTable);
        return idVisitor.visit(jmmNode, 0);
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
