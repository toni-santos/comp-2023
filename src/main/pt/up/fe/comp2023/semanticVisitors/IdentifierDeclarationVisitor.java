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

public class IdentifierDeclarationVisitor extends AJmmVisitor<Object, Type> {

    List<Report> reports = new ArrayList<>();
    SimpleSymbolTable symbolTable;

    public IdentifierDeclarationVisitor(SimpleSymbolTable symbolTable){
        super();
        this.symbolTable = symbolTable;
    }


    @Override
    protected void buildVisitor() {

        setDefaultVisit(this::dealNext);

        addVisit("Identifier", this::dealWithId);
        addVisit("Type", this::dealWithIdType);
    }

    private Type dealWithId(JmmNode jmmNode, Object dummy) {

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
                    return local.getType();
                }
            }
            List<Symbol> params = symbolTable.getParameters(method);
            for(Symbol param : params) {
                if(param.getName().equals(name)) {
                    return param.getType();
                }
            }
            List<Symbol> fields = symbolTable.getFields();
            for(Symbol field : fields) {
                if(field.getName().equals(name)) {
                    if (method.equals("main")) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")),
                                "Variable " + name + " is a field and cannot be used in main method"));
                    }
                    return field.getType();
                }
            }
            if((symbolTable.getImports() == null || !symbolTable.getImports().contains(name)) && (symbolTable.getSuper().equals("") || !symbolTable.getSuper().equals(name)) && !symbolTable.getClassName().equals(name)) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Error: variable " + name + " not declared"));
            }
            if(symbolTable.getImports().contains(name)){
                return new Type(name, false);
            }
        }
        return new Type("", false);
    }

    private Type dealWithIdType(JmmNode jmmNode, Object dummy) {

        if (!jmmNode.hasAttribute("value")){
            return new Type("", false);
        }
        else if (jmmNode.get("value").equals("int") || jmmNode.get("value").equals("boolean") || jmmNode.get("value").equals("void")){
            return new Type("", false);
        }

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
                    return local.getType();
                }
            }
            List<Symbol> params = symbolTable.getParameters(method);
            for(Symbol param : params) {
                if(param.getName().equals(name)) {
                    return param.getType();
                }
            }
            List<Symbol> fields = symbolTable.getFields();
            for(Symbol field : fields) {
                if(field.getName().equals(name)) {
                    if (method.equals("main")) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")),
                                "Variable " + name + " is a field and cannot be used in main method"));
                    }
                    return field.getType();
                }
            }
            if((symbolTable.getImports() == null || !symbolTable.getImports().contains(name)) && (symbolTable.getSuper().equals("") || !symbolTable.getSuper().equals(name)) && !symbolTable.getClassName().equals(name)) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(jmmNode.get("lineStart")), Integer.parseInt(jmmNode.get("colStart")), "Error: invalid type"));
            }
            else{
                return new Type(name, false);
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