package pt.up.fe.comp2023.jasmin;

import org.specs.comp.ollir.*;

import java.util.HashMap;

public class JasminUtils {
    public static int maxStackLimit;
    private static int currentStackLimit;

    public static void updateStackLimits(int change) {
        maxStackLimit = Math.max(currentStackLimit += change, maxStackLimit);
        System.out.println("currentStackLimit = " + currentStackLimit);
        System.out.println("maxStackLimit = " + maxStackLimit);
    }

    public static void resetStackLimits() {
        currentStackLimit = 0;
        maxStackLimit = 0;
    }
    public static boolean isBetween(int integer, int lhs, int rhs) {
        return integer >= lhs && integer <= rhs;
    }

    public static String getClassName() {
        return JasminGenerator.ollirClass.getClassName();
    }
    public static String getClassWithImports(String className) {
        if (className.equals("this")) { return JasminUtils.getClassName(); }
        for (String importName : JasminGenerator.ollirClass.getImports()) {
            if (importName.endsWith(className)) { return importName; }
        }
        return className;
    }
    public static String getType(Type type) {
        ElementType elementType = type.getTypeOfElement();
        switch (elementType) {
            case ARRAYREF -> {
                return getArrayType((ArrayType) type);
            }
            case OBJECTREF, CLASS -> {
                return "L" + getClassWithImports(((ClassType) type).getName()) + ";";
            }
            case THIS -> {
                return getClassName();
            }
            default -> {
                return JasminGenerator.simpleTypes.get(elementType);
            }
        }
    }

    public static String getArrayType(ArrayType type) {
        return "[".repeat(type.getNumDimensions()) + getType(type.getElementType());
    }

    public static ElementType getElementType(Element element) {
        return element.getType().getTypeOfElement();
    }

    public static String getOp(Operation op) {
        return switch (op.getOpType()) {
            case ADD -> "iadd";
            case SUB -> "isub";
            case MUL -> "imul";
            case DIV -> "idiv";
            case SHR -> "ishr";
            case SHL -> "ishl";
            case XOR -> "ixor";
            case AND, ANDB -> "iand";
            case OR -> "ior";
            case LTH -> "if_icmplt";
            case NOTB -> "ifeq";
            default -> "; ERROR: " + op.getOpType() + " not implemented\n";
        };
    }
}
