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
    private final Set<String> queryVariables, allConnectedOperators;
    private final Map<String, GraphOperator> operators;
    private final Map<String, Set<String>> connected;

    public LiebreVisualiser() {
        queryVariables = new HashSet<>();
        allConnectedOperators = new HashSet<>();
        operators = new HashMap<>();
        connected = new HashMap<>();
    }

    @NotNull
    @Override
    public List<Pair<Node<GraphOperator>, VisInfo>> fixList(List<Pair<Node<GraphOperator>, VisInfo>> list) {
        List<Pair<Node<GraphOperator>, VisInfo>> newList = new LinkedList<>();
        System.out.println("connected: " + connected);
        for (Pair<Node<GraphOperator>, VisInfo> p : list) {
            System.out.println("p: " + p.getKey());
            Node<GraphOperator> op = p.getKey();
            for (String from : connected.keySet()) {
                System.out.println("from: " + from);
                if (from.equals(op.getItem().getIdentifier().get())) { // this node has output streams
                    System.out.println("");
                    newList.add(new Pair<>(new Node<>(op.getItem(), getSuccessorsFrom(from)), p.getValue()));
                    break;
                }
            }
        }

        return newList;
    }

    @Nonnull
    private List<Node<GraphOperator>> getSuccessorsFrom(String from) {
        List<GraphOperator> successors = findSuccessorsFor(from);
        List<Node<GraphOperator>> successorsList = new LinkedList<>();
        successors.forEach(successor -> successorsList.add(new Node<>(successor, getSuccessorsFrom(successor.getIdentifier().get())))); // recursively find successors
        System.out.println("successors: " + successorsList);
        return successorsList;
    }

    @Nonnull
    private List<GraphOperator> findSuccessorsFor(String name) {
        List<GraphOperator> successors = new LinkedList<>();
        for (String from : connected.keySet()) {
            if (from.equals(name)) {
                for (String to : connected.get(from)) {
                    System.out.println("from: " + from + ", to: " + to + ", operators: " + operators.get(to));
                    successors.add(operators.get(to));
                }
            }
        }
        return successors;
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
                //System.out.println(n.getScope() + " - " + n.getName());
                // 0. hitta X i "Query X = new Query();"
                // 1. find all connect()ed operators (their names)
                // 2. search for all their names and get their type/definition

                if (n.getScope().isEmpty() || !queryVariables.contains(n.getScope().get().toString())) {
                    return; // method not of interest, not used in any connect() method call
                }

                //super.visit(m, arg);
                //System.out.println("---");
                //System.out.println(m.getScope() + " - " + m.getName());

                Pair<String, String> pair = findLocalVariableInfo(n);
                if (pair != null && pair.getKey() != null && allConnectedOperators.contains(pair.getKey())) {
                    GraphOperator op = new Operator(pair.getKey());
                    operators.put(pair.getKey(), op);
                    Node<GraphOperator> node = new Node<>(op, null);
                    final VisInfo.VariableInfo i = new VisInfo.VariableInfo(pair.getKey(), pair.getValue(), null);
                    VisInfo info = new VisInfo(fileName, c.getName().asString(), method.getNameAsString(), i);
                    methodData.add(new Pair<>(node, info));
                }
                //System.out.println("-------------------");

                // https://stackoverflow.com/questions/51117783/how-to-find-type-of-a-variable-while-reading-a-java-source-file
            }

            private Pair<String, String> findLocalVariableInfo(com.github.javaparser.ast.Node n) { // TODO chained connect().connect() is not found (only last)
                if (n.getParentNode().isPresent()) {
                    com.github.javaparser.ast.Node parent = n.getParentNode().get();
                    String s = parent.toString();
                    if (s.startsWith("{")) { // no variable
                        return new Pair<>(null, s.split("\\.", 2)[0].trim());
                    } else if (s.contains("=")) { // we found a variable
                        String[] strings = s.split("=", 2);
                        if (strings[0].split(" ").length > 2) { // not correct equals sign
                            return findLocalVariableInfo(parent);
                        } else {
                            return new Pair<>(strings[0].trim(), strings[1].split("\\.", 2)[0].trim());
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
                System.out.println(n.getScope() + " - " + n.getName() + ", " + isConnectMethodCall);
                // 0. hitta X i "Query X = new Query();"
                // 1. find all connect()ed operators (their names)
                // 2. search for all their names and get their type/definition
                if (isConnectMethodCall) {
                    System.out.println("connected: " + n.getArguments()); // TODO chained connect().connect() is not found (only last)
                    String from = n.getArguments().get(0).toString();
                    String to = n.getArguments().get(1).toString();
                    if (connected.containsKey(from)) {
                        connected.get(from).add(to);
                    } else {
                        Set<String> s = new HashSet<>();
                        s.add(to);
                        connected.put(from, s);
                        allConnectedOperators.add(from);
                        allConnectedOperators.add(to);
                    }
                    String[] split = n.toString().split("\\."); // query connect(ID, r) connect(r, sink)
                    if (split.length > 2) {
                        for (int i = 0; i < split.length - 1; i++) { // dont include last, it was processed above
                            String s = split[i].replace(" ", "").trim();
                            if (split[i].startsWith("connect(")) {
                                s = s.replace("connect(","").replace(")","");
                                String[] split2 = s.split(",");
                                if (split2.length == 2) {
                                    // TODO extract duplicate connect.put() from above
                                }
                            }
                        }
                    }
                }
            }
        };
    }
}
