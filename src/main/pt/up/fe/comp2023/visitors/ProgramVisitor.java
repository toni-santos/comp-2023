package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class ProgramVisitor extends AJmmVisitor<String, Boolean> {

    @Override
    protected void buildVisitor() {
        addVisit("program", this::dealWithProgram);
    }
    private Boolean dealWithProgram(JmmNode jmmNode, String program) {
        return null;
    }


}
