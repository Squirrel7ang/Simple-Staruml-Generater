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
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import org.json.JSONArray;
import org.json.JSONObject;

public class ProjectParser {
    private final File srcDir;
    private final ArrayList<File> javaSourceFiles = new ArrayList<>();
    private final ArrayList<CompilationUnit> asts = new ArrayList<>();
    private final ArrayList<ClassOrInterfaceDeclaration> classOrInterfaces = new ArrayList<>();
    private final HashMap<String, String> nameToId; // from the Name of a class or interface to its id of its JSONObject
    private final HashMap<String, JSONObject> nameToJson; // from the name of a class or interface to its JSONObject
    private final HashMap<String, String> stateNameToId = new HashMap<>(); // from stateName to the id of the UMLState JSONObject;
    private final HashMap<String, String> lifelineNameToId = new HashMap<>(); // from stateName to the id of the UMLState JSONObject;
    private final ArrayList<Duo<String, String>> generalizations;
    private final ArrayList<Duo<String, String>> realizations;
    private final ArrayList<Duo<String, String>> aggregations;
    private final ArrayList<Trio<String, String, String>> triggers;
    private final ArrayList<Trio<String, String, String>> sendMessages;

    public ProjectParser(String path) {
        File file = new File(path);
        srcDir = file;
        nameToId = new HashMap<>();
        nameToJson = new HashMap<>();
        generalizations = new ArrayList<>();
        realizations = new ArrayList<>();
        aggregations = new ArrayList<>();
        triggers = new ArrayList<>();
        sendMessages = new ArrayList<>();
        walk(file);
        convertToJp();
    }

    public ProjectParser(File file) {
        srcDir = file;
        nameToId = new HashMap<>();
        nameToJson = new HashMap<>();
        generalizations = new ArrayList<>();
        realizations = new ArrayList<>();
        aggregations = new ArrayList<>();
        triggers = new ArrayList<>();
        sendMessages = new ArrayList<>();
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
        if (!triggers.isEmpty()) {
            ownedElements.put(getUmlStateMachine(id));
        }
        if (!sendMessages.isEmpty()) {
            ownedElements.put(getUmlCollaboration(id));
        }
        json.put("ownedElements", ownedElements);

        json.put("name", "uml");
        json.put("_id", id);
        json.put("_type", "Project");
        return json;
    }

    private JSONObject getUmlCollaboration(String parentId) {
        JSONObject json = new JSONObject();
        String id = allocId();

        json.put("name", "Collaboration1");
        json.put("_id", id);
        json.put("_type", "UMLCollaboration");
        json.put("_parent", getRef(parentId));

        JSONArray ownedElements = new JSONArray();
        ownedElements.put(getUmlInteraction(id));
        json.put("ownedElements", ownedElements);

        return json;
    }

    private JSONObject getUmlInteraction(String parentId) {
        JSONObject json = new JSONObject();
        String id = allocId();

        json.put("name", "Interaction1");
        json.put("_id", id);
        json.put("_type", "UMLInteraction");
        json.put("_parent", getRef(parentId));

        JSONArray ownedElements = new JSONArray();
        ownedElements.put(getSequenceDiagram(id));
        json.put("ownedElements", ownedElements);

        JSONArray messages = new JSONArray();
        JSONArray participants = new JSONArray();
        for (Trio<String, String, String> tri: sendMessages) {
            String from = tri.getX();
            String to = tri.getY();
            String methodName = tri.getZ();

            // add participants(lifeline) if necessary
            if (!lifelineNameToId.containsKey(from)) {
                participants.put(getUmlLifeline(id, from));
            }
            if (!lifelineNameToId.containsKey(to)) {
                participants.put(getUmlLifeline(id, to));
            }

            // add message
            JSONObject message = new JSONObject();
            String messageId = allocId();
            message.put("name", methodName);
            message.put("_id", messageId);
            message.put("_type", "UMLMessage");
            message.put("_parent", getRef(id));

            message.put("source", getRef(lifelineNameToId.get(from)));
            message.put("target", getRef(lifelineNameToId.get(to)));

            messages.put(message);
        }

        json.put("messages", messages);
        json.put("participants", participants);

        return json;
    }

