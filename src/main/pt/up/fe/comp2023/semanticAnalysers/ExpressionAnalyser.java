package pt.up.fe.comp2023.semanticAnalysers;

import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.SimpleSymbolTable;

import java.util.List;

public class ExpressionAnalyser implements GenericSemanticAnalyser {
    // Calcular return types de expressões para facilitar no futuro
    private final SimpleSymbolTable symbolTable;
    public ExpressionAnalyser(SimpleSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }
    @Override
    public List<Report> getReports() {
        return null;
    }
}
