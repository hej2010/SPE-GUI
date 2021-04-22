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
import java.util.*;

public class FlinkVisualiser extends Visualiser {

    public FlinkVisualiser(@Nonnull ParsedSPE parsedSPE) {
        super(parsedSPE);
    }

    @NotNull
    @Override
    public List<Pair<Node<GraphOperator>, VisInfo>> fixList(List<Pair<Node<GraphOperator>, VisInfo>> list) {
        List<Pair<Node<GraphOperator>, VisInfo>> newList = new LinkedList<>();
        System.out.println("allConnectedOperators = " + allConnectedOperators);
        for (Pair<Node<GraphOperator>, VisInfo> p : list) {
            for (String queryVariable : queryVariables) {
                if (queryVariable.equals(p.getValue().variableInfo.getCalledWithVariableName())) {
                    newList.add(new Pair<>(new Node<>(p.getKey().getItem(),
                            getSuccessorsFrom(new Pair<>(p.getKey().getItem(), p.getValue()), list)),
                            p.getValue()));
                }
            }
        }
        return newList;
    }

    @Nonnull
    private List<Node<GraphOperator>> getSuccessorsFrom(@Nonnull Pair<GraphOperator, VisInfo> p, List<Pair<Node<GraphOperator>, VisInfo>> list) {
                /*
                1. first run is for the source nodes, called with "calledWithVariable"
                2. if they are saved to a variable they can be used later. If not it is just chaining
                3. If saved to variable, check where it has been used later
                 */


        List<Pair<GraphOperator, VisInfo>> successors = findSuccessorsFor(p, list);
        List<Node<GraphOperator>> successorsList = new LinkedList<>();
        successors.forEach(successor -> successorsList.add(new Node<>(successor.getKey(), getSuccessorsFrom(successor, list)))); // recursively find successors
        return successorsList;
    }

    @Nonnull
    private List<Pair<GraphOperator, VisInfo>> findSuccessorsFor(Pair<GraphOperator, VisInfo> p, List<Pair<Node<GraphOperator>, VisInfo>> list) {
        List<Pair<GraphOperator, VisInfo>> successors = new LinkedList<>();
        final String name = p.getKey().getIdentifier().get();
        final String savedInVariable = p.getValue().variableInfo.getVariableName();
        System.out.println("findSuccessorsFor: " + name + ", " + savedInVariable);
        /*
        Successors are either:
        1. chained directly, found in connectedList
        2. called from the variable name
         */

        String[] sp = name.split("\\?");
        List<String> connectedList = new LinkedList<>();
        for (String o : allConnectedOperators) {
            if (o.contains("?" + sp[1] + "?")) {
                connectedList.add(o);
            }
        }
        System.out.println(name + " is connected with " + connectedList);
        if (connectedList.size() > 2) {
            //connectedList.remove(name);
            int indexOf = connectedList.indexOf(name);
            if (indexOf + 1 < connectedList.size() && indexOf != -1) {
                String cc = connectedList.get(indexOf + 1);
                System.out.println(name + " is outputting to " + cc);
                connectedList.clear();
                connectedList.add(cc);
            }
        } else {
            connectedList.clear();
        }

        if (savedInVariable != null) {
            for (Pair<Node<GraphOperator>, VisInfo> otherP : list) { // Check if someone calls "savedInVariable.abc()"
                if (!otherP.getKey().getItem().getIdentifier().get().equals(name)) {
                    String calledWith = otherP.getValue().variableInfo.getCalledWithVariableName();
                    System.out.println(otherP.getKey().getItem().getIdentifier().get() + " is called with " + calledWith + "? " + savedInVariable);
                    if (savedInVariable.equals(calledWith)) { // Someone calls a method from this variable = connected
                        connectedList.add(otherP.getKey().getItem().getIdentifier().get());
                    }
                }
            }
        }

        System.out.println(name + " sends to all of " + connectedList);
        System.out.println("------------------------");

        return successors;
    }

