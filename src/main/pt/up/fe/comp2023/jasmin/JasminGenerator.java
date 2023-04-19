package pt.up.fe.comp2023.jasmin;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.ElementType;

import java.util.HashMap;

public class JasminGenerator {
    public StringBuilder jasminCode;
    public static ClassUnit ollirClass;
    public static final HashMap<ElementType, String> simpleTypes = generateSimpleTypes();

    public JasminGenerator(ClassUnit ollirClass) {
        this.jasminCode = new StringBuilder();
        JasminGenerator.ollirClass = ollirClass;
    }

    public String generate() {
        String className = ollirClass.getClassName();
        String superName = ollirClass.getSuperClass() != null ? ollirClass.getSuperClass() : "java/lang/Object";
        jasminCode.append(".class public ").append(className).append("\n");
        jasminCode.append(".super ").append(superName).append("\n\n");
        jasminCode.append(generateFields());
        jasminCode.append(generateDefaultConstructor(superName));
        jasminCode.append(generateMethods());
        return jasminCode.toString();
    }

    private static String generateFields() {
        StringBuilder fields = new StringBuilder();
        for (int i = 0; i < ollirClass.getNumFields(); i++) {
            String jasminField = new JasminField(ollirClass).getField(i);
            fields.append(jasminField).append("\n");
        }
        return fields.toString();
    }

    private static String generateMethods() {
        StringBuilder methods = new StringBuilder();
        for (int i = 0; i < ollirClass.getNumMethods(); i++) {
            String jasminMethod = new JasminMethod(ollirClass).getMethod(i);
            if (!ollirClass.getMethod(i).isConstructMethod()) methods.append(jasminMethod).append("\n");
        }
        return methods.toString();
    }

    private static String generateDefaultConstructor(String superName) {
        return ".method public <init>()V\n\taload_0\n\tinvokespecial " +
                superName + "/<init>()V\n\treturn\n.end method" + "\n".repeat(5);
    }
    private static HashMap<ElementType, String> generateSimpleTypes() {
        HashMap<ElementType, String> types = new HashMap<>();
        types.put(ElementType.INT32, "I");
        types.put(ElementType.BOOLEAN, "Z");
        types.put(ElementType.STRING, "Ljava/lang/String;");
        types.put(ElementType.VOID, "V");
        return types;
    }

}
