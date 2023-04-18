package pt.up.fe.comp2023.jasmin;

import org.specs.comp.ollir.*;

import java.util.HashMap;

public class JasminUtils {
    public static String getType(Type type, HashMap<ElementType, String> simpleTypes) {
        ElementType elementType = type.getTypeOfElement();
        switch (elementType) {
            case ARRAYREF -> {
                return getArrayType((ArrayType) type, simpleTypes);
            }
            case OBJECTREF, CLASS -> {
                return ((ClassType) type).getName();
            }
            case THIS -> {
                return JasminGenerator.getClassName();
            }
            default -> {
                return simpleTypes.get(elementType);
            }
        }
    }

    public static String getArrayType(ArrayType type, HashMap<ElementType, String> simpleTypes) {
        return "[".repeat(type.getNumDimensions()) + getType(type.getElementType(), simpleTypes);
    }

    public static boolean isBetween(int integer, int lhs, int rhs) {
        return integer >= lhs && integer <= rhs;
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
