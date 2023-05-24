package pt.up.fe.comp2023;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2023.ollir.SimpleOllir;
import pt.up.fe.comp2023.ollir.optimize.ConstantPropagation;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class Launcher {

    public static void main(String[] args) {
        // Setups console logging and other things
        SpecsSystem.programStandardInit();

        // Parse arguments as a map with predefined options
        var config = parseArgs(args);

        // Get input file
        File inputFile = new File(config.get("inputFile"));

        // Check if file exists
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + inputFile + "'.");
        }

        // Read contents of input file
        String code = SpecsIo.read(inputFile);

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        // Parse stage
        JmmParserResult parserResult = parser.parse(code, config);

        // Check if there are parsing errors
        TestUtils.noErrors(parserResult.getReports());

        // ... add remaining stages
        SimpleAnalysis analysis = new SimpleAnalysis();
        JmmSemanticsResult semanticsResult = analysis.semanticAnalysis(parserResult);

        TestUtils.noErrors(semanticsResult.getReports());

        if (config.get("optimize").equals("true")) {
            Optimizer optimizer = new Optimizer();
            semanticsResult = optimizer.optimize(semanticsResult);
        }

        SimpleOllir ollir = new SimpleOllir();
        OllirResult ollirResult = ollir.toOllir(semanticsResult);

        TestUtils.noErrors(ollirResult.getReports());
    }

    private static Map<String, String> parseArgs(String[] args) {
        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // Check if there is at least one argument
        if (args.length != 1) {
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
        }

        Map<String, String> config = new HashMap<>();

        config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        for (String arg: args) {
            switch (arg) {
                case "-o" -> {
                    config.put("optimize", "true");
                }
            }
        }


        return config;
    }

}
