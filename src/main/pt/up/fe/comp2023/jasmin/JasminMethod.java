package pt.up.fe.comp2023.jasmin;

import org.specs.comp.ollir.*;

import java.util.ArrayList;
import java.util.HashMap;

public class JasminMethod {
    public Method method;
    public int instructionIndex = 0;
    public String access;
    public boolean isStatic;
    public boolean isFinal;
    public boolean isConstruct;
    public StringBuilder methodCode;
    public ClassUnit ollirClass;
    public HashMap<ElementType, String> simpleTypes;

    public JasminMethod(ClassUnit ollirClass, HashMap<ElementType, String> types) {
        this.methodCode = new StringBuilder();
        this.ollirClass = ollirClass;
        this.simpleTypes = types;
    }

    public String getMethod(int i) {
        this.method = ollirClass.getMethod(i);
        this.isStatic = method.isStaticMethod();
        this.isFinal = method.isFinalMethod();
        this.isConstruct = method.isConstructMethod();
        this.access = getAccessModifier(method);

        methodCode.append(".method ").append(getMethodScope(method)).append(getMethodBody(method));
        methodCode.append(".end method\n");
        return methodCode.toString();
    }

    public String getMethodScope(Method method) {
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
            scope.append(JasminUtils.getType(parameter.getType(), simpleTypes));
        }
        scope.append(")").append(JasminUtils.getType(method.getReturnType(), simpleTypes)).append("\n");
        return scope.toString();
    }

    public String getMethodBody(Method method) {
        StringBuilder body = new StringBuilder();
        ArrayList<Instruction> instructions = method.getInstructions();
        Instruction instruction;
        HashMap<String, Descriptor> variables = method.getVarTable();
        JasminInstruction jasminInstruction = new JasminInstruction(ollirClass, variables, simpleTypes);
        body.append("\t.limit stack 99\n\t.limit locals 99\n");
        for (instructionIndex = 0; instructionIndex < instructions.size(); instructionIndex++) {
            instruction = instructions.get(instructionIndex);
            for (String label : method.getLabels(instruction)) {
                body.append(label).append(":\n");
            }
            body.append(jasminInstruction.getInstruction(instruction)).append("\n");
            if (instruction.getInstType() == InstructionType.CALL) {
                ElementType returnType = ((CallInstruction) instruction).getReturnType().getTypeOfElement();
                if (returnType != ElementType.VOID || ((CallInstruction) instruction).getInvocationType() == CallType.invokespecial) {
                    body.append("\tpop\n");
                }
            }
        }
        return body.toString();
    }

    public String getAccessModifier(Method method) {
        if (method.getMethodAccessModifier() == AccessModifiers.DEFAULT) {
            return this.isConstruct ? "public" : "private";
        }
        return method.getMethodAccessModifier().name().toLowerCase();
    }
}
