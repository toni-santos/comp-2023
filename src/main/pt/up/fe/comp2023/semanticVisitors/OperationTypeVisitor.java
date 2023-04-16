package pt.up.fe.comp2023.semanticVisitors;

import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;

import pt.up.fe.comp2023.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OperationTypeVisitor extends AJmmVisitor<Object, Boolean> {

    List<Report> reports = new ArrayList<>();
    SimpleSymbolTable symbolTable;

    public OperationTypeVisitor(SimpleSymbolTable symbolTable){
        super();
        this.symbolTable = symbolTable;
    }


    @Override
    protected void buildVisitor() {

        setDefaultVisit(this::dealNext);

        addVisit("BinaryOp", this::dealWithThisBinOp);
        addVisit("UnaryOp", this::dealWithThisUnaryOp);
    }

    private Boolean dealWithThisBinOp(JmmNode jmmNode, Object dummy) {

        return true;
    }

    private Boolean dealWithThisUnaryOp(JmmNode jmmNode, Object dummy) {

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