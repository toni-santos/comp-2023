package pt.up.fe.comp2023.jasmin;

import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

public class SimpleJasmin implements JasminBackend {
    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {

        JasminGenerator generator = new JasminGenerator(ollirResult.getOllirClass());;

        String jasminCode = generator.generate();
        return new JasminResult(ollirResult.getOllirClass().getClassName(), jasminCode, ollirResult.getReports());
    }

}
