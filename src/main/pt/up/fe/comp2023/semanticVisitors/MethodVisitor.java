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

public class MethodVisitor extends AJmmVisitor<Object, Type> {

    List<Report> reports = new ArrayList<>();
    SimpleSymbolTable symbolTable;

    public MethodVisitor(SimpleSymbolTable symbolTable){
        super();
        this.symbolTable = symbolTable;
    }


    @Override
    protected void buildVisitor() {

        setDefaultVisit(this::dealNext);

        addVisit("MethodCall", this::dealWithMethod);
        addVisit("Length", this::dealWithLength);
        addVisit("MethodDeclaration", this::dealWithMethodDeclaration);
    }

    private Type dealWithMethod(JmmNode jmmNode, Object dummy) {
        Type type;
        try {
            type = this.symbolTable.getReturnType(jmmNode.get("value"));
        }
        catch (Exception e){
            type = null;
        }
        //int line = Integer.valueOf(jmmNode.get("line"));
        //int col = Integer.valueOf(jmmNode.get("col"));
        int line = 0;
        int col = 0;


        if (type == null && symbolTable.getSuper() != null || type == null && !symbolTable.getImports().isEmpty()) {
            switch (jmmNode.getJmmParent().getKind()) {
                case "DeclarationStatement":
                    JmmNode child = jmmNode.getJmmParent().getJmmChild(0);
                    VariableVisitor variableVisitor = new VariableVisitor(symbolTable);
                    type = variableVisitor.visit(child);
                    break;
                default:
                    type = visit(jmmNode.getJmmChild(0));
                    break;
            }
            return type;
        } else if (type == null && jmmNode.getJmmChild(0).getKind().equals("This")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Error on method " + jmmNode.get("value") + ": Method Undeclared"));
            return new Type("", false);
        } else if (type == null && symbolTable.getSuper() == null && symbolTable.getImports().isEmpty()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Error on method " + jmmNode.get("value") + ": Method Undeclared"));
            return new Type("", false);
        }

        for(int i = 1; i < jmmNode.getChildren().size(); i++){
            Type argType = new Type("", false);

            switch(jmmNode.getJmmChild(i).getKind()) {
                case "DeclarationStatement":
                    AssignmentVisitor assignmentVisitor = new AssignmentVisitor(symbolTable);
                    argType = assignmentVisitor.visit(jmmNode.getJmmChild(i), 0);
                    break;
                case "Parenthesis":
                    ExpressionVisitor expressionVisitor = new ExpressionVisitor(symbolTable);
                    argType = expressionVisitor.visit(jmmNode.getJmmChild(i), 0);
                    break;
                case "IntValue":
                case "BooleanValue":
                case "NewObject":
                case "NewArray":
                case "Identifier":
                    VariableVisitor variableVisitor = new VariableVisitor(symbolTable);
                    argType = variableVisitor.visit(jmmNode.getJmmChild(i), 0);
                    break;
                case "Length":
                case "MethodCall":
                    argType = this.visit(jmmNode.getJmmChild(i), 0);
                    break;
                case "BinaryOp":
                case "UnaryOp":
                    OperationTypeVisitor opVisitor = new OperationTypeVisitor(symbolTable);
                    argType = opVisitor.visit(jmmNode.getJmmChild(i), 0);
                    break;
                case "ArraySubscript":
                    ArrayVisitor arrayVisitor = new ArrayVisitor(symbolTable);
                    argType = arrayVisitor.visit(jmmNode.getJmmChild(i), 0);
                    break;
                default:
                    argType = visit(jmmNode.getJmmChild(i), 0);
                    break;
            }
            //int argLine = Integer.valueOf(jmmNode.getJmmChild(1).getJmmChild(i).get("line"));
            //int argCol = Integer.valueOf(jmmNode.getJmmChild(1).getJmmChild(i).get("col"));
            List<Symbol> params = symbolTable.getParameters(jmmNode.get("value"));
            if(!params.get(i-1).getType().equals(argType)) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Error on method " + jmmNode.get("value") + ": invalid method call, types of parameters are invalid. Parameter " + params.get(i-1).getName() + " expected " + params.get(i-1).getType() + " but got " + argType));
            }
        }
        return type != null ? type: new Type("", false);
    }

    private Type dealWithLength(JmmNode jmmNode, Object dummy) {

        return new Type("int", false);
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
                    type = this.visit(jmmNode.getJmmChild(numOfChildren).getJmmChild(0), 0);
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
                                callerType = this.visit(jmmNode.getJmmChild(numOfChildren).getJmmChild(0).getJmmChild(0), 0);
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
            visit(child);
        }
        return new Type("", false);
    }
    public List<Report> getReports() {
        return this.reports;
    }
}