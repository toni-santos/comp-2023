package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmVisitor;
import pt.up.fe.comp2023.Method;

import java.util.List;

public class MethodVisitor extends AJmmVisitor<List<Method>, Boolean> implements VisitorInterface {

    @Override
    protected void buildVisitor() {
        addVisit("methodDeclaration", this::dealWithMethod);
    }

    private Boolean dealWithMethod(JmmNode jmmNode, List<Method> methods) {
        Method method = new Method();


        if (jmmNode.getKind().equals("MainMethod")) {
            method.setReturnType(new Type("void", false));
            method.setName("main");
        } else {
            jmmNode.getJmmChild(0);


            //method.setReturnType();
            method.setName("name");
        }

        for (JmmNode child : jmmNode.getChildren()) {
            if (types.contains(child.getKind())) {

            } else if (statements.contains(child.getKind())) {

            } else if (child.getKind().equals("varDeclaration")) {

            }
        }

        return null;
    }


}
