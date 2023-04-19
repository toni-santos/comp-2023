package pt.up.fe.comp2023.jasmin;

import org.specs.comp.ollir.*;

import java.util.HashMap;

public class JasminField {
    public Field field;
    public String name;
    public String access;
    public Type type;
    public boolean isStatic;
    public boolean isFinal;
    public boolean isInitialized;
    public StringBuilder fieldCode;
    public ClassUnit ollirClass;

    public JasminField(ClassUnit ollirClass) {
        this.fieldCode = new StringBuilder();
        this.ollirClass = ollirClass;
    }

    public String getField(int i) {
        this.field = ollirClass.getField(i);
        this.isStatic = field.isStaticField();
        this.isFinal = field.isFinalField();
        this.isInitialized = field.isInitialized();
        this.name = field.getFieldName();
        this.access = getAccessModifier(field);
        this.type = field.getFieldType();

        fieldCode.append(".field ").append(access.equals("default") ? "private " : access + " ");
        fieldCode.append(isStatic ? "static " : "").append(isFinal ? "final " : "");
        fieldCode.append(name).append(" ").append(JasminUtils.getType(type));
        fieldCode.append(isInitialized ? " = " + field.getInitialValue() : "");

        return fieldCode.toString();
    }

    private static String getAccessModifier(Field field) {
        return field.getFieldAccessModifier().name().toLowerCase();
    }
}
