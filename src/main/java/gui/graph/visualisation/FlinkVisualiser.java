package gui.graph.visualisation;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import gui.graph.dag.Node;
import gui.graph.data.GraphOperator;
import gui.graph.data.Operator;
import gui.spe.ParsedSPE;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FlinkVisualiser extends Visualiser {

    public FlinkVisualiser(@Nonnull ParsedSPE parsedSPE) {
        super(parsedSPE);
    }

    @Nonnull
    @Override
    List<Pair<Node<GraphOperator>, VisInfo>> fixList(List<Pair<Node<GraphOperator>, VisInfo>> list) {
        return list;
    }

    @Nonnull
    @Override
    VoidVisitorAdapter<Void> methodParser(List<Pair<Node<GraphOperator>, VisInfo>> methodData, String fileName, ClassOrInterfaceDeclaration c, MethodDeclaration method) {
        return new VoidVisitorAdapter<>() {

            /**
             * Finds all method calls, query structure
             */
            @Override
            public void visit(MethodCallExpr n, Void arg) {
                //System.out.println(n.getScope() + " - " + n.getName());
                // 0. hitta X i "Query X = new Query();"
                // 1. find all connect()ed operators (their names)
                // 2. search for all their names and get their type/definition

                if (n.getScope().isEmpty()) {
                    return; // method not of interest, not used in any connect() method call
                }

                final VisInfo.VariableInfo variableInfo = findLocalVariableInfo(n);
                final String variableName = variableInfo == null ? null : variableInfo.getVariableName();
                if (variableName != null && allConnectedOperators.contains(variableName)) {
                    GraphOperator op = new Operator(variableName);
                    operators.put(variableName, op);
                    methodData.add(new Pair<>(new Node<>(op, null),
                            new VisInfo(fileName, c.getName().asString(), method.getNameAsString(), variableInfo)));
                }
            }

            @Nullable
            private VisInfo.VariableInfo findLocalVariableInfo(com.github.javaparser.ast.Node n) {
                if (n.getParentNode().isPresent()) {
                    com.github.javaparser.ast.Node parent = n.getParentNode().get();
                    String s = parent.toString();
                    if (s.startsWith("{")) { // no variable
                        //System.out.println("parent1 = " + parent);
                        return new VisInfo.VariableInfo(null, n.toString().split("\\.", 2)[0].trim(), null, null, Operator.class, null);
                    } else if (s.contains("=")) { // we found a variable
                        String[] strings = s.split("=", 2);
                        if (strings[0].split(" ").length > 2) { // not correct equals sign
                            return findLocalVariableInfo(parent);
                        } else {
                            final String variableName = strings[0].trim();
                            final String varData = strings[1].trim();
                            final String[] varDataDot = varData.split("\\.", 2);
                            final String calledWithVar = varDataDot[0].trim();
                            final String varClass = getTypeFor(variableName);
                            final Pair<Class<? extends GraphOperator>, String> operator = findOperator(varDataDot[1]);
                            //System.out.println("parent2 = " + parent);
                            return new VisInfo.VariableInfo(variableName, calledWithVar, varClass, strings[1].trim(), operator.getKey(), operator.getValue()); // TODO
                        }
                    } else { // no variable yet, search from parent
                        return findLocalVariableInfo(parent);
                    }
                }
                return null;
            }

            @Nonnull
            private Pair<Class<? extends GraphOperator>, String> findOperator(String afterDot) {
                afterDot = afterDot.toLowerCase();
                Map<String, Pair<Class<? extends GraphOperator>, String>> codeToOpMap = parsedSPE.getCodeToOpMap();
                for (String key : codeToOpMap.keySet()) {
                    if (afterDot.startsWith(key.toLowerCase())) {
                        return codeToOpMap.get(key);
                    }
                }
                return new Pair<>(Operator.class, null);
            }

            @Nullable
            private String getTypeFor(@Nonnull String variable) {
                return variableClasses.get(variable.trim());
            }
        };
    }

    @NotNull
    @Override
    VoidVisitorAdapter<Void> methodParserInit(List<Pair<Node<GraphOperator>, VisInfo>> methodData, String fileName, ClassOrInterfaceDeclaration c, MethodDeclaration method) {
        final List<String> connected = new LinkedList<>();
        final Map<String, Pair<Class<? extends GraphOperator>, String>> codeToOpMap = parsedSPE.getCodeToOpMap();
        return new VoidVisitorAdapter<>() {
            /**
             * Finds all query variables and their types
             *
             * @param n
             * @param arg
             */
            @Override
            public void visit(VariableDeclarator n, Void arg) {
                super.visit(n, arg);
                //System.out.println("decl: " + n.getType() + "-" + n.getNameAsString());
                if (n.getType().asString().equals("StreamExecutionEnvironment")) {
                    //System.out.println("found query with name: " + n.getNameAsString());
                    queryVariables.add(n.getNameAsString());
                } else {
                    queryVariables.add(n.getNameAsString());
                    variableClasses.put(n.getNameAsString(), n.getType().asString());
                    //System.out.println("put " + n.getNameAsString() + ";" + n.getType().asString());
                }
            }

            /**
             * Finds all connected methods
             *
             * @param n
             * @param arg
             */
            @Override
            public void visit(MethodCallExpr n, Void arg) {
                //System.out.println(n.getScope() + " - " + n.getName() + ", " + isConnectMethodCall);
                // 0. hitta X i "Query X = new Query();"
                // 1. find all connect()ed operators (their names)
                // 2. search for all their names and get their type/definition

                System.out.println("n2: " + n.getNameAsString()); // save these (compare with json first), all before "------" are connected
                connected.add(0, n.getNameAsString()); // add at first pos as the last chained call is found first
                super.visit(n, arg);

                for (int i = 0; i < connected.size() - 1; i++) {
                    String from = connected.get(i);
                    String to = null;
                    for (int j = i + 1; j < connected.size(); j++) {
                        String t = connected.get(j);
                        if (codeToOpMap.containsKey(t)) {
                            to = t;
                            break;
                        }
                    }
                    if (to != null) {
                        System.out.println("connected: " + from + "->" + to);
                        addToConnected(from, to);
                        System.out.println(n);
                        String[] sp = n.toString().split("\\.",2);
                        if (!sp[0].startsWith(from)) {
                            addToConnected(sp[0], from);
                            System.out.println("connected2: " + sp[0] + "->" + from);
                        }
                    } else {
                        System.out.println("Not connected: " + from);
                    }
                }

                System.out.println("connected?: " + connected);
                String[] split = n.toString().split("\\."); // query connect(ID, r) connect(r, sink)
                if (split.length > 2) {
                    for (int i = 0; i < split.length - 1; i++) { // dont include last, it was processed above
                        String s = split[i].replace(" ", "").trim();
                        if (split[i].startsWith("connect(")) {
                            s = s.replace("connect(", "").replace(")", "");
                            String[] split2 = s.split(",");
                            if (split2.length == 2) {
                                addToConnected(split2[0], split2[1]);
                            }
                        }
                    }
                }
                System.out.println("--------------------");
                connected.clear();
            }


        };
    }
}
