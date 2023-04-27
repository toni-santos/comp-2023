package pt.up.fe.comp2023.jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;

public class JasminInstruction {
    public ClassUnit ollirClass;
    public HashMap<String, Descriptor> variables;

    public JasminInstruction(ClassUnit ollirClass, HashMap<String, Descriptor> variables) {
        this.ollirClass = ollirClass;
        this.variables = variables;
    }

    public String getInstruction(Instruction instruction) {
        InstructionType instructionType = instruction.getInstType();
        switch (instructionType) {
            case ASSIGN -> { return "\t" + dealWithAssign((AssignInstruction) instruction); }
            case CALL -> { return "\t" + dealWithCall((CallInstruction) instruction); }
            case GOTO -> { return "\t" + dealWithGoto((GotoInstruction) instruction); }
            case RETURN -> { return dealWithReturn((ReturnInstruction) instruction); }
            case PUTFIELD -> { return dealWithPutfield((PutFieldInstruction) instruction); }
            case GETFIELD -> { return "\t" + dealWithGetfield((GetFieldInstruction) instruction); }
            case UNARYOPER -> { return "\t" + dealWithUnaryOp((UnaryOpInstruction) instruction); }
            case BINARYOPER -> { return "\t" + dealWithBinaryOp((BinaryOpInstruction) instruction); }
            case NOPER -> { return "\t" + dealWithNop((SingleOpInstruction) instruction); }
            default -> throw new NotImplementedException(instructionType);
        }
    }

    private String dealWithAssign(AssignInstruction instruction) {
        StringBuilder assign = new StringBuilder();
        Operand lhs = (Operand) instruction.getDest();
        Instruction rhs = instruction.getRhs();
        String operandName = lhs.getName();
        if (rhs.getInstType() == InstructionType.BINARYOPER) {
            BinaryOpInstruction expression = (BinaryOpInstruction) rhs;
            OperationType opType = expression.getOperation().getOpType();
            if (opType == OperationType.ADD || opType == OperationType.SUB) {
                String returnCode = dealWithIncrement(expression, lhs, opType, operandName);
                if (!returnCode.isEmpty()) return returnCode;
            }
        }
        assign.append(getInstruction(rhs).strip()).append("\n").append(getStore(lhs));
        return assign.toString();
    }

    private String dealWithIncrement(BinaryOpInstruction expr, Operand lhs, OperationType opType, String operandName) {
        boolean leftAssignLiteral = expr.getLeftOperand().isLiteral() && !expr.getRightOperand().isLiteral();
        boolean rightAssignLiteral = expr.getRightOperand().isLiteral() && !expr.getLeftOperand().isLiteral();
        String operator = opType == OperationType.ADD ? "" : "-";
        int register = variables.get(operandName).getVirtualReg();
        if (rightAssignLiteral) {
            String leftOperandName = ((Operand) expr.getLeftOperand()).getName();
            if (leftOperandName.equals(lhs.getName())) {
                String literal = operator + ((LiteralElement) expr.getRightOperand()).getLiteral();
                return "iinc " + register + " " + literal;
            }
        }
        else if (leftAssignLiteral) {
            String rightOperandName = ((Operand) expr.getRightOperand()).getName();
            if (rightOperandName.equals(lhs.getName())) {
                String literal = operator + ((LiteralElement) expr.getLeftOperand()).getLiteral();
                return "iinc " + register + " " + literal;
            }
        }
        return "";
    }

    private String dealWithCall(CallInstruction instruction) {
        CallType invokeType = instruction.getInvocationType();
        return switch (invokeType) {
            case invokevirtual, invokespecial, invokestatic -> dealWithInvokeCall(instruction, invokeType);
            case NEW -> dealWithNewCall(instruction);
            case arraylength -> getArrayLength(instruction);
            default -> throw new NotImplementedException(instruction.getInvocationType());
        };
    }

    private String dealWithInvokeCall(CallInstruction instruction, CallType invokeType) {
        StringBuilder invoke = new StringBuilder();
        ArrayList<Element> operandList = instruction.getListOfOperands();
        String className;
        String methodName = ((LiteralElement) instruction.getSecondArg()).getLiteral().replace("\"", "");
        String invokeCode;
        String returnType = JasminUtils.getType(instruction.getReturnType());

        if (invokeType != CallType.invokestatic) {
            invoke.append(getLoad(instruction.getFirstArg()).strip()).append("\n");
            className = JasminUtils.getClassWithImports(((ClassType) instruction.getFirstArg().getType()).getName());
        }
        else {
            className = JasminUtils.getClassWithImports(((Operand) instruction.getFirstArg()).getName());
        }
        String argumentTypes = getInvokeArguments(invoke, operandList);
        invokeCode = "\t" + invokeType.toString() + " " + className + "/" + methodName + "(" + argumentTypes + ")" + 
                returnType;
        invoke.append(invokeCode);
        return invoke.toString();
    }

    private String getInvokeArguments(StringBuilder invoke, ArrayList<Element> arguments) {
        StringBuilder argumentTypes = new StringBuilder();
        for (Element argument: arguments) {
            argumentTypes.append(JasminUtils.getType(argument.getType()));
            invoke.append(getLoad(argument));
        }
        return argumentTypes.toString();
    }

