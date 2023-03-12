package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;

public class SimpleAnalysis implements JmmAnalysis {
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {
        SimpleSymbolTable symbolTable = new SimpleSymbolTable(jmmParserResult.getRootNode());
        return new JmmSemanticsResult(jmmParserResult, symbolTable, jmmParserResult.getReports());
    }
}
