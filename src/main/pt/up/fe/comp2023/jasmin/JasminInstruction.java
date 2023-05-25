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
        System.out.println("instructionType = " + instructionType);
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
            case BRANCH -> { return "\t" + dealWithBranch((CondBranchInstruction) instruction); }
            default -> throw new NotImplementedException(instructionType);
        }
    }

    private String dealWithAssign(AssignInstruction instruction) {
        StringBuilder assign = new StringBuilder();
        Operand lhs = (Operand) instruction.getDest();
        Instruction rhs = instruction.getRhs();
        String operandName = lhs.getName();
        if (lhs instanceof ArrayOperand array) {
            assign.append("\taload").append(getVarNum(array.getName())).append("\n")
                    .append(getLoad(array.getIndexOperands().get(0)));
            JasminUtils.updateStackLimits(1);
        }
        else if (rhs.getInstType() == InstructionType.BINARYOPER) {
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
        JasminUtils.updateStackLimits(-operandList.size());
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
            JasminUtils.updateStackLimits(2);
        }
        return call.toString();
    }

    private String getArrayLength(CallInstruction instruction) {
        return getLoad(instruction.getFirstArg()) + "\tarraylength";
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
        JasminUtils.updateStackLimits(-2);
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
        StringBuilder unOp = new StringBuilder();
        Element operand = instruction.getOperand();
        Operation operation = instruction.getOperation();

        if (operation.getOpType() == OperationType.NOTB) {
            unOp.append(getLoadInst(1)).append("1\n").append(getLoad(operand)).append("\t").append("isub");
        }
        return unOp.toString();
    }

    private String dealWithBinaryOp(BinaryOpInstruction instruction) {
        StringBuilder binOp = new StringBuilder();
        Element lhs = instruction.getLeftOperand();
        Element rhs = instruction.getRightOperand();
        OperationType type = instruction.getOperation().getOpType();
        switch (type) {
            case LTH -> {
                return dealWithCmp(lhs, rhs, "lt", type.name());
            }
            case LTE -> {
                return dealWithCmp(lhs, rhs, "le", type.name());
            }
            case GTH -> {
                return dealWithCmp(lhs, rhs, "gt", type.name());
            }
            case GTE -> {
                return dealWithCmp(lhs, rhs, "ge", type.name());
            }
            default -> {
                binOp.append(getLoad(lhs)).append(getLoad(rhs)).append("\t")
                        .append(JasminUtils.getOp(instruction.getOperation())).append("\n");
            }
        }
        JasminUtils.updateStackLimits(-1);
        return binOp.toString();
    }

    private String dealWithCmp(Element lhs, Element rhs, String instSuffix, String type) {
        StringBuilder cmp = new StringBuilder();
        String firstLabel = type + JasminGenerator.condCounter++;
        String secondLabel = type + JasminGenerator.condCounter++;

        cmp.append(getLoad(lhs)).append(getLoad(rhs)).append("\tif_icmp").append(instSuffix).append(" ").append(firstLabel);

        cmp.append("\n\t").append(getLoadInst(0)).append("0\n")
                .append(getInstruction(new GotoInstruction(secondLabel))).append("\n").append(firstLabel).append(":\n\t")
                .append(getLoadInst(1)).append("1\n").append(secondLabel)
                .append(":\n");

        return cmp.toString();
    }

    private String dealWithNop(SingleOpInstruction instruction) {
        return getLoad(instruction.getSingleOperand());
    }

    private String dealWithBranch(CondBranchInstruction instruction) {
        StringBuilder branch = new StringBuilder();
        Instruction condition = instruction.getCondition();
        String label = instruction.getLabel();
        branch.append(getInstruction(condition).strip()).append("\n\tifne ").append(label);
        JasminUtils.updateStackLimits(-1);
        return branch.toString();
    }

    private String getLoad(Element element) {
        System.out.println("element = " + element);
        StringBuilder load = new StringBuilder();
        load.append("\t");
        ElementType elementType = JasminUtils.getElementType(element);
        System.out.println("elementType = " + elementType);

        if (element.isLiteral()) {
            LiteralElement literalElement = (LiteralElement) element;
            int literal = Integer.parseInt(literalElement.getLiteral());
            load.append(getLoadInst(literal)).append(literal == -1 ? "m1" : literal);
            JasminUtils.updateStackLimits(1);
        }
        else if (elementType == ElementType.INT32 ||
                elementType == ElementType.BOOLEAN ||
                elementType == ElementType.STRING) {
            String register = getVarNum(((Operand) element).getName());
            ElementType varType = getOperandElementType((Operand) element);
            System.out.println("varType = " + varType);
            if (varType == ElementType.ARRAYREF) {
                ArrayOperand arrayOperand = (ArrayOperand) element;
                JasminUtils.updateStackLimits(1);
                load.append("aload").append(register).append("\n").append(
                        getLoad(arrayOperand.getIndexOperands().get(0))).append("\tiaload");
                JasminUtils.updateStackLimits(-1);
            }
            else {
                load.append("iload").append(register);
                JasminUtils.updateStackLimits(1);
            }
        }
        else if (elementType == ElementType.ARRAYREF ||
                elementType == ElementType.OBJECTREF ||
                elementType == ElementType.THIS) {
            String register = getVarNum(((Operand) element).getName());
            load.append("aload").append(register);
            JasminUtils.updateStackLimits(1);
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
                    JasminUtils.updateStackLimits(-3);
                }
                else {
                    store.append("istore").append(varNum);
                    JasminUtils.updateStackLimits(-1);
                }
            }
            case ARRAYREF, OBJECTREF, THIS, STRING -> {
                store.append("astore").append(varNum);
                JasminUtils.updateStackLimits(-1);
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
