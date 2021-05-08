package gui.graph.visualisation;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import gui.graph.dag.Node;
import gui.graph.data.GraphOperator;
import gui.graph.data.Operator;
import gui.spe.ParsedSPE;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

abstract class Visualiser {
    final ParsedSPE parsedSPE;
    final Map<String, Set<String>> connected;
    final Set<String> queryVariables;
    final List<String> allConnectedOperators;
    final Map<String, GraphOperator> operators;
    final Map<String, String> variableClasses;

    protected Visualiser(@Nonnull ParsedSPE parsedSPE) {
        this.parsedSPE = parsedSPE;
        queryVariables = new HashSet<>();
        allConnectedOperators = new LinkedList<>();
        operators = new HashMap<>();
        connected = new HashMap<>();
        variableClasses = new HashMap<>();
    }

    @Nonnull
    abstract List<Pair<Node<GraphOperator>, VisInfo>> fixList(List<Pair<Node<GraphOperator>, VisInfo>> list);

    @Nonnull
    abstract VoidVisitorAdapter<Void> methodParserFindVariables(List<Pair<Node<GraphOperator>, VisInfo>> methodData, String fileName, ClassOrInterfaceDeclaration c, MethodDeclaration method);

    @Nonnull
    abstract VoidVisitorAdapter<Void> methodParserFindDefinitions(List<Pair<Node<GraphOperator>, VisInfo>> methodData, String fileName, ClassOrInterfaceDeclaration c, MethodDeclaration method);

    @Nonnull
    List<ClassOrInterfaceDeclaration> findClasses(CompilationUnit cu) {
        List<ClassOrInterfaceDeclaration> classes = new LinkedList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                System.out.println(n.getName()); // Name of class
                classes.add(n);
                super.visit(n, arg);
            }
        }, null);

        return classes;
    }

    @Nonnull
    List<Pair<Node<GraphOperator>, VisInfo>> getClassData(String fileName, ClassOrInterfaceDeclaration c) {
        List<Pair<Node<GraphOperator>, VisInfo>> methodData = new LinkedList<>();
        for (MethodDeclaration method : c.getMethods()) {
            // Make the visitor go through everything inside the method.
            method.accept(methodParserFindVariables(methodData, fileName, c, method), null);
            method.accept(methodParserFindDefinitions(methodData, fileName, c, method), null);
        }

        return methodData;
    }

    void addToConnectedMap(String from, String to) {
        if (connected.containsKey(from)) {
            connected.get(from).add(to);
        } else {
            Set<String> s = new HashSet<>();
            s.add(to);
            connected.put(from, s);
        }
        System.out.println("from = " + from + ", to = " + to);
        if (!allConnectedOperators.contains(from)) {
            allConnectedOperators.add(from);
        }
        if (!allConnectedOperators.contains(to)) {
            allConnectedOperators.add(to);
        }
    }

    @Nullable
    VisInfo.VariableInfo findLocalVariableInfo(com.github.javaparser.ast.Node n) {
        if (n instanceof VariableDeclarator) {
            return extractData((VariableDeclarator) n, n.toString());
        }
        if (n.getParentNode().isPresent()) {
            com.github.javaparser.ast.Node parent = n.getParentNode().get();
            String s = parent.toString();
            if (s.startsWith("{")) { // no variable
                String[] sp = n.toString().split("\\.", 2);
                return new VisInfo.VariableInfo(null, sp[0], null, sp[1], Operator.class, null);
            } else if (parent instanceof VariableDeclarator) { // we found a variable
                VariableDeclarator de = (VariableDeclarator) parent;
                //System.out.println("variableDec, parent is " + de.getParentNode().get().getClass() + ", " + de.getParentNode().get().getParentNode().get().getClass());
                return extractData(de, s);
            } else { // no variable yet, search from parent
                return findLocalVariableInfo(parent);
            }
        }
        return null;
    }

    private VisInfo.VariableInfo extractData(VariableDeclarator de, String s) {
        String[] strings = s.split("=", 2);
        /*if (strings[0].split(" ").length > 2) { // not correct equals sign
            return findLocalVariableInfo(parent);
        } else {*/
        final String variableName = de.getNameAsString();
        final String varData = strings[1].trim();
        final String[] varDataDot = varData.split("\\.", 2);
        final String calledWithVar = varDataDot[0].trim();
        final String varClass = de.getTypeAsString(); //getTypeFor(variableName);
        final Pair<Class<? extends GraphOperator>, String> operator = findOperator(varDataDot[1]);
        //System.out.println("parent2 = " + parent);
        return new VisInfo.VariableInfo(variableName, calledWithVar, varClass, strings[1].trim(), operator.getKey(), operator.getValue());
        //}
    }

    Pair<Class<? extends GraphOperator>, String> getClassStringPair(String afterDot) {
        afterDot = afterDot.toLowerCase();
        Map<String, Pair<Class<? extends GraphOperator>, String>> codeToOpMap = parsedSPE.getCodeToOpMap();
        for (String key : codeToOpMap.keySet()) {
            if (afterDot.startsWith(key.toLowerCase())) {
                return codeToOpMap.get(key);
            }
        }
        return new Pair<>(Operator.class, null);
    }

    @Nonnull
    Pair<Class<? extends GraphOperator>, String> findOperator(String afterDot) {
        return getClassStringPair(afterDot);
    }

    @Nullable
    private String getTypeFor(@Nonnull String variable) {
        return variableClasses.get(variable.trim());
    }

}
