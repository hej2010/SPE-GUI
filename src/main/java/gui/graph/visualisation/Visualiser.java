package gui.graph.visualisation;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import gui.graph.dag.Node;
import gui.graph.data.GraphOperator;
import gui.spe.ParsedSPE;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import java.util.*;

abstract class Visualiser {
    final ParsedSPE parsedSPE;
    final Map<String, Set<String>> connected;
    final Set<String> queryVariables, allConnectedOperators;
    final Map<String, GraphOperator> operators;
    final Map<String, String> variableClasses;

    protected Visualiser(@Nonnull ParsedSPE parsedSPE) {
        this.parsedSPE = parsedSPE;
        queryVariables = new HashSet<>();
        allConnectedOperators = new HashSet<>();
        operators = new HashMap<>();
        connected = new HashMap<>();
        variableClasses = new HashMap<>();
    }

    @Nonnull
    abstract List<Pair<Node<GraphOperator>, VisInfo>> fixList(List<Pair<Node<GraphOperator>, VisInfo>> list);

    @Nonnull
    abstract VoidVisitorAdapter<Void> methodParserInit(List<Pair<Node<GraphOperator>, VisInfo>> methodData, String fileName, ClassOrInterfaceDeclaration c, MethodDeclaration method);
    @Nonnull
    abstract VoidVisitorAdapter<Void> methodParser(List<Pair<Node<GraphOperator>, VisInfo>> methodData, String fileName, ClassOrInterfaceDeclaration c, MethodDeclaration method);

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
        //List<Pair<Node<GraphOperator>, VisInfo>> data = new LinkedList<>();
        List<Pair<Node<GraphOperator>, VisInfo>> methodData = new LinkedList<>();
        for (MethodDeclaration method : c.getMethods()) {
            // Make the visitor go through everything inside the method.
            //System.out.println(method.findAll(VariableDeclarator.class));
            method.accept(methodParserInit(methodData, fileName, c, method), null);
            method.accept(methodParser(methodData, fileName, c, method), null);

            /*BlockStmt block = method.getBody().orElse(null); // TODO move to flink (chaining, find links)
            if (block == null) {
                System.err.println("Block empty in " + method.getNameAsString());
            } else {
                List<VariableDeclarator> declaredVariables = block.findAll(VariableDeclarator.class);
                if (!declaredVariables.isEmpty() && !methodData.isEmpty()) {
                    for (VariableDeclarator variable : declaredVariables) {
                        for (Pair<Node<GraphOperator>, VisInfo> p : methodData) {
                            String variableName = variable.getNameAsString();
                            if (variableName.equals(p.getValue().variableInfo.getVariableName())) {
                                p.getValue().variableInfo.setVariableClass(variable.getType().asString());
                                System.out.println("found match for " + variableName + " (" + variable.getType().asString() + ")");
                                break;
                            }
                        }
                    }
                }
            }*/

            //data.addAll(methodData);
        }

        return methodData;
    }

    void addToConnected(String from, String to) {
        if (connected.containsKey(from)) {
            connected.get(from).add(to);
        } else {
            Set<String> s = new HashSet<>();
            s.add(to);
            connected.put(from, s);
            allConnectedOperators.add(from);
            allConnectedOperators.add(to);
        }
    }
}
