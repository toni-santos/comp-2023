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

public class MethodDeclarationVisitor extends AJmmVisitor<Object, Type>  {
    List<Report> reports = new ArrayList<>();
    SimpleSymbolTable symbolTable;

    public MethodDeclarationVisitor(SimpleSymbolTable symbolTable){
        super();
        this.symbolTable = symbolTable;
    }


    @Override
    protected void buildVisitor() {

        setDefaultVisit(this::dealNext);

        addVisit("MethodDeclaration", this::dealWithMethodDeclaration);
    }

    private Type dealWithMethodDeclaration(JmmNode jmmNode, Object dummy) {
        if (jmmNode.get("methodName").equals("main")){
            return new Type("", false);
        }
        int numOfChildren = jmmNode.getChildren().size() - 1;
        if(numOfChildren > -1) {
            Type type = new Type("", false);

            switch(jmmNode.getJmmChild(numOfChildren).getJmmChild(0).getKind()) {
                case "DeclarationStatement":
                    AssignmentVisitor assignmentVisitor = new AssignmentVisitor(symbolTable);
                    type = assignmentVisitor.visit(jmmNode.getJmmChild(numOfChildren).getJmmChild(0), 0);
                    break;
                case "This":
                case "Parenthesis":
                    ExpressionVisitor expressionVisitor = new ExpressionVisitor(symbolTable);
                    type = expressionVisitor.visit(jmmNode.getJmmChild(numOfChildren).getJmmChild(0), 0);
                    break;
                case "ArraySubscript":
                    ArrayVisitor arrayVisitor = new ArrayVisitor(symbolTable);
                    type = arrayVisitor.visit(jmmNode.getJmmChild(numOfChildren).getJmmChild(0), 0);
                    break;
                case "BinaryOp":
                case "UnaryOp":
                    OperationTypeVisitor opVisitor = new OperationTypeVisitor(symbolTable);
                    type = opVisitor.visit(jmmNode.getJmmChild(numOfChildren).getJmmChild(0), 0);
                    break;
                case "LengthMethod":
                case "MethodCall":
                    MethodVisitor methodVisitor = new MethodVisitor(symbolTable);
                    type = methodVisitor.visit(jmmNode.getJmmChild(numOfChildren).getJmmChild(0), 0);
                    break;
                case "IntValue":
                case "NewObject":
                case "NewArray":
                case "BooleanValue":
                case "Identifier":
                    VariableVisitor variableVisitor = new VariableVisitor(symbolTable);
                    type = variableVisitor.visit(jmmNode.getJmmChild(numOfChildren).getJmmChild(0), 0);
                    break;
            }

            int line = 0;
            int col = 0;
            if(type != null && !type.isArray()) {
                if(!type.getName().equals(jmmNode.getJmmChild(0).get("value"))) {
                    if(jmmNode.getJmmChild(numOfChildren).getJmmChild(0).getKind().equals("MethodCall")) {
                        Type callerType = new Type("", false);
                        switch(jmmNode.getJmmChild(numOfChildren).getJmmChild(0).getJmmChild(0).getKind()) {
                            case "DeclarationStatement":
                                AssignmentVisitor assignmentVisitor = new AssignmentVisitor(symbolTable);
                                callerType = assignmentVisitor.visit(jmmNode.getJmmChild(numOfChildren).getJmmChild(0).getJmmChild(0), 0);
                                break;
                            case "This":
                            case "Parenthesis":
                                ExpressionVisitor expressionVisitor = new ExpressionVisitor(symbolTable);
                                callerType = expressionVisitor.visit(jmmNode.getJmmChild(numOfChildren).getJmmChild(0).getJmmChild(0), 0);
                                break;
                            case "ArraySubscript":
                                ArrayVisitor arrayVisitor = new ArrayVisitor(symbolTable);
                                callerType = arrayVisitor.visit(jmmNode.getJmmChild(numOfChildren).getJmmChild(0).getJmmChild(0), 0);
                                break;
                            case "BinaryOp":
                            case "UnaryOp":
                                OperationTypeVisitor opVisitor = new OperationTypeVisitor(symbolTable);
                                callerType = opVisitor.visit(jmmNode.getJmmChild(numOfChildren).getJmmChild(0).getJmmChild(0), 0);
                                break;
                            case "LengthMethod":
                            case "MethodCall":
                                MethodVisitor methodVisitor = new MethodVisitor(symbolTable);
                                callerType = methodVisitor.visit(jmmNode.getJmmChild(numOfChildren).getJmmChild(0).getJmmChild(0), 0);
                                break;
                            case "IntValue":
                            case "NewObject":
                            case "NewArray":
                            case "BooleanValue":
                            case "Identifier":
                                VariableVisitor variableVisitor = new VariableVisitor(symbolTable);
                                callerType = variableVisitor.visit(jmmNode.getJmmChild(numOfChildren).getJmmChild(0).getJmmChild(0), 0);
                                break;
                        }
                        if(!symbolTable.getImports().contains(callerType.getName())) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Error: invalid return type on method " + jmmNode.get("methodName") + " method not declared or imported"));
                        }
                    }
                    else {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Error: invalid return type on method " + jmmNode.get("methodName") + ". Expected " + jmmNode.getJmmChild(0).get("value") + " but got " + type.getName()));
                    }
                }
            }
        }
        return new Type("", false);
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
