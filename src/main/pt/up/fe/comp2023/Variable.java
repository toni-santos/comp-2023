package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Type;

public class Variable {
    private Type type;
    private String name;
    public String getName() {
        return name;
    }

    public Variable(Type type, String name) {
        this.type = type;
        this.name = name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
