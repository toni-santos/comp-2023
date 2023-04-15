package pt.up.fe.comp2023.semanticAnalysers;

import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.SimpleSymbolTable;
import pt.up.fe.comp2023.semanticVisitors.IdentifierDeclarationVisitor;
import pt.up.fe.comp.jmm.parser.JmmParserResult;

import java.util.List;

public class IdentifierDeclarationAnalyser implements GenericSemanticAnalyser {
    private final SimpleSymbolTable symbolTable;
    private final JmmParserResult parserResult;

    public IdentifierDeclarationAnalyser(SimpleSymbolTable symbolTable, JmmParserResult parserResult) {
        this.symbolTable = symbolTable;
        this.parserResult = parserResult;
    }
    @Override
    public List<Report> getReports() {
        IdentifierDeclarationVisitor semanticVisitor = new IdentifierDeclarationVisitor(symbolTable);
        semanticVisitor.visit(this.parserResult.getRootNode(), 0);
        return semanticVisitor.getReports();
    }
}