    private JSONObject getUmlLifeline(String parentId, String lifelineName) {
        String lifelineId = allocId();
        lifelineNameToId.put(lifelineName, lifelineId);
        JSONObject obj = new JSONObject();
        obj.put("name", lifelineName);
        obj.put("_id", lifelineId);
        obj.put("_parent", getRef(parentId));
        obj.put("_type", "UMLLifeline");
        obj.put("isMultiInstance", false);
        return obj;
    }

    private JSONObject getSequenceDiagram(String parentId) {
        JSONObject json = new JSONObject();
        String id = allocId();

        json.put("name", "SequenceDiagram1");
        json.put("_id", id);
        json.put("_type", "UMLSequenceDiagram");
        json.put("_parent", getRef(parentId));

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
        for (Trio<String, String, String> tri: triggers) {
            String from = tri.getX();
            String to = tri.getY();
            String methodName = tri.getZ();

            // add vertices if necessary
            if (!stateNameToId.containsKey(from)) {
                vertices.put(getUmlState(id, from));
            }
            if (!stateNameToId.containsKey(to)) {
                vertices.put(getUmlState(id, to));
            }

            // add Transition
            JSONObject transition = new JSONObject();
            String tranId = allocId();
            transition.put("name", methodName);
            transition.put("_id", tranId);
            transition.put("_type", "UMLTransition");
            transition.put("_parent", getRef(id));

            transition.put("source", getRef(stateNameToId.get(from)));
            transition.put("target", getRef(stateNameToId.get(to)));

            JSONArray trigs = new JSONArray();
            trigs.put(getUmlEvent(tranId, methodName));

            transition.put("triggers", trigs);

            transitions.put(transition);
        }

        json.put("vertices", vertices);
        json.put("transitions", transitions);

        return json;
    }

    private JSONObject getUmlState(String parentId, String stateName) {
        String vertexId = allocId();
        stateNameToId.put(stateName, vertexId);
        JSONObject obj = new JSONObject();
        obj.put("name", stateName);
        obj.put("_id", vertexId);
        obj.put("_parent", getRef(parentId));
        if (stateName.equals("InitState")) {
            obj.put("_type", "UMLPseudostate");
            obj.put("kind", "initial");
        }
        else if (stateName.equals("FinalState")) {
            obj.put("_type", "UMLFinalState");

        }
        else {
            obj.put("_type", "UMLState");
        }
        return obj;
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
        setRelation();
        return json;
    }

    private void setRelation() {
        setGeneralization();
        setRealization();
        setAggregation();
    }

    private void setGeneralization() {
        for (Duo<String, String> duo: generalizations) {
            String name1 = duo.getX();
            String name2 = duo.getY();
            JSONObject class1 = nameToJson.get(name1);
            JSONObject class2 = nameToJson.get(name2);
            if (class1 == null || class2 == null) {
                continue;
            }
            JSONObject obj = new JSONObject();
            obj.put("_type", "UMLGeneralization");
            obj.put("_parent", getRef(class1.getString("_id")));
            obj.put("_id", allocId());
            obj.put("source", getRef(class1.getString("_id")));
            obj.put("target", getRef(class2.getString("_id")));
            addOwnedElements(class1, obj);
        }
    }

    private void setRealization() {
        for (Duo<String, String> duo: realizations) {
            String name1 = duo.getX();
            String name2 = duo.getY();
            JSONObject class1 = nameToJson.get(name1);
            JSONObject class2 = nameToJson.get(name2);
            if (class1 == null || class2 == null) {
                continue;
            }
            JSONObject obj = new JSONObject();
            obj.put("_type", "UMLRealization");
            obj.put("_parent", getRef(class1.getString("_id")));
            obj.put("_id", allocId());
            obj.put("source", getRef(class1.getString("_id")));
            obj.put("target", getRef(class2.getString("_id")));
            addOwnedElements(class1, obj);
        }
    }

