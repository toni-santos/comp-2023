package pt.up.fe.comp2023.jasmin;

import org.specs.comp.ollir.*;

import java.util.ArrayList;
import java.util.HashMap;

public class JasminMethod {
    public Method method;
    public String access;
    public boolean isStatic;
    public boolean isFinal;
    public boolean isConstruct;
    public StringBuilder methodCode;
    public ClassUnit ollirClass;

    public JasminMethod(ClassUnit ollirClass) {
        this.methodCode = new StringBuilder();
        this.ollirClass = ollirClass;
    }

    public String getMethod(int i) {
        this.method = ollirClass.getMethod(i);
        this.isStatic = method.isStaticMethod();
        this.isFinal = method.isFinalMethod();
        this.isConstruct = method.isConstructMethod();
        this.access = getAccessModifier(method);

        methodCode.append(".method ")
                .append(getMethodScope(method))
                .append(getMethodBody(method));
        methodCode.append(".end method\n");
        return methodCode.toString();
    }

    private String getMethodScope(Method method) {
        StringBuilder scope = new StringBuilder(access).append(" ");
        if (isStatic) {
            scope.append("static ");
        }
        if (isFinal) {
            scope.append("final ");
        }
        if (isConstruct) {
            scope.append("<init>(");
        }
        else {
            scope.append(method.getMethodName()).append("(");
        }
        for (Element parameter: method.getParams()){
            scope.append(JasminUtils.getType(parameter.getType()));
        }
        scope.append(")").append(JasminUtils.getType(method.getReturnType())).append("\n");
        return scope.toString();
    }

    private String getMethodBody(Method method) {
        StringBuilder body = new StringBuilder();
        ArrayList<Instruction> instructions = method.getInstructions();
        body.append(getStackLimit());
        for (Instruction instruction : instructions) {
            body.append(getInstructionWithLabels(instruction));
        }
        if (instructions.get(instructions.size() - 1).getInstType() != InstructionType.RETURN) body.append("\n\treturn\n");
        return body.toString();
    }

    private String getStackLimit() {
        return "\t.limit stack 99\n\t.limit locals 99\n";
    }

    private String getInstructionWithLabels(Instruction instruction) {
        StringBuilder inst = new StringBuilder();
        HashMap<String, Descriptor> variables = method.getVarTable();
        JasminInstruction jasminInstruction = new JasminInstruction(ollirClass, variables);
        for (String label : method.getLabels(instruction)) {
            inst.append(label).append(":\n");
        }
        inst.append(jasminInstruction.getInstruction(instruction)).append("\n");
        if (instruction.getInstType() == InstructionType.CALL) {
            ElementType returnType = ((CallInstruction) instruction).getReturnType().getTypeOfElement();
            if (returnType != ElementType.VOID || ((CallInstruction) instruction).getInvocationType() == CallType.invokespecial) {
                inst.append("\tpop\n");
            }
        }

        return inst.toString();
    }

    private String getAccessModifier(Method method) {
        if (method.getMethodAccessModifier() == AccessModifiers.DEFAULT) {
            return this.isConstruct ? "public" : "private";
        }
        return method.getMethodAccessModifier().name().toLowerCase();
    }
}
