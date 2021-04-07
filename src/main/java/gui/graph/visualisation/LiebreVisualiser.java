package gui.graph.visualisation;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import gui.graph.dag.Node;
import gui.graph.data.GraphOperator;
import gui.graph.data.Operator;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;

public class LiebreVisualiser extends Visualiser {
    private final Set<String> queryVariables;
    private final Map<String, String> connected;

    public LiebreVisualiser() {
        queryVariables = new HashSet<>();
        connected = new HashMap<>();
    }

    @NotNull
    @Override
    public List<Pair<Node<GraphOperator>, VisInfo>> fixList(List<Pair<Node<GraphOperator>, VisInfo>> list) {
        final List<Pair<String, List<String>>> queryConnectors = new LinkedList<>();
        return list;
    }

    @Nonnull
    @Override
    VoidVisitorAdapter<Void> methodParser(List<Pair<Node<GraphOperator>, VisInfo>> methodData, String fileName, ClassOrInterfaceDeclaration c, MethodDeclaration method) {
        return new VoidVisitorAdapter<>() {

            /**
             * Finds all method calls, query structure
             *
             * @param n
             * @param arg
             */
            @Override
            public void visit(MethodCallExpr n, Void arg) {
                String name = n.getName().asString();
                //System.out.println(n.getScope() + " - " + n.getName());
                // 0. hitta X i "Query X = new Query();"
                // 1. find all connect()ed operators (their names)
                // 2. search for all their names and get their type/definition

                if (n.getScope().isEmpty() || !queryVariables.contains(n.getScope().get().toString())) {
                    return; // method not of interest, not used in any connect() method call
                }

                System.out.println(name);

                GraphOperator op = new Operator(name);

                //super.visit(m, arg);
                //System.out.println("---");
                //System.out.println(m.getScope() + " - " + m.getName());
                System.out.println("-------------------");

                Node<GraphOperator> node = new Node<>(op, null);

                Pair<String, String> pair = findLocalVariableInfo(n);
                if (pair != null) {
                    //System.out.println("Found pair: " + pair);
                }
                final VisInfo.VariableInfo i = pair == null ? new VisInfo.VariableInfo(null, null, null)
                        : new VisInfo.VariableInfo(pair.getKey(), pair.getValue(), null);
                VisInfo info = new VisInfo(fileName, c.getName().asString(), method.getNameAsString(), i);
                methodData.add(new Pair<>(node, info));

                // https://stackoverflow.com/questions/51117783/how-to-find-type-of-a-variable-while-reading-a-java-source-file
            }

            private Pair<String, String> findLocalVariableInfo(com.github.javaparser.ast.Node n) {
                if (n.getParentNode().isPresent()) {
                    com.github.javaparser.ast.Node parent = n.getParentNode().get();
                    String s = parent.toString();
                    if (s.startsWith("{")) { // no variable
                        return new Pair<>(null, s.split("\\.", 2)[0]);
                    } else if (s.contains("=")) { // we found a variable
                        String[] strings = s.split("=", 2);
                        if (strings[0].split(" ").length > 2) { // not correct equals sign
                            return findLocalVariableInfo(parent);
                        } else {
                            return new Pair<>(strings[0], strings[1].split("\\.", 2)[0]);
                        }
                    } else { // no variable yet, search from parent
                        return findLocalVariableInfo(parent);
                    }
                }
                return null;
            }
        };
    }

    @Nonnull
    @Override
    VoidVisitorAdapter<Void> methodParserInit(List<Pair<Node<GraphOperator>, VisInfo>> methodData, String fileName, ClassOrInterfaceDeclaration c, MethodDeclaration method) {
        return new VoidVisitorAdapter<>() {

            /**
             * Finds all query variables
             *
             * @param n
             * @param arg
             */
            @Override
            public void visit(VariableDeclarator n, Void arg) {
                //super.visit(n, arg);
                //System.out.println("decl: " + n.getType() + "-" + n.getNameAsString());
                if (n.getType().asString().equals("Query")) {
                    System.out.println("found query with name: " + n.getNameAsString());
                    queryVariables.add(n.getNameAsString());
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
                final boolean isConnectMethodCall = n.getArguments().size() == 2 && n.getNameAsString().equals("connect");
                //System.out.println(n.getScope() + " - " + n.getName() + ", " + isConnectMethodCall);
                // 0. hitta X i "Query X = new Query();"
                // 1. find all connect()ed operators (their names)
                // 2. search for all their names and get their type/definition
                if (isConnectMethodCall) {
                    System.out.println("connected: " + n.getArguments());
                    connected.put(n.getArguments().get(0).toString(), n.getArguments().get(1).toString());
                }
            }
        };
    }
}