    private void setAggregation() {
        for (Duo<String, String> duo: aggregations) {
            String name1 = duo.getX();
            String name2 = duo.getY();
            JSONObject class1 = nameToJson.get(name1);
            JSONObject class2 = nameToJson.get(name2);
            if (class1 == null || class2 == null) {
                continue;
            }
            JSONObject obj = new JSONObject();
            obj.put("_type", "UMLAssociation");
            obj.put("_parent", getRef(class1.getString("_id")));
            String objId = allocId();
            obj.put("_id", objId);

            JSONObject end1 = new JSONObject();
            end1.put("_type", "UMLAssociationEnd");
            end1.put("_parent", getRef(objId));
            end1.put("_id", allocId());
            end1.put("reference", getRef(class1.getString("_id")));
            obj.put("end1", end1);

            JSONObject end2 = new JSONObject();
            end2.put("_type", "UMLAssociationEnd");
            end2.put("_parent", getRef(objId));
            end2.put("_id", allocId());
            end2.put("reference", getRef(class2.getString("_id")));
            end2.put("aggregation", "shared");
            obj.put("end2", end2);

            addOwnedElements(class1, obj);
        }
    }

    private void addOwnedElements(JSONObject classJson, JSONObject element) {
        if (!classJson.has("ownedElements")) {
            classJson.put("ownedElements", new JSONArray());
        }
        ((JSONArray) classJson.get("ownedElements")).put(element);
    }

    private void setReference(JSONObject umlModel) {
        // turn every "type: ..." "" into a reference if there are any.
        for (String key: umlModel.keySet()) {
            if (key.equals("type")) {
                String name = (String) umlModel.get("type");
                String id = nameToId.get(name);
                if (id != null) {
                    umlModel.put("type", getRef(id));
                }
            }
            else if (key.equals("source")) {
                String name = (String) umlModel.get("source");
                String id = nameToId.get(name);
                if (id != null) {
                    umlModel.put("source", getRef(id));
                }
            }
            else if (key.equals("target")) {
                String name = (String) umlModel.get("target");
                String id = nameToId.get(name);
                if (id != null) {
                    umlModel.put("target", getRef(id));
                }
            }
            else if (umlModel.get(key) instanceof JSONObject) {
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
        nameToJson.put(ci.getNameAsString(), json);

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
                attrs.put(getUmlAttribute((FieldDeclaration) ci.getMember(i), ci.getNameAsString(), id));
            }
            else if (ci.getMember(i) instanceof MethodDeclaration) {
                ops.put(getUmlOperation((MethodDeclaration) ci.getMember(i), id));
            }
        }

        json.put("attributes", attrs);
        json.put("operations", ops);

        // store generalization and realization
        for (ClassOrInterfaceType node: ci.getImplementedTypes()) {
            realizations.add(new Duo<>(ci.getNameAsString(), node.getNameAsString()));
        }
        for (ClassOrInterfaceType  node: ci.getExtendedTypes()) {
            generalizations.add(new Duo<>(ci.getNameAsString(), node.getNameAsString()));
        }

        return json;
    }

    public JSONObject getUmlAttribute(FieldDeclaration dec, String classOrInterfaceName, String parentId) {
        JSONObject json = new JSONObject();
        String id = allocId();
        json.put("_type", "UMLAttribute");
        json.put("_id", id);
        json.put("_parent", getRef(parentId));
        json.put("name", dec.getVariable(0).getNameAsString());

        setModifier(json, dec.getModifiers());

        json.put("type", dec.getVariable(0).getType().toString());

        if (dec.getVariable(0).getType() instanceof ClassOrInterfaceType) {
            ClassOrInterfaceType _type = ((ClassOrInterfaceType) dec.getVariable(0).getType());
            // store aggregations
            storeAggregation(classOrInterfaceName, _type);
        }

        return json;
    }

