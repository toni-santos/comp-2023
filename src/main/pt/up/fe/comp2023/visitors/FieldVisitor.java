package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class FieldVisitor extends AJmmVisitor<String, Boolean> {

    @Override
    protected void buildVisitor() {
        addVisit("varDeclaration", this::dealWithField);
    }
    private Boolean dealWithField(JmmNode jmmNode, String field) {
        return null;
    }


}
