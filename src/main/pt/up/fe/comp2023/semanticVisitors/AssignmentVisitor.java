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
        addVisit("ArrayStatement", this::dealWithArrayAssignment);
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

        if (lhs.isArray() && !rhs.isArray() && !jmmNode.getJmmChild(1).getKind().equals("NewArray"))
        {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Arrays can only be assigned array values"));
        } else if (!lhs.isArray() && rhs.isArray()){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Not able to assign array value to non-array"));
        }

        if(!lhs.getName().equals(rhs.getName()) &&
                ( !symbolTable.getImports().contains(lhs.getName()) || !symbolTable.getImports().contains(rhs.getName())) &&
                (!lhs.getName().equals(symbolTable.getSuper()) || !rhs.getName().equals(symbolTable.getClassName())) &&
                !(lhs.getName().equals("int") && lhs.isArray() && rhs.getName().equals("array"))
        )
        {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Error in assignment: imcompatible types -> " + lhs.getName() + " : " + rhs.getName()));
        }
        return new Type(lhs.getName(), lhs.isArray());
    }


    private Type dealWithArrayAssignment(JmmNode jmmNode, Object dummy){

        // check id for existing variable

        Type idType = new Type("", false);

        String name = jmmNode.get("value");
        JmmNode parent = jmmNode.getJmmParent();
        while(!parent.getKind().equals("MethodDeclaration") && !parent.getKind().equals("ImportDeclaration")) {
            parent = parent.getJmmParent();
        }

        if(!parent.getKind().equals("ImportDeclaration")) {
            String method = parent.get("methodName");
            List<Symbol> locals = symbolTable.getLocalVariables(method);
            for(Symbol local : locals) {
                if(local.getName().equals(name)) {
                    idType = local.getType();
                    break;
                }
            }
            List<Symbol> params = symbolTable.getParameters(method);
            for(Symbol param : params) {
                if(param.getName().equals(name)) {
                    idType = param.getType();
                    break;
                }
            }
            List<Symbol> fields = symbolTable.getFields();
            for(Symbol field : fields) {
                if(field.getName().equals(name)) {
                    if (method.equals("main")) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")),
                                "Variable " + name + " is a field and cannot be used in main method"));
                    }
                    idType = field.getType();
                    break;
                }
            }
            if (idType.getName().equals("")) {
                if ((symbolTable.getImports() == null || !symbolTable.getImports().contains(name)) && (symbolTable.getSuper().equals("") || !symbolTable.getSuper().equals(name)) && !symbolTable.getClassName().equals(name)) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Error: variable " + name + " not declared"));
                    return new Type("array", true);
                }
                if (symbolTable.getImports().contains(name)) {
                    idType = new Type(name, false);
                }
            }
        }

        if (!idType.isArray()){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Error: variable " + name + " is not an array"));
            return new Type("array", true);
        }

        // check child 0 (index) is int

        Type indexType = new Type("", false);

        switch(jmmNode.getJmmChild(0).getKind()) {
            case "ArrayStatement":
            case "DeclarationStatement":
                indexType = this.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "Parenthesis":
                ExpressionVisitor expressionVisitor = new ExpressionVisitor(symbolTable);
                indexType = expressionVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "IntValue":
            case "BooleanValue":
            case "NewObject":
            case "NewArray":
            case "Identifier":
                VariableVisitor variableVisitor = new VariableVisitor(symbolTable);
                indexType = variableVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "Length":
            case "MethodCall":
                MethodVisitor methodVisitor = new MethodVisitor(symbolTable);
                indexType = methodVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "BinaryOp":
            case "UnaryOp":
                OperationTypeVisitor opVisitor = new OperationTypeVisitor(symbolTable);
                indexType = opVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
            case "ArraySubscript":
                ArrayVisitor arrayVisitor = new ArrayVisitor(symbolTable);
                indexType = arrayVisitor.visit(jmmNode.getJmmChild(0), 0);
                break;
        }

        if (!indexType.getName().equals("int") || indexType.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Array index must be an integer"));
            return new Type("array", true);
        }

        // check child 1 is int

        Type type = new Type("", false);

        switch(jmmNode.getJmmChild(1).getKind()) {
            case "ArrayStatement":
            case "DeclarationStatement":
                type = this.visit(jmmNode.getJmmChild(1), 0);
                break;
            case "Parenthesis":
                ExpressionVisitor expressionVisitor = new ExpressionVisitor(symbolTable);
                type = expressionVisitor.visit(jmmNode.getJmmChild(1), 0);
                break;
            case "IntValue":
            case "BooleanValue":
            case "NewObject":
            case "NewArray":
            case "Identifier":
                VariableVisitor variableVisitor = new VariableVisitor(symbolTable);
                type = variableVisitor.visit(jmmNode.getJmmChild(1), 0);
                break;
            case "Length":
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
                ArrayVisitor arrayVisitor = new ArrayVisitor(symbolTable);
                type = arrayVisitor.visit(jmmNode.getJmmChild(1), 0);
                break;
        }

        if (!type.getName().equals("int") || type.isArray()) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Value to assign must be an array"));
            return new Type("array", true);
        }


        return new Type("array", true);
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