    private String dealWithNewCall(CallInstruction instruction) {
        StringBuilder call = new StringBuilder();
        ElementType returnElementType = instruction.getReturnType().getTypeOfElement();
        if (returnElementType == ElementType.ARRAYREF)
            call.append(getLoad(instruction.getListOfOperands().get(0))).append("newarray int");
        else {
            String returnTypeName = ((ClassType) instruction.getReturnType()).getName();
            call.append("new ").append(JasminUtils.getClassWithImports(returnTypeName)).append("\n").append("\tdup\n");
        }
        return call.toString();
    }

    private String getArrayLength(CallInstruction instruction) {
        return getLoad(instruction.getFirstArg()) + "arraylength";
    }

    private String dealWithGoto(GotoInstruction instruction) {
        return "goto " + instruction.getLabel();
    }

    private String dealWithReturn(ReturnInstruction instruction) {
        if (!instruction.hasReturnValue()) return "\treturn";
        StringBuilder returnInst = new StringBuilder();
        Element returnResult = instruction.getOperand();
        ElementType returnType = JasminUtils.getElementType(returnResult);
        returnInst.append(getLoad(returnResult)).append(
                returnType == ElementType.INT32 || returnType == ElementType.BOOLEAN ? "\tireturn" : "\tareturn");
        return returnInst.toString();
    }

    private String dealWithPutfield(PutFieldInstruction instruction) {
        return dealWithFieldInstruction(instruction, "putfield");
    }

    private String dealWithGetfield(GetFieldInstruction instruction) {
        return dealWithFieldInstruction(instruction, "getfield");
    }

    private String dealWithFieldInstruction(FieldInstruction instruction, String instructionName) {
        String firstOp = getLoad(instruction.getFirstOperand());
        String firstOpName = ((Operand) instruction.getFirstOperand()).getName();
        String secondOpName = ((Operand) instruction.getSecondOperand()).getName();
        String classWithImports = JasminUtils.getClassWithImports(firstOpName);
        String type = JasminUtils.getType(instruction.getSecondOperand().getType());
        String thirdOp = "";
        if (instructionName.equals("putfield")) {
            thirdOp = getLoad(((PutFieldInstruction) instruction).getThirdOperand());
        }
        return firstOp + thirdOp + "\t" + instructionName + " " + classWithImports + "/" + secondOpName + " " + type;

    }

    private String dealWithUnaryOp(UnaryOpInstruction instruction) {
        return "unaryoper";
    }

    private String dealWithBinaryOp(BinaryOpInstruction instruction) {
        StringBuilder binOp = new StringBuilder();
        Element lhs = instruction.getLeftOperand();
        Element rhs = instruction.getRightOperand();
        binOp.append(getLoad(lhs)).append(getLoad(rhs)).append("\t")
                .append(JasminUtils.getOp(instruction.getOperation())).append("\n");
        return binOp.toString();
    }

    private String dealWithNop(SingleOpInstruction instruction) {
        return getLoad(instruction.getSingleOperand());
    }

    private String getLoad(Element element) {
        StringBuilder load = new StringBuilder();
        load.append("\t");
        ElementType elementType = JasminUtils.getElementType(element);

        if (element.isLiteral()) {
            LiteralElement literalElement = (LiteralElement) element;
            int literal = Integer.parseInt(literalElement.getLiteral());
            load.append(getLoadInst(literal)).append(literal == -1 ? "m1" : literal);
        }
        else if (elementType == ElementType.INT32 ||
                elementType == ElementType.BOOLEAN ||
                elementType == ElementType.STRING) {
            String register = getVarNum(((Operand) element).getName());
            ElementType varType = getOperandElementType((Operand) element);
            if (varType == ElementType.ARRAYREF) {
                ArrayOperand arrayOperand = (ArrayOperand) element;
                load.append("aload").append(register).append("\n").append(
                        getLoad(arrayOperand.getIndexOperands().get(0))).append("\tiaload");
            }
            else {
                load.append("iload").append(register);
            }
        }
        else if (elementType == ElementType.ARRAYREF ||
                elementType == ElementType.OBJECTREF ||
                elementType == ElementType.THIS) {
            String register = getVarNum(((Operand) element).getName());
            load.append("aload").append(register);
        }
        load.append("\n");
        return load.toString();
    }

    private String getStore(Operand lhs) {
        StringBuilder store = new StringBuilder();
        store.append("\t");
        ElementType lhsType = JasminUtils.getElementType(lhs);
        ElementType variableType = getOperandElementType(lhs);
        String varNum = getVarNum(lhs.getName());
        switch (lhsType) {
            case INT32, BOOLEAN -> {
                if (variableType == ElementType.ARRAYREF) {
                    store.append("iastore");
                }
                else {
                    store.append("istore").append(varNum);
                }
            }
            case ARRAYREF, OBJECTREF, THIS, STRING -> {
                store.append("astore").append(varNum);
            }
            default -> { throw new NotImplementedException(lhsType.toString()); }
        }
        return store.toString();
    }

    private String getLoadInst(int literal) {
        if (JasminUtils.isBetween(literal, -1, 5)) { return "iconst_"; }
        else if (JasminUtils.isBetween(literal, -128, 127)) { return "bipush "; }
        else if (JasminUtils.isBetween(literal, -32768, 32767)) { return "sipush "; }
        else { return "ldc "; }
    }

    private String getVarNum(String operandName) {
        StringBuilder variableNumber = new StringBuilder();
        int register = variables.get(operandName).getVirtualReg();
        variableNumber.append(register < 4 ? "_" : " ").append(register);
        return variableNumber.toString();
    }

    private ElementType getOperandElementType(Operand operand) {
        return variables.get(operand.getName()).getVarType().getTypeOfElement();
    }
}
