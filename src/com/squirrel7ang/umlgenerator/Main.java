package com.squirrel7ang.umlgenerator;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        File dir = null;
        String outputPath = "uml.mdj";
        if (args.length == 0) {
            dir = new File("./");
        }
        else if (args.length == 1) {
            dir = new File(args[0]);
        }
        else if (args.length == 2) {
            dir = new File(args[0]);
            outputPath = args[1];
            if (outputPath.endsWith("/") || outputPath.endsWith("\\")) {
                outputPath += "uml.mdj";
            }
        }
        else {
            System.err.println("no more than two arguments is expected, while "
                    + args.length + " arguments are detected");
            System.err.println("Example1: java -jar umlgenerator.jar");
            System.err.println("Example2: java -jar umlgenerator.jar ./src");
            System.err.println("Example3: java -jar umlgenerator.jar ./src ./uml.mdj");
            System.exit(-1);
        }
        ProjectParser pp = new ProjectParser(dir);

        pp.outputStaruml(outputPath);
    }
}
