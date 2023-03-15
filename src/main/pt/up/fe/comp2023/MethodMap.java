package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodMap {
    private final Map<String, Method> methods;
    private final Integer methodsLength;

    public MethodMap(List<Method> methods ) {
        Map<String, Method> map = new HashMap<String, Method>();
        methods.forEach(method -> map.put(method.getName(), method));
        this.methods = map;
        this.methodsLength = methods.size();
    }

    public Map<String, Method> getMethodsMap() {
        return this.methods;
    }

    public Integer length() {
        return this.methodsLength;
    }

    public Type returnType(String s) {
        return this.methods.get(s).getReturnType();
    }

    public List<String> getMethodsName() {
        return this.methods.keySet().stream().toList();
    }

    public List<Symbol> getParameters(String s) {
        return this.methods.get(s).getParameters();
    }

    public List<Symbol> getLocalVariables(String s) {
        return this.methods.get(s).getVariables();
    }
}
