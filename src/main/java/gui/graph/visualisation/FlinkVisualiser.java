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
        List<Pair<Node<GraphOperator>, VisInfo>> newList = new LinkedList<>();
        for (Pair<Node<GraphOperator>, VisInfo> p : list) {
            Node<GraphOperator> op = p.getKey();
            boolean isParent = false;
            for (String from : connected.keySet()) {
                if (from.equals(op.getItem().getIdentifier().get())) { // this node has output streams
                    newList.add(new Pair<>(new Node<>(op.getItem(), getSuccessorsFrom(from)), p.getValue()));
                    isParent = true;
                    break;
                }
            }
            if (!isParent) {
                newList.add(new Pair<>(new Node<>(op.getItem(), new LinkedList<>()), p.getValue()));
            }
        }
        return newList;
    }


    @Nonnull
    private List<Node<GraphOperator>> getSuccessorsFrom(String from) {
        List<GraphOperator> successors = findSuccessorsFor(from);
        List<Node<GraphOperator>> successorsList = new LinkedList<>();
        successors.forEach(successor -> successorsList.add(new Node<>(successor, getSuccessorsFrom(successor.getIdentifier().get())))); // recursively find successors
        return successorsList;
    }

    @Nonnull
    private List<GraphOperator> findSuccessorsFor(String name) {
        List<GraphOperator> successors = new LinkedList<>();
        for (String from : connected.keySet()) {
            if (from.equals(name)) {
                for (String to : connected.get(from)) {
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
             */
            @Override
            public void visit(MethodCallExpr n, Void arg) {
                //System.out.println(n.getScope() + " - " + n.getName());

                //System.out.println("method call: " + n);
                if (n.getScope().isEmpty()) {
                    return; // method not of interest, not used in any connect() method call
                }

                //System.out.println("queryVariables: " + queryVariables);

                final VisInfo.VariableInfo variableInfo = findLocalVariableInfo(n);
                final List<VisInfo.VariableInfo> fixedVarInfo = fixVarInfo(variableInfo);
                for (VisInfo.VariableInfo v : fixedVarInfo) {
                    System.out.println("allConnectedOperators = " + allConnectedOperators);
                    if (v.getOperatorName() != null) {
                        GraphOperator op = new Operator(v.getOperatorName());
                        operators.put(v.getOperatorName(), op);
                        methodData.add(new Pair<>(new Node<>(op, null),
                                new VisInfo(fileName, c.getName().asString(), method.getNameAsString(), v))); // TODO, save the fixed vars, knows order/connections
                    }
                }
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
                    System.out.println("data, " + data);
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
                        System.out.println("found " + name + part);

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
                    queryVariables.add(n.getNameAsString());
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
                        addToConnectedMap(from, to);
                        if (!f[0]) {
                            String[] sp = n.toString().split("\\.", 2);
                            if (!sp[0].startsWith(from)) {
                                addToConnectedMap(sp[0], from);
                                System.out.println("connected2: " + sp[0] + "->" + from);
                            }
                            f[0] = true;
                        }
                    }
                }
                System.out.println("--------------------");
                connected2.clear();
                f[0] = false;
            }

        };
    }
}