    private void storeAggregation(String classOrInterfaceName, ClassOrInterfaceType type) {
        String name = type.getNameAsString();
        aggregations.add(new Duo<>(name, classOrInterfaceName));
        if (type.getTypeArguments().isPresent()) {
            for (Type t: type.getTypeArguments().get()) {
                storeAggregation(classOrInterfaceName, (ClassOrInterfaceType) t);
            }
        }

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

        for (AnnotationExpr anno: dec.getAnnotations()) {
            // get Trigger annotation
            if (anno.getNameAsString().equals("Trigger")) {
                setTriggerAnnotation(dec.getNameAsString(), (NormalAnnotationExpr) anno);
            }
            else if (anno.getNameAsString().equals("Triggers")) {
                setTriggersAnnotation(dec.getNameAsString(), (NormalAnnotationExpr) anno);
            }
            // get sendMessage annotation
            else if (anno.getNameAsString().equals("SendMessage")) {
                setSendMessageAnnotation(dec.getNameAsString(), (NormalAnnotationExpr) anno);
            }
            else if (anno.getNameAsString().equals("SendMessages")) {
                setSendMessageAnnotations(dec.getNameAsString(), (NormalAnnotationExpr) anno);
            }
        }
        return json;
    }

    private void setSendMessageAnnotation(String methodName, NormalAnnotationExpr anno) {
        NodeList<MemberValuePair> pairs = anno.getPairs();
        String from = ((StringLiteralExpr) pairs.get(0).getValue()).getValue();
        if (pairs.get(1).getValue() instanceof StringLiteralExpr) {
            String to = ((StringLiteralExpr) pairs.get(1).getValue()).getValue();
            sendMessages.add(new Trio<>(from, to, methodName + "()"));
        } else if (pairs.get(1).getValue() instanceof ArrayInitializerExpr) {
            for (Node _node : ((ArrayInitializerExpr) pairs.get(1).getValue()).getValues()) {
                String to = ((StringLiteralExpr) _node).getValue();
                sendMessages.add(new Trio<>(from, to, methodName + "()"));
            }
        }
    }

    private void setSendMessageAnnotations(String methodName, NormalAnnotationExpr anno) {
        NodeList<MemberValuePair> pairs = anno.getPairs();
        for (Node node: ((ArrayInitializerExpr) pairs.get(0).getValue()).getValues()) {
            setTriggerAnnotation(methodName, (NormalAnnotationExpr) node);
        }
    }

    private void setTriggerAnnotation(String methodName, NormalAnnotationExpr anno) {
        NodeList<MemberValuePair> pairs = anno.getPairs();
        String from = ((StringLiteralExpr) pairs.get(0).getValue()).getValue();
        if (pairs.get(1).getValue() instanceof StringLiteralExpr) {
            String to = ((StringLiteralExpr) pairs.get(1).getValue()).getValue();
            triggers.add(new Trio<>(from, to, methodName + "()"));
        } else if (pairs.get(1).getValue() instanceof ArrayInitializerExpr) {
            for (Node _node : ((ArrayInitializerExpr) pairs.get(1).getValue()).getValues()) {
                String to = ((StringLiteralExpr) _node).getValue();
                triggers.add(new Trio<>(from, to, methodName + "()"));
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
        for (Modifier _modifier: modifiers) {
            String modifier = _modifier.toString();
            if (modifier.startsWith("private")) {
                json.put("visibility", "private");
            }
            else if (modifier.startsWith("protected")) {
                json.put("visibility", "protected");
            }
            else if (modifier.startsWith("public")) {
                json.put("visibility", "public");
            }
            else if (modifier.startsWith("static")) {
                json.put("isStatic", true);
            }
            else if (modifier.startsWith("abstract")) {
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

    private class Duo<X, Y> {
        public X x;
        public Y y;

        public Duo(X x, Y y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Duo)) {
                return false;
            }
            Duo obj = (Duo) object;
            return x.equals(obj.x) && y.equals(obj.y);
        }

        public X getX() {
            return x;
        }

        public Y getY() {
            return y;
        }
    }

    private class Trio<X, Y, Z> {
        public X x;
        public Y y;
        public Z z;

        public Trio(X x, Y y, Z z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Trio)) {
                return false;
            }
            Trio obj = (Trio) object;
            return x.equals(obj.x) && y.equals(obj.y) && z.equals(obj.z);
        }

        public X getX() {
            return x;
        }

        public Y getY() {
            return y;
        }

        public Z getZ() {
            return z;
        }

    }
}
