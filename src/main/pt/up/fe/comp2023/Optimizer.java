package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.ollir.SimpleOllir;
import pt.up.fe.comp2023.ollir.optimize.ConstantFolding;
import pt.up.fe.comp2023.ollir.optimize.ConstantPropagation;

public class Optimizer implements JmmOptimization {
    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        if (!semanticsResult.getConfig().containsKey("optimize")) return semanticsResult;
        ConstantPropagation constantPropagation = new ConstantPropagation();
        ConstantFolding constantFolding = new ConstantFolding();

        do {
            constantPropagation.visit(semanticsResult.getRootNode());
            constantFolding.visit(semanticsResult.getRootNode());
        } while (constantPropagation.isChanged() || constantFolding.isChanged());

        constantPropagation.setProcessWhile(true);
        constantPropagation.visit(semanticsResult.getRootNode());
        constantFolding.visit(semanticsResult.getRootNode());

        return semanticsResult;
    }

    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {
        
        SimpleOllir ollir = new SimpleOllir();
        OllirResult ollirResult = ollir.toOllir(jmmSemanticsResult);

        return ollirResult;
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        return JmmOptimization.super.optimize(ollirResult);
    }
}
