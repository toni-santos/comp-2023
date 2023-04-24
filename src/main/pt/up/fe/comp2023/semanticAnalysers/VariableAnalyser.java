package pt.up.fe.comp2023.semanticAnalysers;

import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.SimpleSymbolTable;
import pt.up.fe.comp2023.semanticVisitors.VariableVisitor;

import java.util.List;

public class VariableAnalyser implements GenericSemanticAnalyser{
    private final SimpleSymbolTable symbolTable;
    private final JmmParserResult parserResult;
    public VariableAnalyser(SimpleSymbolTable symbolTable, JmmParserResult parserResult) {
        this.symbolTable = symbolTable;
        this.parserResult = parserResult;
    }
    @Override
    public List<Report> getReports() {
        VariableVisitor variableVisitor = new VariableVisitor(symbolTable);
        variableVisitor.visit(this.parserResult.getRootNode(), 0);
        return variableVisitor.getReports();
    }
}
