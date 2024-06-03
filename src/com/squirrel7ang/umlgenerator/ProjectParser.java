package com.squirrel7ang.umlgenerator;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.json.JSONArray;
import org.json.JSONObject;

public class ProjectParser {
    private File srcDir;
    private final ArrayList<File> javaSourceFiles = new ArrayList<>();
    private final ArrayList<CompilationUnit> asts = new ArrayList<>();
    private final ArrayList<ClassOrInterfaceDeclaration> classOrInterfaces = new ArrayList<>();
    private final HashMap<String, String> nameToId;
    private final HashMap<String, String> idToName;
    private final ArrayList<Trible> triggers;

    public ProjectParser(String path) {
        File file = new File(path);
        srcDir = file;
        nameToId = new HashMap<>();
        idToName = new HashMap<>();
        triggers = new ArrayList<>();
        walk(file);
        convertToJp();
    }

    public ProjectParser(File file) {
        srcDir = file;
        nameToId = new HashMap<>();
        idToName = new HashMap<>();
        triggers = new ArrayList<>();
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

        JSONArray ownedElements = new JSONArray();
        ownedElements.put(getUmlModel(id));
        ownedElements.put(getUmlStateMachine(id));
        json.put("ownedElements", ownedElements);

        json.put("name", "uml");
        json.put("_id", id);
        json.put("_type", "Project");
        return json;
    }

    private JSONObject getUmlStateMachine(String parentId) {
        JSONObject json = new JSONObject();
        String id = allocId();

        json.put("name", "StateMachine1");
        json.put("_id", id);
        json.put("_type", "UMLStateMachine");
        json.put("_parent", getRef(parentId));

        JSONArray ownedElements = new JSONArray();
        ownedElements.put(getStatechartDiagram(id));
        JSONArray regions = new JSONArray();
        regions.put(getUmlRegion(id));

        json.put("ownedElements", ownedElements);
        json.put("regions", regions);

        return json;
    }

    private JSONObject getUmlRegion(String parentId) {
        JSONObject json = new JSONObject();
        String id = allocId();

        json.put("_id", id);
        json.put("_type", "UMLRegion");
        json.put("_parent", getRef(parentId));


        JSONArray vertices = new JSONArray();
        JSONArray transitions = new JSONArray();
        HashMap<String, String> map = new HashMap<>(); // from nodeName to id;
        for (Trible tri: triggers) {
            String from = (String) tri.getX();
            String to = (String) tri.getY();
            String methodName = (String) tri.getZ();

            if (!map.containsKey(from)) {
                String vertexId = allocId();
                map.put(from, vertexId);
                JSONObject obj = new JSONObject();
                obj.put("name", from);
                obj.put("_id", vertexId);
                obj.put("_type", "UMLState");
                obj.put("_parent", getRef(id));
                vertices.put(obj);
            }
            if (!map.containsKey(to)) {
                String vertexId = allocId();
                map.put(to, vertexId);
                JSONObject obj = new JSONObject();
                obj.put("name", to);
                obj.put("_id", vertexId);
                obj.put("_type", "UMLState");
                obj.put("_parent", getRef(id));
                vertices.put(obj);
            }
            JSONObject transition = new JSONObject();
            String tranId = allocId();
            transition.put("name", to);
            transition.put("_id", tranId);
            transition.put("_type", "UMLTransition");
            transition.put("_parent", getRef(id));

            transition.put("source", getRef(map.get(from)));
            transition.put("target", getRef(map.get(to)));

            JSONArray trigs = new JSONArray();
            trigs.put(getUmlEvent(tranId, methodName));

            transition.put("triggers", trigs);

            transitions.put(transition);
        }

        json.put("vertices", vertices);
        json.put("transitions", transitions);

        return json;
    }

    private JSONObject getUmlEvent(String parentId, String methodName) {
        JSONObject json = new JSONObject();
        String id = allocId();

        json.put("name", methodName);
        json.put("_id", id);
        json.put("_type", "UMLEvent");
        json.put("_parent", getRef(parentId));

        return json;
    }

    private JSONObject getStatechartDiagram(String parentId) {
        JSONObject json = new JSONObject();
        String id = allocId();

        json.put("name", "StatechartDiagram1");
        json.put("_id", id);
        json.put("_type", "UMLStatechartDiagram");
        json.put("_parent", getRef(parentId));

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

        setReference(json);
        setAggregation(json);
        return json;
    }

    private void setAggregation(JSONObject umlModel) {

    }

    private void setReference(JSONObject umlModel) {
        // turn every "type: ..." "" into a reference if there are any.
        if (umlModel.has("type")) {
            String name = (String) umlModel.get("type");
            String id = nameToId.get(name);
            umlModel.put("type", getRef(id));
        }
        if (umlModel.has("source")) {
            String name = (String) umlModel.get("source");
            String id = nameToId.get(name);
            umlModel.put("source", getRef(id));
        }
        if (umlModel.has("target")) {
            String name = (String) umlModel.get("target");
            String id = nameToId.get(name);
            umlModel.put("target", getRef(id));
        }
        for (String key: umlModel.keySet()) {
            if (umlModel.get(key) instanceof JSONObject) {
                setReference((JSONObject) umlModel.get(key));
            }
            else if (umlModel.get(key) instanceof JSONArray) {
                setReference((JSONArray) umlModel.get(key));
            }
        }
    }

