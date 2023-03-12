package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class ClassVisitor extends AJmmVisitor<String, Boolean> {

    @Override
    protected void buildVisitor() {
        addVisit("classDeclaration", this::dealWithClass);
    }
    private Boolean dealWithClass(JmmNode jmmNode, String string) {
        return null;
    }


}
