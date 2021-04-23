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
import java.util.*;

public class FlinkVisualiser extends Visualiser {

    public FlinkVisualiser(@Nonnull ParsedSPE parsedSPE) {
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
        /*for (Pair<Node<GraphOperator>, VisInfo> p : newList) {
            updateOperator(p.getKey());
        }*/
        return newList;
    }

    private void updateOperator(Node<GraphOperator> n) {
        GraphOperator op = n.getItem();
        op.setIdentifier(op.getIdentifier().get().replace("?", "-"));
        //VisInfo.VariableInfo var = op.getVisInfo().variableInfo;
        //var.setVariableData(var.getCalledWithVariableName() + var.getVariableData());
        List<Node<GraphOperator>> successors = n.getSuccessors();
        for (Node<GraphOperator> o : successors) {
            updateOperator(o);
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
        final String name = p.getKey().getItem().getIdentifier().get();
        final VisInfo.VisInfo2 info2 = (VisInfo.VisInfo2) p.getValue();
        final String savedInVariable = p.getValue().variableInfo.getVariableName();
        System.out.println("findSuccessorsFor: " + name + ", " + savedInVariable);

        if (!info2.isFirstInChain() && !info2.isLastInChain()) { // a chained call, already have successors
            for (Node<GraphOperator> s : p.getKey().getSuccessors()) {
                successors.add(new Pair<>(s, s.getItem().getVisInfo()));
            }
            return successors;
        }

        for (Pair<Node<GraphOperator>, VisInfo> pair : list) {
            VisInfo.VisInfo2 pi2 = (VisInfo.VisInfo2) pair.getValue();
            if (pi2.isFirstInChain()) {
                System.out.println("First; " + pi2.getVariableName() + ". " + name);
            } else if (pi2.isLastInChain()) {
                System.out.println("Last; " + pi2.getVariableName() + ". " + name);
            }
        }

        /*String[] sp = name.split("\\?");
        List<String> connectedList = new LinkedList<>();
        for (String o : allConnectedOperators) {
            if (o.contains("?" + sp[1] + "?")) {
                connectedList.add(o);
            }
        }

        if (connectedList.size() > 2) { // chained
            connectedList.sort((o1, o2) -> { // Sort the list as we expect _?_?x to be sorted in ascending order
                int f1 = Integer.parseInt(o1.split("\\?")[2]);
                int f2 = Integer.parseInt(o2.split("\\?")[2]);
                return Integer.compare(f1, f2);
            });
            System.out.println("Connected:" + connectedList + ". " + name);
            int indexOf = connectedList.indexOf(name);
            if (indexOf + 1 < connectedList.size() && indexOf != -1) {
                String cc = connectedList.get(indexOf + 1);
                System.out.println(name + " is outputting to " + cc);
                connectedList.clear();
                connectedList.add(cc);
            } else if (indexOf != -1) {
                connectedList.clear();
            }
        } else {
            connectedList.clear();
        }

        if (savedInVariable != null) {
            for (Pair<Node<GraphOperator>, VisInfo> otherP : list) { // Check if someone calls "savedInVariable.abc()"
                final GraphOperator otherOp = otherP.getKey().getItem();
                final String otherIdent = otherOp.getIdentifier().get();
                if (!otherIdent.equals(name)) {
                    String calledWith = otherP.getValue().variableInfo.getCalledWithVariableName();
                    //System.out.println(otherIdent + " is called with " + calledWith + "? " + savedInVariable);
                    if (savedInVariable.equals(calledWith)) { // Someone calls a method from this variable = connected
                        if (otherP.getValue() instanceof VisInfo.VisInfo2 && p.getValue() instanceof VisInfo.VisInfo2) {
                            if (((VisInfo.VisInfo2) otherP.getValue()).isFirstInChain() && ((VisInfo.VisInfo2) p.getValue()).isLastInChain()) {
                                connectedList.add(otherIdent);
                                successors.add(new Pair<>(otherOp, otherP.getValue()));
                                System.out.println("add successor 1: " + otherIdent);
                            }
                        } else {
                            connectedList.add(otherIdent);
                            successors.add(new Pair<>(otherOp, otherP.getValue()));
                            System.out.println("add successor 2: " + otherIdent);
                        }
                    } else if (connectedList.contains(otherIdent)) {
                        successors.add(new Pair<>(otherOp, otherP.getValue()));
                        System.out.println("add successor 3: " + otherIdent);
                    }
                }
            }
        }*/

        //System.out.println(name + " sends to all of " + connectedList);
        System.out.println("------------------------");

        return successors;
    }

    @Nonnull
    @Override
    VoidVisitorAdapter<Void> methodParserFindDefinitions(List<Pair<Node<GraphOperator>, VisInfo>> methodData, String fileName, ClassOrInterfaceDeclaration c, MethodDeclaration method) {
        //final Map<String, Integer> nameToCount = new HashMap<>();
        final Set<String> used = new HashSet<>();
        return new VoidVisitorAdapter<>() {

            /**
             * Finds all method calls, query structure
             */
            /*@Override
            public void visit(MethodCallExpr n, Void arg) {
                //System.out.println(n.getScope() + " - " + n.getName());

                if (n.getScope().isEmpty()) {
                    return; // method not of interest
                }

                String name = null;
                for (String s : allConnectedOperators) {
                    if (!used.contains(s)) {
                        String[] sp = s.split("\\?", 2);
                        //int i = Integer.parseInt(sp[1]);
                        //int old = nameToCount.getOrDefault(sp[0], -1);
                        if (n.getNameAsString().startsWith(sp[0])) {
                            name = s;
                            //nameToCount.put(sp[0], i);
                            used.add(s);
                            break;
                        }
                    }
                }
                if (name == null) {
                    return;
                }

                final List<VisInfo.VariableInfo> fixedVarInfo = fixVarInfo(findLocalVariableInfo(n));
                System.out.println("fixed for " + name + ": " + fixedVarInfo);

                Set<String> used = new HashSet<>();
                VisInfo.VisInfo2 last = null;
                boolean firstInChain = true;
                for (VisInfo.VariableInfo v : fixedVarInfo) {
                    for (String s : allConnectedOperators) {
                        if (used.contains(s)) {
                            continue;
                        }
                        String[] sp = s.split("\\?");
                        if (name.contains("?" + sp[1] + "?") && v.getOperatorName() != null && s.startsWith(v.getOperatorName())) {
                            Operator operator = new Operator(s);

                            used.add(s);
                            if (last != null) {
                                last.setLastInChain(false);
                            }
                            VisInfo.VisInfo2 visInfo2 = new VisInfo.VisInfo2(fileName, c.getName().asString(), method.getNameAsString(), v, firstInChain, true);
                            operator.setVisInfo(visInfo2);
                            last = visInfo2;
                            methodData.add(new Pair<>(new Node<>(operator, null), visInfo2));
                            firstInChain = false;
                            break;
                        }
                    }
                }

                System.out.println("-------------");
            }

            @Nonnull
            private List<VisInfo.VariableInfo> fixVarInfo(@Nullable VisInfo.VariableInfo variableInfo) {
                List<VisInfo.VariableInfo> l = new LinkedList<>();
                if (variableInfo != null && variableInfo.getVariableData() != null) {
                    String data = variableInfo.getVariableData();
                    List<Pair<String, String>> methods = getMethods(data);
                    for (Pair<String, String> p : methods) {
                        l.add(new VisInfo.VariableInfo(variableInfo.getVariableName(), variableInfo.getCalledWithVariableName(), variableInfo.getVariableClass(), p.getValue(), findOperator(p.getKey()).getKey(), p.getKey()));
                    }
                }
                return l;
            }*/
        };
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
    VoidVisitorAdapter<Void> methodParserFindConnected(List<Pair<Node<GraphOperator>, VisInfo>> methodData, String fileName, ClassOrInterfaceDeclaration c, MethodDeclaration method) {
        final List<String> connected2 = new LinkedList<>(), found = new LinkedList<>();
        final Map<String, Pair<Class<? extends GraphOperator>, String>> codeToOpMap = parsedSPE.getCodeToOpMap();
        final int[] counter = {0};
        System.out.println("start connected: " + queryVariables);
        return new VoidVisitorAdapter<>() {
            /**
             * Finds all connected methods
             */
            @Override
            public void visit(MethodCallExpr n, Void arg) {
                //System.out.println("n.getNameAsString() = " + n.getNameAsString());
                connected2.add(n.getNameAsString());
                super.visit(n, arg);
                if (connected2.isEmpty()) {
                    return;
                }

                com.github.javaparser.ast.Node parent = n;
                while (true) {
                    Optional<com.github.javaparser.ast.Node> o = parent.getParentNode();
                    if (o.isPresent()) {
                        com.github.javaparser.ast.Node p = o.get();
                        if (!p.toString().startsWith("{")) {
                            parent = o.get();
                            //System.out.println("found parent: " + parent);
                        } else {
                            if (!found.contains(parent.toString())) {
                                found.add(parent.toString());
                                break;
                            } else {
                                return;
                            }
                        }
                    } else {
                        return;
                    }
                }

                final VisInfo.VariableInfo vis = findLocalVariableInfo(n);
                if (vis == null) {
                    return;
                }
                if (queryVariables.contains(vis.getCalledWithVariableName()) && vis.getVariableName() != null) {
                    queryVariables.add(vis.getVariableName());
                    System.out.println("added " + vis.getVariableName() + " to variables");
                }
                List<Pair<String, String>> methods = getMethods(parent.toString());

                System.out.println(methods);
                //System.out.println(vis);

                System.out.println("---------------");

                List<Node<GraphOperator>> succs = new LinkedList<>();
                for (int i = methods.size() - 1; i >= 0; i--) {
                    Pair<String, String> p = methods.get(i);
                    Operator op = new Operator(p.getKey());
                    System.out.println("got " + p + " for " + i);
                    VisInfo.VariableInfo variableInfo = new VisInfo.VariableInfo(vis.getVariableName(), vis.getCalledWithVariableName(), vis.getVariableClass(), p.getValue(), vis.getOperatorType(), vis.getOperatorName());
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
                    System.out.println("added " + visInfo2.getVariableName() + "=" + node.getItem().getIdentifier().get() + ", " + node.getSuccessors());
                    methodData.add(new Pair<>(node, visInfo2));
                }

                /*connected2.add(0, n.toString().split("\\.", 2)[0]);

                List<String> uniqueList = new LinkedList<>();
                int counter2 = 0;
                for (String s : connected2) {
                    uniqueList.add(s + "?" + counter[0] + "?" + counter2++);
                }
                if (uniqueList.size() > 1) {
                    for (int i = 0; i < uniqueList.size() - 1; i++) {
                        String from = uniqueList.get(i);
                        String to = null;
                        for (int j = i + 1; j < uniqueList.size(); j++) {
                            String t = uniqueList.get(j);
                            if (codeToOpMap.containsKey(deUnique(t))) {
                                to = t;
                                break;
                            }
                        }
                        if (to != null) {
                            System.out.println("connected: " + from + "->" + to);
                            addToConnectedMap(from, to);
                        }
                    }
                    System.out.println("--------------------");
                    connected2.clear();
                    counter[0]++;
                }*/
                connected2.clear();
            }

            private String deUnique(String s) {
                return s.split("\\?", 2)[0];
            }
        };
    }
}
