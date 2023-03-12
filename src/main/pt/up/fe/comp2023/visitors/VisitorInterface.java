package pt.up.fe.comp2023.visitors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface VisitorInterface {
    public final List<String> types = new ArrayList<String>(
            Arrays.asList("IntArray", "Boolean", "Integer", "Object")
    );

    public final List<String> statements = new ArrayList<String>(
            Arrays.asList("Brackets", "IfELse", "While", "RegularStatement", "DeclarationStatement", "ArrayStatement")
    );
}
