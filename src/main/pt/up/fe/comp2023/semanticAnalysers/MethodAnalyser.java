package pt.up.fe.comp2023.semanticAnalysers;

import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.SimpleSymbolTable;

import java.util.List;

public class MethodAnalyser implements GenericSemanticAnalyser {
    private final SimpleSymbolTable symbolTable;
    public MethodAnalyser(SimpleSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }
    @Override
    public List<Report> getReports() {
        return null;
    }
}
