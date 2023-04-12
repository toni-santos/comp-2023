package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.ArrayList;

public class SimpleOllir implements JmmOptimization {
    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {

        OllirGenerator ollirGenerator = new OllirGenerator(jmmSemanticsResult.getSymbolTable());

        ollirGenerator.visit(jmmSemanticsResult.getRootNode());

        String code = ollirGenerator.getCode();

        return new OllirResult(jmmSemanticsResult, code, new ArrayList<>());
    }
}
