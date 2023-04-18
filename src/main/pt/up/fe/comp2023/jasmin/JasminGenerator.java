package pt.up.fe.comp2023.jasmin;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.ElementType;

import java.util.HashMap;

public class JasminGenerator {
    public StringBuilder jasminCode;
    public static ClassUnit ollirClass;
    public HashMap<ElementType, String> simpleTypes;

    public JasminGenerator(ClassUnit ollirClass) {
        this.jasminCode = new StringBuilder();
        JasminGenerator.ollirClass = ollirClass;
        this.simpleTypes = this.getSimpleTypes();
    }

    public String generate() {
        String className = ollirClass.getClassName();
        String superName = ollirClass.getSuperClass() != null ? ollirClass.getSuperClass() : "java/lang/Object";
        jasminCode.append(".class public ").append(className).append("\n");
        jasminCode.append(".super ").append(superName).append("\n\n");
        for (int i = 0; i < ollirClass.getNumFields(); i++) {
            String jasminField = new JasminField(ollirClass, simpleTypes).getField(i);
            jasminCode.append(jasminField).append("\n");
        }

        jasminCode.append(".method public <init>()V\n").append("\taload_0\n").append("\tinvokespecial ")
                .append(superName).append("/<init>()V\n")
                .append("\treturn\n")
                .append(".end method").append("\n".repeat(5));

        for (int i = 0; i < ollirClass.getNumMethods(); i++) {
            String jasminMethod = new JasminMethod(ollirClass, simpleTypes).getMethod(i);
            if (!ollirClass.getMethod(i).isConstructMethod()) jasminCode.append(jasminMethod).append("\n");
        }
        return jasminCode.toString();
    }

    public HashMap<ElementType, String> getSimpleTypes() {
        simpleTypes = new HashMap<ElementType, String>();
        simpleTypes.put(ElementType.INT32, "I");
        simpleTypes.put(ElementType.BOOLEAN, "Z");
        simpleTypes.put(ElementType.STRING, "Ljava/lang/String;");
        simpleTypes.put(ElementType.VOID, "V");
        return simpleTypes;
    }

    public static String getClassName() {
        return ollirClass.getClassName();
    }
}
