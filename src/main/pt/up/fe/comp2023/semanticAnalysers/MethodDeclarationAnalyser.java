package pt.up.fe.comp2023.semanticAnalysers;

import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.SimpleSymbolTable;
import pt.up.fe.comp2023.semanticVisitors.MethodDeclarationVisitor;

import java.util.List;

public class MethodDeclarationAnalyser implements GenericSemanticAnalyser {
    private final SimpleSymbolTable symbolTable;
    private final JmmParserResult parserResult;

    public MethodDeclarationAnalyser(SimpleSymbolTable symbolTable, JmmParserResult parserResult) {
        this.symbolTable = symbolTable;
        this.parserResult = parserResult;
    }
    @Override
    public List<Report> getReports() {
        MethodDeclarationVisitor semanticVisitor = new MethodDeclarationVisitor(symbolTable);
        semanticVisitor.visit(this.parserResult.getRootNode(), 0);
        return semanticVisitor.getReports();
    }
}
