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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class FlinkVisualiser extends Visualiser {

    FlinkVisualiser(@Nonnull ParsedSPE parsedSPE) {
        super(parsedSPE);
    }

    @NotNull
    @Override
    public List<Pair<Node<GraphOperator>, VisInfo>> fixList(List<Pair<Node<GraphOperator>, VisInfo>> list) {
        List<Pair<Node<GraphOperator>, VisInfo>> newList = new LinkedList<>();
        for (Pair<Node<GraphOperator>, VisInfo> p : list) {
            for (String queryVariable : queryVariables) {
                if (queryVariable.equals(p.getValue().variableInfo.getCalledWithVariableName())) {
                    newList.add(new Pair<>(new Node<>(p.getKey().getItem(),
                            getSuccessorsFrom(new Pair<>(p.getKey(), p.getValue()), list)),
                            p.getValue()));
                }
            }
        }
        findJoined(newList);
        return newList;
    }

    private void findJoined(List<Pair<Node<GraphOperator>, VisInfo>> newList) {
        for (Pair<Node<GraphOperator>, VisInfo> p : newList) {
            String s = p.getKey().getItem().getIdentifier().get();
            if (s.startsWith("join-") || s.startsWith("union-")) {
                System.out.println("starts with " + s);
                String data = p.getValue().variableInfo.getVariableData();
                System.out.println("and data is " + data);
                if (data != null) {
                    if (s.startsWith("join-")) {
                        String joinedWith = data.split("\\(", 2)[1].split("\\)", 2)[0];
                        System.out.println("joined with " + joinedWith);
                        for (Pair<Node<GraphOperator>, VisInfo> p2 : newList) {
                            if (joinedWith.equals(p2.getValue().variableInfo.getVariableName())) {
                                for (Pair<Node<GraphOperator>, VisInfo> p3 : newList) {
                                    System.out.println("joined, fix");
                                    fixJoined(p2.getKey().getItem().getIdentifier().get(), p.getKey(), p3.getKey());
                                }
                                break;
                            }
                        }
                    } else {
                        String[] joinedWith = data.split("\\(", 2)[1].split("\\)", 2)[0].split(",");
                        for (String join : joinedWith) {
                            for (Pair<Node<GraphOperator>, VisInfo> p2 : newList) {
                                if (join.trim().equals(p2.getValue().variableInfo.getVariableName())) {
                                    for (Pair<Node<GraphOperator>, VisInfo> p3 : newList) {
                                        fixJoined(p2.getKey().getItem().getIdentifier().get(), p.getKey(), p3.getKey());
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void fixJoined(String identifier, Node<GraphOperator> newSuccessor, Node<GraphOperator> node) {
        for (Node<GraphOperator> op : node.getSuccessors()) {
            if (op.getItem().getIdentifier().get().equals(identifier)) {
                op.getSuccessors().add(newSuccessor);
            }
            fixJoined(identifier, newSuccessor, op);
        }
    }

    @Nonnull
    private List<Node<GraphOperator>> getSuccessorsFrom(@Nonnull Pair<Node<GraphOperator>, VisInfo> p, List<Pair<Node<GraphOperator>, VisInfo>> list) {
        /*
          1. first run is for the source nodes, called with "calledWithVariable"
          2. if they are saved to a variable they can be used later. If not it is just chaining
          3. If saved to variable, check where it has been used later
        */
        List<Pair<Node<GraphOperator>, VisInfo>> successors = findSuccessorsFor(p, list);
        List<Node<GraphOperator>> successorsList = new LinkedList<>();
        successors.forEach(successor -> successorsList.add(new Node<>(successor.getKey().getItem(), getSuccessorsFrom(successor, list)))); // recursively find successors
        return successorsList;
    }

    @Nonnull
    private List<Pair<Node<GraphOperator>, VisInfo>> findSuccessorsFor(Pair<Node<GraphOperator>, VisInfo> p, List<Pair<Node<GraphOperator>, VisInfo>> list) {
        List<Pair<Node<GraphOperator>, VisInfo>> successors = new LinkedList<>();
        final VisInfo.VisInfo2 info2 = (VisInfo.VisInfo2) p.getValue();
        final String savedInVariable = p.getValue().variableInfo.getVariableName();

        if (!info2.isLastInChain()) { // a chained call, already have successors
            for (Node<GraphOperator> s : p.getKey().getSuccessors()) {
                successors.add(new Pair<>(s, s.getItem().getVisInfo()));
            }
            return successors;
        }

        for (Pair<Node<GraphOperator>, VisInfo> pair : list) {
            VisInfo.VisInfo2 pi2 = (VisInfo.VisInfo2) pair.getValue();
            if (pi2.isFirstInChain() && savedInVariable != null && savedInVariable.equals(pair.getValue().variableInfo.getCalledWithVariableName())) {
                successors.add(new Pair<>(pair.getKey(), pair.getValue()));
            }
        }

        return successors;
    }

    @Nonnull
    private List<Pair<String, String>> getMethods(String data) {
        List<Pair<String, String>> s = new LinkedList<>();
        if (data == null) {
            return s;
        }
        char[] c = data.toCharArray();
        int count = 0;
        int start = -1;
        boolean foundAny = false;
        for (int i = 0; i < c.length; i++) {
            if (c[i] == '(') {
                foundAny = true;
                count++;
                if (count == 1) {
                    start = i;
                }
            } else if (c[i] == ')') {
                count--;
            }
            if (foundAny && count == 0) {
                String part = data.substring(start, i + 1);
                int namePos = data.substring(0, start).lastIndexOf(".");
                String name = data.substring(namePos + 1, start);
                if (!parsedSPE.getCodeToOpMap().containsKey(name)) { // not in "links"
                    if (!s.isEmpty()) { // add to previous item as data
                        int pos = s.size() - 1;
                        Pair<String, String> p = s.remove(pos);
                        s.add(pos, new Pair<>(p.getKey(), p.getValue() + "." + name + part));
                    }
                } else {
                    s.add(new Pair<>(name, "." + name + part));
                }
                foundAny = false;
            }
        }
        return s;
    }

    @NotNull
    @Override
    VoidVisitorAdapter<Void> methodParserFindVariables(List<Pair<Node<GraphOperator>, VisInfo>> methodData, String fileName, ClassOrInterfaceDeclaration c, MethodDeclaration method) {
        return new VoidVisitorAdapter<>() {
            /**
             * Finds all query variables and their types
             */
            @Override
            public void visit(VariableDeclarator n, Void arg) {
                super.visit(n, arg);
                if (n.getType().asString().equals("StreamExecutionEnvironment")) {
                    queryVariables.add(n.getNameAsString());
                } else {
                    variableClasses.put(n.getNameAsString(), n.getType().asString());
                }
            }
        };
    }

    @NotNull
    @Override
    VoidVisitorAdapter<Void> methodParserFindDefinitions(List<Pair<Node<GraphOperator>, VisInfo>> methodData, String fileName, ClassOrInterfaceDeclaration c, MethodDeclaration method) {
        final List<String> connected2 = new LinkedList<>(), found = new LinkedList<>();
        final int[] counter = {0};
        return new VoidVisitorAdapter<>() {
            /**
             * Finds all connected methods
             */
            @Override
            public void visit(MethodCallExpr n, Void arg) {
                connected2.add(n.getNameAsString());
                super.visit(n, arg);
                if (connected2.isEmpty()) {
                    return;
                }

                ////System.out.println("----");

                com.github.javaparser.ast.Node parent = getCorrectNode(n);
                if (parent == null) {
                    return;
                }
                String s = parent.toString();
                if (!found.contains(s)) {
                    found.add(s);
                } else {
                    return;
                }

                //System.out.println("find for " + parent.getClass());
                final VisInfo.VariableInfo vis = findLocalVariableInfo(parent);
                if (vis == null) {
                    return;
                }

                if (queryVariables.contains(vis.getCalledWithVariableName()) && vis.getVariableName() != null) {
                    queryVariables.add(vis.getVariableName());
                }
                List<Pair<String, String>> methods = getMethods(parent.toString());
                if (methods.isEmpty() || !parsedSPE.getCodeToOpMap().containsKey(methods.get(0).getKey())) {
                    return;
                }

                List<Node<GraphOperator>> succs = new LinkedList<>();
                for (int i = methods.size() - 1; i >= 0; i--) {
                    Pair<String, String> p = methods.get(i);
                    Pair<Class<? extends GraphOperator>, String> classStringPair = parsedSPE.getCodeToOpMap().get(p.getKey());
                    final String name = p.getKey() + "-" + counter[0]++;
                    GraphOperator op = getCorrectOpClass(name, classStringPair.getKey());

                    VisInfo.VariableInfo variableInfo = new VisInfo.VariableInfo(vis.getVariableName(), vis.getCalledWithVariableName(), vis.getVariableClass(), p.getValue(), vis.getOperatorType(), classStringPair.getValue());
                    VisInfo.VisInfo2 visInfo2 = new VisInfo.VisInfo2(fileName, c.getName().asString(), method.getNameAsString(), variableInfo, i == 0, i == methods.size() - 1, vis.getVariableName());
                    op.setVisInfo(visInfo2);
                    Node<GraphOperator> node;
                    if (i == methods.size() - 1) {
                        node = new Node<>(op, null);
                    } else {
                        node = new Node<>(op, succs);
                    }
                    succs = new ArrayList<>();
                    succs.add(node);
                    methodData.add(new Pair<>(node, visInfo2));
                }
                connected2.clear();
                ////System.out.println("---");
            }

            @Nullable
            private com.github.javaparser.ast.Node getCorrectNode(@Nonnull MethodCallExpr n) {
                com.github.javaparser.ast.Node first = n;
                com.github.javaparser.ast.Node lastMethodCall = n;
                com.github.javaparser.ast.Node lastVariableDecl = n;
                while (!(first instanceof ClassOrInterfaceDeclaration)) {
                    Optional<com.github.javaparser.ast.Node> optionalNode = first.getParentNode();
                    if (first instanceof VariableDeclarator) {
                        lastVariableDecl = first;
                    } else if (first instanceof MethodCallExpr) {
                        lastMethodCall = first;
                    }
                    if (optionalNode.isPresent()) {
                        first = optionalNode.get();
                    } else {
                        return null;
                    }
                }
                if (lastVariableDecl instanceof VariableDeclarator) {
                    return lastVariableDecl;
                } else {
                    return lastMethodCall;
                }
            }
        };
    }

    @Nullable
    VisInfo.VariableInfo findLocalVariableInfo(com.github.javaparser.ast.Node n) {
        if (n instanceof VariableDeclarator) { // we found a variable
            return extractData((VariableDeclarator) n, n.toString());
        } else if (n instanceof MethodCallExpr) { // no variable
            String[] sp = n.toString().split("\\.", 2);
            return new VisInfo.VariableInfo(null, sp[0], null, sp[1], Operator.class, null);
        }
        return null;
    }
}
