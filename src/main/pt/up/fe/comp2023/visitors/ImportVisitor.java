package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
import java.util.stream.Collectors;

public class ImportVisitor extends AJmmVisitor<List<String>, Boolean> {

    @Override
    protected void buildVisitor() {
        addVisit("importDeclaration", this::dealWithImport);
    }
    private Boolean dealWithImport(JmmNode jmmNode, List<String> imports) {
        String importStr = jmmNode.getChildren().stream().map(node -> node.get("name"))
                .collect(Collectors.joining("."));
        return true;
    }


}
