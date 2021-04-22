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

    @NotNull
    @Override
    public List<Pair<Node<GraphOperator>, VisInfo>> fixList(List<Pair<Node<GraphOperator>, VisInfo>> list) {
        return list;
    }

    @Nonnull
    @Override
    VoidVisitorAdapter<Void> methodParser(List<Pair<Node<GraphOperator>, VisInfo>> methodData, String fileName, ClassOrInterfaceDeclaration c, MethodDeclaration method) {
        final int[] counter = {0};
        final List<Pair<GraphOperator, VisInfo.VariableInfo>> ops = new LinkedList<>();
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
                boolean found = false;
                for (String s : allConnectedOperators) {
                    if (n.getNameAsString().startsWith(s.split("\\?", 2)[0])) {
                        found = true;
                    }
                }
                if (!found) {
                    return;
                }
                final VisInfo.VariableInfo variableInfo = findLocalVariableInfo(n);
                final List<VisInfo.VariableInfo> fixedVarInfo = fixVarInfo(variableInfo);
                for (String s : allConnectedOperators) {
                    if (variableInfo != null && fixedVarInfo.size() > 0) {
                        String operatorName = fixedVarInfo.get(0).getOperatorName();
                        //System.out.println("op: " + op);
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
                                System.out.println("found: " + connectedList);
                                counter[0]++;
                                for (VisInfo.VariableInfo v : fixedVarInfo) {
                                    // TODO allConnectedOperators = [intStream?3?0, map?3?0, addSource?1?0, map?3?1, addSource?2?0, query?1?0, StreamExecutionEnvironment?0?0, filter?3?1, query?2?0, getExecutionEnvironment?0?0]
                                    //System.out.println("allConnectedOperators = " + allConnectedOperators);
                                    final String opName = v.getOperatorName();
                                    if (opName != null) {
                                        System.out.println("v.getOperatorName() = " + opName);
                                        for (String conn : connectedList) {
                                            if (conn.startsWith(opName)) {
                                                System.out.println("conn " + conn + " starts with " + opName);
                                                Operator operator = new Operator(conn);
                                                ops.add(new Pair<>(operator, v));
                                            }
                                        }
                                        //operators.put(v.getOperatorName(), operator);
                                        //methodData.add(new Pair<>(new Node<>(operator, null),
                                        //        new VisInfo(fileName, c.getName().asString(), method.getNameAsString(), v)));
                                    }
                                }
                                //ops.clear();
                                // TODO add called with as connected
                            }
                        }
                    }
                }

                for (Pair<GraphOperator, VisInfo.VariableInfo> p : ops) {
                    methodData.add(new Pair<>(new Node<>(p.getKey(), getSuccessorsFrom(p.getKey().getIdentifier().get(), ops)),
                            new VisInfo(fileName, c.getName().asString(), method.getNameAsString(), p.getValue())));
                }

            }

            private List<Node<GraphOperator>> getSuccessorsFrom(String name, List<Pair<GraphOperator, VisInfo.VariableInfo>> ops) {
                String[] sp = name.split("\\?");
                List<String> connectedList = new LinkedList<>();
                for (String o : allConnectedOperators) {
                    if (o.contains("?" + sp[1] + "?")) {
                        connectedList.add(o);
                    }
                }
                System.out.println(name + " is connected with " + connectedList);
                return null;
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
                    if (parsedSPE.getCodeToOpMap().containsKey(to)) {
                        System.out.println("connected3: " + sp[0] + "->" + to);
                        addToConnectedMap(makeUnique(sp[0]), makeUnique(to));
                        counter[1]++;
                    } else {
                        System.out.println("Does not contain key " + to);
                    }
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
