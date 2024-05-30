package com.squirrel7ang.umlgenerator;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;

import java.io.*;
import java.util.ArrayList;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import org.json.JSONArray;
import org.json.JSONObject;

public class ProjectParser {
    private File srcDir;
    private final ArrayList<File> javaSourceFiles = new ArrayList<>();
    private final ArrayList<CompilationUnit> asts = new ArrayList<>();
    private final ArrayList<ClassOrInterfaceDeclaration> classOrInterfaces = new ArrayList<>();

    public ProjectParser(String path) {
        File file = new File(path);
        srcDir = file;
        walk(file);
        convertToJp();
    }

    public ProjectParser(File file) {
        srcDir = file;
        walk(file);
        convertToJp();
    }

    public void outputStaruml(String outputPath) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(outputPath));
            out.write(this.toJSON().toString(2));
            out.close();
        }
        catch (IOException e) {
            System.err.println("file create fail");
        }
    }

    public void walk(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File file: files) {
                    walk(file);
                }
            }
        } else {
            if (f.toString().endsWith(".java")) {
                javaSourceFiles.add(f);
            }
        }
    }

    public void convertToJp() {
        for (File f: javaSourceFiles) {
            CompilationUnit cu;
            try {
                cu = StaticJavaParser.parse(f);
            } catch (FileNotFoundException e) {
                System.err.println("File " + f.toString() + " not found");
                continue;
            }
            asts.add(cu);
            for (Node ci: cu.getChildNodes()) {
                if (ci instanceof ClassOrInterfaceDeclaration) {
                    classOrInterfaces.add((ClassOrInterfaceDeclaration) ci);
                }
            }
        }
    }

    public JSONObject toJSON() {
        return getUmlProject();
    }

    private JSONObject getUmlProject() {
        JSONObject json = new JSONObject();
        String id = allocId();

        json.put("ownedElements", new JSONArray());
        ((JSONArray) json.get("ownedElements")).put(getUmlModel(id));

        json.put("name", "projectName");
        json.put("_id", id);
        json.put("_type", "Project");
        return json;
    }

    private JSONObject getUmlModel(String parentId) {
        JSONObject json = new JSONObject();
        String id = allocId();

        json.put("ownedElements", new JSONArray());
        JSONArray arr = json.getJSONArray("ownedElements");
        arr.put(getUmlClassDiagram(id));
        for (ClassOrInterfaceDeclaration ci: classOrInterfaces) {
            arr.put(getUmlClass(id, ci));
        }

        json.put("name", "Model");
        json.put("_id", id);
        json.put("_type", "UMLModel");
        json.put("_parent", getRef(parentId));
        return json;
    }

    private JSONObject getRef(String id) {
        JSONObject json = new JSONObject();
        json.put("$ref", id);
        return json;
    }

    private JSONObject getUmlClassDiagram(String parentId) {
        JSONObject json = new JSONObject();
        String id = allocId();
        json.put("_type", "UMLClassDiagram");
        json.put("_id", id);
        json.put("_parent", getRef(parentId));
        json.put("name", "Main");
        json.put("defaultDiagram", true);

        return json;
    }

    private JSONObject getUmlClass(String parentId, ClassOrInterfaceDeclaration ci) {
        JSONObject json = new JSONObject();
        String id = allocId();
        if (ci.isInterface()) {
            json.put("_type", "UMLInterface");
        }
        else {
            json.put("_type", "UMLClass");
        }
        json.put("_id", id);
        json.put("_parent", getRef(parentId));
        json.put("name", ci.getName());

        JSONArray attrs = new JSONArray();
        JSONArray ops = new JSONArray();

        for (int i = 0; i < ci.getMembers().size(); i++) {
            if (ci.getMember(i) instanceof FieldDeclaration) {
                attrs.put(getUmlAttribute((FieldDeclaration) ci.getMember(i), id));
            }
            else if (ci.getMember(i) instanceof MethodDeclaration) {
                ops.put(getUmlOperation((MethodDeclaration) ci.getMember(i), id));
            }
        }

        json.put("attributes", attrs);
        json.put("operations", ops);

        return json;
    }

    public JSONObject getUmlAttribute(FieldDeclaration dec, String parentId) {
        JSONObject json = new JSONObject();
        String id = allocId();
        json.put("_type", "UMLAttribute");
        json.put("_id", id);
        json.put("_parent", getRef(parentId));
        json.put("name", dec.getVariable(0));
        for (Modifier mf: dec.getModifiers()) {
            if (mf.toString().startsWith("private")) {
                json.put("visibility", "private");
                break;
            }
            else if (mf.toString().startsWith("public")) {
                json.put("visibility", "public");
                break;
            }
        }
        json.put("type", dec.getVariable(0).getType());
        return json;
    }

    public JSONObject getUmlOperation(MethodDeclaration dec, String parentId) {
        JSONObject json = new JSONObject();
        String id = allocId();
        json.put("_type", "UMLOperation");
        json.put("_id", id);
        json.put("_parent", getRef(parentId));
        json.put("name", dec.getName().toString());

        JSONArray paras = new JSONArray();
        paras.put(getUmlReturnParameter(dec, id));
        for (Parameter para: dec.getParameters()) {
            paras.put(getUmlParameter(para, id));
        }
        json.put("parameters", paras);

        return json;
    }

    public JSONObject getUmlReturnParameter(MethodDeclaration dec, String parentId) {
        JSONObject json = new JSONObject();
        String id = allocId();
        json.put("_type", "UMLParameter");
        json.put("_id", id);
        json.put("_parent", getRef(parentId));
        json.put("type", dec.getType().toString());
        json.put("direction", "return");
        return json;
    }

    public JSONObject getUmlParameter(Parameter para, String parentId) {
        JSONObject json = new JSONObject();
        String id = allocId();
        json.put("_type", "UMLParameter");
        json.put("_id", id);
        json.put("_parent", getRef(parentId));
        json.put("name", para.getName());
        json.put("type", para.getType().toString());

        return json;
    }

    private static Integer _id = 0x11111111;
    public static String allocId() {
        return (_id++).toString();
    }
}
