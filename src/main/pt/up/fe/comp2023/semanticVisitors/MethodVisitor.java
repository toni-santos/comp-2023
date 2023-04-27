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
    }

    private Type dealWithMethod(JmmNode jmmNode, Object dummy) {
        Type type;
        try {
            type = this.symbolTable.getReturnType(jmmNode.get("value"));
        }
        catch (Exception e){
            type = null;
        }


        if (type == null && !symbolTable.getSuper().equals("") || type == null && !symbolTable.getImports().isEmpty()) {
            switch (jmmNode.getJmmParent().getKind()) {
                case "DeclarationStatement":
                    JmmNode child = jmmNode.getJmmParent().getJmmChild(0);
                    VariableVisitor variableVisitor = new VariableVisitor(symbolTable);
                    type = variableVisitor.visit(child);
                    break;
                default:
                    type = this.visit(jmmNode.getJmmChild(0));
                    break;
            }
            return type;
        } else if (type == null && jmmNode.getJmmChild(0).getKind().equals("This")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Error on method " + jmmNode.get("value") + ": Method Undeclared"));
            return new Type("", false);
        } else if (type == null && symbolTable.getSuper().equals("") && symbolTable.getImports().isEmpty()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Error on method " + jmmNode.get("value") + ": Method Undeclared"));
            return new Type("", false);
        }

        List<Symbol> params = symbolTable.getParameters(jmmNode.get("value"));

        if (params.size() != (jmmNode.getChildren().size() - 1)){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Error on method " + jmmNode.get("value") + ": invalid method call, number of parameters not matching method definition."));
            return type != null ? type: new Type("", false);
        }

        for(int i = 1; i < jmmNode.getChildren().size(); i++){
            Type argType = new Type("", false);

            switch(jmmNode.getJmmChild(i).getKind()) {
                case "ArrayStatement":
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

            if(!params.get(i-1).getType().equals(argType)) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Error on method " + jmmNode.get("value") + ": invalid method call, types of parameters are invalid. Parameter " + params.get(i-1).getName() + " expected " + params.get(i-1).getType() + " but got " + argType));
            }
        }
        return type != null ? type: new Type("", false);
    }

    private Type dealWithLength(JmmNode jmmNode, Object dummy) {
        VariableVisitor variableVisitor = new VariableVisitor(symbolTable);
        Type type = variableVisitor.visit(jmmNode.getJmmChild(0));
        if(!type.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Error in length method: " + jmmNode.getJmmChild(0).get("value") + " is not an array"));
            return new Type("", false);
        }
        return new Type("int", false);
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