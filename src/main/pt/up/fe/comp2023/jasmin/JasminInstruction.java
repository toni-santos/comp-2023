package pt.up.fe.comp2023.jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;

public class JasminInstruction {
    public ClassUnit ollirClass;
    public HashMap<String, Descriptor> variables;
    public HashMap<ElementType, String> simpleTypes;

    public JasminInstruction(ClassUnit ollirClass, HashMap<String, Descriptor> variables,
                             HashMap<ElementType, String> simpleTypes) {
        this.ollirClass = ollirClass;
        this.variables = variables;
        this.simpleTypes = simpleTypes;
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
        }
        return "ERROR: Unknown instruction type.";
    }

    public String dealWithAssign(AssignInstruction instruction) {
        StringBuilder assign = new StringBuilder();
        Operand lhs = (Operand) instruction.getDest();
        Instruction rhs = instruction.getRhs();
        String operandName = lhs.getName();
        if (lhs instanceof ArrayOperand) {
            ArrayOperand operand = (ArrayOperand) lhs;
            assign.append("aload").append(getVarNum(operand.getName())).append("\n");
            assign.append(getLoad(operand.getIndexOperands().get(0)));
        }
        else {
            if (rhs.getInstType() == InstructionType.BINARYOPER) {
                BinaryOpInstruction expression = (BinaryOpInstruction) rhs;
                boolean lhsIsLiteral = expression.getLeftOperand().isLiteral();
                boolean rhsIsLiteral = expression.getRightOperand().isLiteral();
                OperationType opType = expression.getOperation().getOpType();
                if (opType == OperationType.ADD || opType == OperationType.SUB) {
                    String operator = opType == OperationType.ADD ? "" : "-";
                    int register = variables.get(operandName).getVirtualReg();
                    if (!lhsIsLiteral && rhsIsLiteral) {
                        String leftOperandName = ((Operand) expression.getLeftOperand()).getName();
                        if (leftOperandName.equals(lhs.getName())) {
                            String literal = operator + ((LiteralElement) expression.getRightOperand()).getLiteral();
                            return "iinc " + register + " " + literal;
                        }
                    }
                    else if (lhsIsLiteral && !rhsIsLiteral) {
                        String rightOperandName = ((Operand) expression.getRightOperand()).getName();
                        if (rightOperandName.equals(lhs.getName())) {
                            String literal = operator + ((LiteralElement) expression.getLeftOperand()).getLiteral();
                            return "iinc " + register + " " + literal;
                        }
                    }
                }
            }
        }

        assign.append(getInstruction(rhs).strip()).append("\n").append(getStore(lhs));
        return assign.toString();
    }

    public String dealWithCall(CallInstruction instruction) {
        CallType invokeType = instruction.getInvocationType();
        return switch (invokeType) {
            case invokevirtual, invokespecial, invokestatic -> dealWithInvokeCall(instruction, invokeType);
            case NEW -> dealWithNewCall(instruction);
            case arraylength -> getArrayLength(instruction);
            default -> throw new NotImplementedException("Not implemented: " + instruction.getInvocationType());
        };
    }

    public String dealWithInvokeCall(CallInstruction instruction, CallType invokeType) {
        StringBuilder invoke = new StringBuilder();
        ArrayList<Element> operandList = instruction.getListOfOperands();
        String className;
        String methodName = ((LiteralElement) instruction.getSecondArg()).getLiteral().replace("\"", "");
        String invokeCode;
        String returnType = JasminUtils.getType(instruction.getReturnType(), simpleTypes);
        StringBuilder argumentTypes = new StringBuilder();

        if (invokeType != CallType.invokestatic) {
            invoke.append(getLoad(instruction.getFirstArg()));
            className = getClassWithImports(((ClassType) instruction.getFirstArg().getType()).getName());
        }
        else {
            className = getClassWithImports(((Operand) instruction.getFirstArg()).getName());
        }
        argumentTypes.append("(");
        for (Element argument: operandList) {
            argumentTypes.append(JasminUtils.getType(argument.getType(), simpleTypes));
            invoke.append(getLoad(argument));
        }
        argumentTypes.append(")");
        invokeCode = invokeType.toString() + " " + className + "/" + methodName + argumentTypes + returnType
                + "\n";
        invoke.append(invokeCode);
        return invoke.toString();
    }

    public String dealWithNewCall(CallInstruction instruction) {
        StringBuilder call = new StringBuilder();
        ElementType returnElementType = instruction.getReturnType().getTypeOfElement();
        if (returnElementType == ElementType.ARRAYREF)
            call.append(getLoad(instruction.getListOfOperands().get(0))).append("newarray int");
        else {
            String returnTypeName = ((ClassType) instruction.getReturnType()).getName();
            call.append("new ").append(getClassWithImports(returnTypeName)).append("\n").append("\tdup\n");
        }
        return call.toString();
    }

    public String getArrayLength(CallInstruction instruction) {
        return getLoad(instruction.getFirstArg()) + "arraylength";
    }

    public String dealWithGoto(GotoInstruction instruction) {
        return "goto " + instruction.getLabel();
    }

    public String dealWithReturn(ReturnInstruction instruction) {
        if (!instruction.hasReturnValue()) return "return";
        StringBuilder _return = new StringBuilder();
        Element returnResult = instruction.getOperand();
        ElementType returnType = returnResult.getType().getTypeOfElement();
        _return.append(getLoad(returnResult)).append(
                returnType == ElementType.INT32 || returnType == ElementType.BOOLEAN ? "\tireturn" : "\tareturn");
        return _return.toString();
    }

    public String dealWithPutfield(PutFieldInstruction instruction) {
        StringBuilder putField = new StringBuilder();
        String firstOp = getLoad(instruction.getFirstOperand());
        String firstOpName = ((Operand) instruction.getFirstOperand()).getName();
        String secondOpName = ((Operand) instruction.getSecondOperand()).getName();
        String thirdOp = getLoad(instruction.getThirdOperand());
        String classWithImports = getClassWithImports(firstOpName);
        String type = JasminUtils.getType(instruction.getSecondOperand().getType(), simpleTypes);

        putField.append(firstOp).append(thirdOp).append("\tputfield ").append(classWithImports).append("/").append(
                secondOpName).append(" ").append(type);

        return putField.toString();
    }

    public String dealWithGetfield(GetFieldInstruction instruction) {
        StringBuilder getField = new StringBuilder();
        String firstOp = getLoad(instruction.getFirstOperand());
        String firstOpName = ((Operand) instruction.getFirstOperand()).getName();
        String secondOpName = ((Operand) instruction.getSecondOperand()).getName();
        String classWithImports = getClassWithImports(firstOpName);
        String type = JasminUtils.getType(instruction.getSecondOperand().getType(), simpleTypes);

        getField.append(firstOp).append("\tgetfield ").append(classWithImports).append("/").append(secondOpName).append(
                " ").append(type);
        return getField.toString();
    }

    public String dealWithUnaryOp(UnaryOpInstruction instruction) {
        return "unaryoper";
    }

    public String dealWithBinaryOp(BinaryOpInstruction instruction) {
        StringBuilder binOp = new StringBuilder();
        Element lhs = instruction.getLeftOperand();
        Element rhs = instruction.getRightOperand();
        binOp.append(getLoad(lhs)).append(getLoad(rhs)).append("\t").append(
                JasminUtils.getOp(instruction.getOperation()));
        binOp.append("\n");
        return binOp.toString();
    }

    public String dealWithNop(SingleOpInstruction instruction) {
        return getLoad(instruction.getSingleOperand());
    }

    public String getLoad(Element element) {
        StringBuilder load = new StringBuilder();
        load.append("\t");
        ElementType elementType = element.getType().getTypeOfElement();

        if (element.isLiteral()) {
            LiteralElement literalElement = (LiteralElement) element;
            int literal = Integer.parseInt(literalElement.getLiteral());
            load.append(getLoadInst(literal)).append(literal == -1 ? "m1" : literal);
        }
        else if (elementType == ElementType.INT32 ||
                elementType == ElementType.BOOLEAN ||
                elementType == ElementType.STRING) {
            String register = getVarNum(((Operand) element).getName());
            ElementType varType = variables.get(((Operand) element).getName()).getVarType().getTypeOfElement();
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
        /*
        String elementClass = element.getClass().getName();
        String elementLastClass = elementClass.substring(elementClass.lastIndexOf('.') + 1);
        System.out.println("elementLastClass = " + elementLastClass);
        switch (elementLastClass) {
            case "LiteralElement" -> {
                LiteralElement literalElement = ((LiteralElement) element);
                ElementType elementType = literalElement.getType().getTypeOfElement();
                System.out.println("elementType = " + elementType);
                if (elementType == ElementType.INT32 || elementType == ElementType.BOOLEAN) {
                    int literal = Integer.parseInt(literalElement.getLiteral());
                    load.append(getLoadInst(literal)).append(literal == -1 ? "m1" : literal);
                }
                else {
                    load.append("ldc ").append(literalElement.getLiteral());
                }
            }
            case "Operand" -> {
                Operand operand = (Operand) element;
                ElementType elementType = operand.getType().getTypeOfElement();
                switch (elementType) {
                    case INT32, BOOLEAN, STRING -> { load.append("iload").append(getVarNum(operand.getName(), variables)); }
                    case ARRAYREF, OBJECTREF -> { load.append("aload").append(getVarNum(operand.getName(), variables)); }
                    case THIS -> { load.append("aload_0"); }
                    default -> { load.append("Invalid operand for getLoad(): ").append(elementType); }
                }
            }
            case "ArrayOperand" -> {
                ArrayOperand arrayOperand = (ArrayOperand) element;
                load.append("aload").append(getVarNum(arrayOperand.getName(), variables)).append("\n").append(
                        getLoad(arrayOperand.getIndexOperands().get(0))).append("\tiaload");

            }
        }
        load.append("\n");
         */
        load.append("\n");
        return load.toString();
    }

    public String getStore(Operand lhs) {
        StringBuilder store = new StringBuilder();
        store.append("\t");
        ElementType lhsType = lhs.getType().getTypeOfElement();
        ElementType variableType = variables.get(lhs.getName()).getVarType().getTypeOfElement();
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
            default -> {
                store.append("Invalid lhs type for getStore(): ").append(lhsType);
            }
        }
        return store.toString();
    }

    public String getLoadInst(int literal) {
        if (JasminUtils.isBetween(literal, -1, 5)) { return "iconst_"; }
        else if (JasminUtils.isBetween(literal, -128, 127)) { return "bipush "; }
        else if (JasminUtils.isBetween(literal, -32768, 32767)) { return "sipush "; }
        else { return "ldc "; }
    }

    public String getVarNum(String operandName) {
        StringBuilder variableNumber = new StringBuilder();
        int register = variables.get(operandName).getVirtualReg();
        variableNumber.append(register < 4 ? "_" : " ").append(register);
        return variableNumber.toString();
    }

    public String getClassWithImports(String className) {
        if (className.equals("this")) { return ollirClass.getClassName(); }
        for (String importName : ollirClass.getImports()) {
            if (importName.endsWith(className)) { return importName; }
        }
        return className;
    }
}
