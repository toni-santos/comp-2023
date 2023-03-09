package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode ;

public class SymbolTableVisitor extends AJmmVisitor<String, String> {

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithProgram);
        addVisit("GenericMethod", this::dealWithMethod);
        addVisit("MainMethod", this::dealWithMethod);
        addVisit("ClassDeclaration", this::dealWithClass);
        addVisit("VarDeclaration", this::dealWithField);
    }

    private String dealWithProgram(JmmNode jmmNode, String string){
        return null;
    }

    private String dealWithMethod(JmmNode jmmNode, String string){
        return null;
    }

    private String dealWithClass(JmmNode jmmNode, String string){
        return null;
    }

    private String dealWithField(JmmNode jmmNode, String string){
        return null;
    }
}