    @Nonnull
    @Override
    VoidVisitorAdapter<Void> methodParser(List<Pair<Node<GraphOperator>, VisInfo>> methodData, String fileName, ClassOrInterfaceDeclaration c, MethodDeclaration method) {
        final int[] counter = {0};
        final Map<String, Integer> nameToCount = new HashMap<>();
        return new VoidVisitorAdapter<>() {

            /**
             * Finds all method calls, query structure
             */
            @Override
            public void visit(MethodCallExpr n, Void arg) {
                //System.out.println(n.getScope() + " - " + n.getName());

                //System.out.println("method call: " + n);
                if (n.getScope().isEmpty()) {
                    return; // method not of interest, not used in any connect() method call
                }

                //System.out.println("queryVariables: " + queryVariables);
                String name = null;
                for (String s : allConnectedOperators) {
                    String[] sp = s.split("\\?");
                    int i = Integer.parseInt(sp[1]);
                    int old = nameToCount.getOrDefault(sp[0], -1);
                    System.out.println("parsed " + i + ":" + old + " for " + s);
                    if (i <= old) {
                        continue;
                    }
                    if (n.getNameAsString().startsWith(sp[0])) {
                        System.out.println("found " + s + " in loop");
                        name = s;
                        nameToCount.put(sp[0], i);
                        break;
                    }
                }
                System.out.println("WORK WITH: " + name);
                if (name == null) {
                    return;
                }
                final VisInfo.VariableInfo variableInfo = findLocalVariableInfo(n);
                System.out.println("got " + variableInfo);
                final List<VisInfo.VariableInfo> fixedVarInfo = fixVarInfo(variableInfo);
                System.out.println("after fix: " + fixedVarInfo);

                for (VisInfo.VariableInfo v : fixedVarInfo) {
                    Operator operator = new Operator(name);
                    methodData.add(new Pair<>(new Node<>(operator, null), // TODO, how to set name?
                            new VisInfo(fileName, c.getName().asString(), method.getNameAsString(), v)));
                }

                //final Set<VisInfo.VariableInfo> taken = new HashSet<>();
                /*for (String s : allConnectedOperators) {
                    if (variableInfo != null && fixedVarInfo.size() > 0) {
                        String operatorName = fixedVarInfo.get(0).getOperatorName();
                        System.out.println("opName: " + operatorName);
                        if (operatorName != null && s.startsWith(operatorName)) {
                            //System.out.println("Found " + s);
                            String[] sp = s.split("\\?");
                            if (sp[1].equals(String.valueOf(counter[0]))) {
                                List<String> connectedList = new LinkedList<>();
                                for (String o : allConnectedOperators) {
                                    if (o.contains("?" + sp[1] + "?")) {
                                        connectedList.add(o);
                                    }
                                }
                                counter[0]++;
                                for (VisInfo.VariableInfo v : fixedVarInfo) {
                                    //if (taken.contains(v)) {
                                    //    continue;
                                    //}
                                    final String opName = v.getOperatorName();
                                    if (opName != null) {
                                        for (String conn : connectedList) {
                                            if (conn.startsWith(opName)) {
                                                System.out.println("conn " + conn + " starts with " + opName + ", " + connectedList);
                                                Operator operator = new Operator(conn);
                                                methodData.add(new Pair<>(new Node<>(operator, null),
                                                        new VisInfo(fileName, c.getName().asString(), method.getNameAsString(), v)));
                                                //System.out.println("add operator " + conn + " with " + v);
                                                //taken.add(v);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }*/
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
                    //System.out.println("data, " + data);
                }
                return l;
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

                        s.add(new Pair<>(name, "." + name + part));
                        foundAny = false;
                    }
                }
                return s;
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
        final List<String> connected2 = new LinkedList<>();
        final Map<String, Pair<Class<? extends GraphOperator>, String>> codeToOpMap = parsedSPE.getCodeToOpMap();
        final boolean[] f = {false};
        final int[] counter = {0, 0};
        return new VoidVisitorAdapter<>() {
            /**
             * Finds all query variables and their types
             */
            @Override
            public void visit(VariableDeclarator n, Void arg) {
                super.visit(n, arg);
                //System.out.println("decl: " + n.getType() + "-" + n.getNameAsString());
                if (n.getType().asString().equals("StreamExecutionEnvironment")) {
                    //System.out.println("found query with name: " + n.getNameAsString());
                    queryVariables.add(n.getNameAsString());
                } else {
                    //queryVariables.add(n.getNameAsString());
                    variableClasses.put(n.getNameAsString(), n.getType().asString());
                    //System.out.println("put " + n.getNameAsString() + ";" + n.getType().asString());
                }
            }

            /**
             * Finds all connected methods
             */
            @Override
            public void visit(MethodCallExpr n, Void arg) {
                //System.out.println(n.getScope() + " - " + n.getName() + ", " + isConnectMethodCall);

                //System.out.println("n2: " + n.getNameAsString()); // save these (compare with json first), all before "------" are connected
                connected2.add(0, n.getNameAsString()); // add at first pos as the last chained call is found first
                super.visit(n, arg);
                //System.out.println("n1: " + n.getNameAsString());
                //System.out.println("connected2 = " + connected2);
                if (connected2.size() > 1) {
                    for (int i = 0; i < connected2.size() - 1; i++) {
                        String from = connected2.get(i);
                        String to = null;
                        for (int j = i + 1; j < connected2.size(); j++) {
                            String t = connected2.get(j);
                            if (codeToOpMap.containsKey(t)) {
                                to = t;
                                break;
                            }
                        }
                        if (to != null) {
                            System.out.println("connected: " + from + "->" + to);
                            addToConnectedMap(makeUnique(from), makeUnique(to));
                            if (!f[0]) {
                                String[] sp = n.toString().split("\\.", 2);
                                if (!sp[0].startsWith(from)) {
                                    addToConnectedMap(makeUnique(sp[0]), makeUnique(from));
                                    System.out.println("connected2: " + sp[0] + "->" + from);
                                }
                                f[0] = true;
                            }
                            counter[1]++;
                        }
                    }
                } else if (connected2.size() == 1) {
                    String[] sp = n.toString().split("\\.", 2);
                    String to = connected2.get(0);
                    System.out.println("connected3: " + sp[0] + "->" + to);
                    addToConnectedMap(makeUnique(sp[0]), makeUnique(to));
                    counter[1]++;
                }

                System.out.println("--------------------");
                connected2.clear();
                f[0] = false;
                counter[0]++;
                counter[1] = 0;
            }

            private String makeUnique(String s) {
                return s + "?" + counter[0] + "?" + counter[1];
            }
        };
    }
}
