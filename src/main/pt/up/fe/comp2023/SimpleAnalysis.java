package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.semanticAnalysers.*;

import java.util.ArrayList;
import java.util.List;

public class SimpleAnalysis implements JmmAnalysis {
    /*
    - Analysers provenientes de um analyser genérico com method comum getReports()
    - Cada analyser tem um visitor que percorre a symbol table à procura de pormenores específicos que o analyser
    precisa
    - Em caso de erro, adiciona-se ao report
    - No final, reports adicionados à lista de reports do SimpleAnalyser
    - Return do resultado com reports da análise semântica
 */
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {
        SimpleSymbolTable symbolTable = new SimpleSymbolTable(jmmParserResult.getRootNode());
        List<GenericSemanticAnalyser> semanticAnalysers = new ArrayList<GenericSemanticAnalyser>();
        List<Report> reports = new ArrayList<Report>();

        semanticAnalysers.add(new ArrayAnalyser(symbolTable));
        semanticAnalysers.add(new AssignmentAnalyser(symbolTable));
        semanticAnalysers.add(new ConditionalAnalyser(symbolTable));
        semanticAnalysers.add(new ExpressionAnalyser(symbolTable));
        semanticAnalysers.add(new IdentifierDeclarationAnalyser(symbolTable));
        semanticAnalysers.add(new MethodAnalyser(symbolTable));
        semanticAnalysers.add(new OperationTypeAnalyser(symbolTable));
        semanticAnalysers.add(new VariableAnalyser(symbolTable));

        for (GenericSemanticAnalyser semanticAnalyser : semanticAnalysers) {
            List<Report> report = semanticAnalyser.getReports();
            reports.addAll(report);
        }

        return new JmmSemanticsResult(jmmParserResult, symbolTable, reports);
    }
}