    private void setReference(JSONArray json) {
        for (Object obj: json) {
            if (obj instanceof JSONObject) {
                setReference((JSONObject) obj);
            }
            else if (obj instanceof JSONArray) {
                setReference((JSONArray) obj);
            }
        }
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
        nameToId.put(ci.getNameAsString(), id);
        idToName.put(id, ci.getNameAsString());

        // basic definition
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

        // operations and attributes
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

        // generalization and realization
        JSONArray ownedElements = new JSONArray();
        for (ClassOrInterfaceType node: ci.getImplementedTypes()) {
            JSONObject obj = new JSONObject();
            obj.put("_type", "UMLInterfaceRealization");
            obj.put("_id", allocId());
            obj.put("_parent", getRef(id));
            obj.put("source", ci.getNameAsString());
            obj.put("target", node.getNameAsString());
            ownedElements.put(obj);
        }
        for (ClassOrInterfaceType  node: ci.getExtendedTypes()) {
            JSONObject obj = new JSONObject();
            obj.put("_type", "UMLGeneralization");
            obj.put("_id", allocId());
            obj.put("_parent", getRef(id));
            obj.put("source", ci.getNameAsString());
            obj.put("target", node.getNameAsString());
            ownedElements.put(obj);
        }

        json.put("ownedElements", ownedElements);

        return json;
    }

    public JSONObject getUmlAttribute(FieldDeclaration dec, String parentId) {
        JSONObject json = new JSONObject();
        String id = allocId();
        json.put("_type", "UMLAttribute");
        json.put("_id", id);
        json.put("_parent", getRef(parentId));
        json.put("name", dec.getVariable(0).getNameAsString());

        setModifier(json, dec.getModifiers());

        json.put("type", dec.getVariable(0).getTypeAsString());
        return json;
    }

    public JSONObject getUmlOperation(MethodDeclaration dec, String parentId) {
        JSONObject json = new JSONObject();
        String id = allocId();

        // set the basics
        json.put("_type", "UMLOperation");
        json.put("_id", id);
        json.put("_parent", getRef(parentId));
        json.put("name", dec.getNameAsString());

        // get parameters
        JSONArray paras = new JSONArray();
        paras.put(getUmlReturnParameter(dec, id));
        for (Parameter para: dec.getParameters()) {
            paras.put(getUmlParameter(para, id));
        }
        json.put("parameters", paras);

        // set modifier of this operation
        setModifier(json, dec.getModifiers());

        // get Trigger annotation
        for (AnnotationExpr anno: dec.getAnnotations()) {
            if (anno.getNameAsString().equals("Trigger")) {
                setTriggerAnnotation(dec.getNameAsString(), (NormalAnnotationExpr) anno);
            }
            else if (anno.getNameAsString().equals("Triggers")) {
                setTriggersAnnotation(dec.getNameAsString(), (NormalAnnotationExpr) anno);
            }
        }

        return json;
    }

    private void setTriggerAnnotation(String methodName, NormalAnnotationExpr anno) {
        NodeList<MemberValuePair> pairs = anno.getPairs();
        String from = ((StringLiteralExpr) pairs.get(0).getValue()).getValue();
        if (pairs.get(1).getValue() instanceof StringLiteralExpr) {
            String to = ((StringLiteralExpr) pairs.get(1).getValue()).getValue();
            triggers.add(new Trible(from, to, methodName + "()"));
        } else if (pairs.get(1).getValue() instanceof ArrayInitializerExpr) {
            for (Node _node : ((ArrayInitializerExpr) pairs.get(1).getValue()).getValues()) {
                String to = ((StringLiteralExpr) _node).getValue();
                triggers.add(new Trible(from, to, methodName + "()"));
            }

        }
    }

    private void setTriggersAnnotation(String methodName, NormalAnnotationExpr anno) {
        NodeList<MemberValuePair> pairs = anno.getPairs();
        for (Node node: ((ArrayInitializerExpr) pairs.get(0).getValue()).getValues()) {
            setTriggerAnnotation(methodName, (NormalAnnotationExpr) node);
        }
    }

    private void setModifier(JSONObject json, NodeList<Modifier> modifiers) {
        for (Modifier modifier: modifiers) {
            switch (modifier.toString()) {
                case "private":
                    json.put("visibility", "private");
                    break;
                case "protected":
                    json.put("visibility", "protected");
                    break;
                case "public":
                    json.put("visibility", "public");
                    break;
                case "static":
                    json.put("isStatic", true);
                    break;
                case "abstract":
                    json.put("isAbstract", true);
            }
        }
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

    private class Trible {
        public Object x;
        public Object y;
        public Object z;

        public Trible(Object x, Object y, Object z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Trible() {

        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Trible)) {
                return false;
            }
            Trible obj = (Trible) object;
            return x.equals(obj.x) && y.equals(obj.y) && z.equals(obj.z);
        }

        public Object getX() {
            return x;
        }

        public Object getY() {
            return y;
        }

        public Object getZ() {
            return z;
        }

    }
}
