package pt.up.fe.comp2023.semanticVisitors;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
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

    private Type dealWithId(JmmNode jmmNode, Object dummy) {
        IdentifierDeclarationVisitor idVisitor = new IdentifierDeclarationVisitor(symbolTable);
        return idVisitor.visit(jmmNode, 0);
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
