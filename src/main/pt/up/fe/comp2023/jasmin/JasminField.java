package pt.up.fe.comp2023.jasmin;

import org.specs.comp.ollir.*;

import java.util.HashMap;

public class JasminField {
    public Field field;
    public String access;
    public boolean isStatic;
    public boolean isFinal;
    public boolean isInitialized;
    public StringBuilder fieldCode;
    public ClassUnit ollirClass;
    public HashMap<ElementType, String> simpleTypes;
    public JasminField(ClassUnit ollirClass, HashMap<ElementType, String> types) {
        this.fieldCode = new StringBuilder();
        this.ollirClass = ollirClass;
        this.simpleTypes = types;
    }

    public String getField(int i) {
        this.field = ollirClass.getField(i);
        this.isStatic = field.isStaticField();
        this.isFinal = field.isFinalField();
        this.isInitialized = field.isInitialized();
        this.access = getAccessModifier(field);

        fieldCode.append(".field ");
        if (access.equals("default")) fieldCode.append("private ");
        else { fieldCode.append(access).append(" "); }
        if (isStatic) {
            fieldCode.append("static ");
        }
        if (isFinal) {
            fieldCode.append("final ");
        }
        fieldCode.append(field.getFieldName()).append(" ").append(JasminUtils.getType(field.getFieldType(), simpleTypes));
        if (isInitialized) {
            fieldCode.append(" = ").append(field.getInitialValue());
        }

        return fieldCode.toString();
    }

    public static String getAccessModifier(Field field) {
        return field.getFieldAccessModifier().name().toLowerCase();
    }
}
