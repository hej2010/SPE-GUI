package gui.graph.visualisation;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import gui.graph.dag.Node;
import gui.graph.data.GraphOperator;
import gui.spe.ParsedSPE;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

public class LiebreVisualiser extends Visualiser {

    LiebreVisualiser(@Nonnull ParsedSPE parsedSPE) {
        super(parsedSPE);
    }

    @NotNull
    @Override
    public List<Pair<Node<GraphOperator>, VisInfo>> fixList(List<Pair<Node<GraphOperator>, VisInfo>> list) {
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
    VoidVisitorAdapter<Void> methodParserFindDefinitions(List<Pair<Node<GraphOperator>, VisInfo>> methodData, String fileName, ClassOrInterfaceDeclaration c, MethodDeclaration method) {
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

                super.visit(n, arg);
                if (n.getScope().isEmpty() || !queryVariables.contains(n.getScope().get().toString())) {
                    return; // method not of interest, not used in any connect() method call
                }

                final VisInfo.VariableInfo variableInfo = findLocalVariableInfo(n);
                final String variableName = variableInfo == null ? null : variableInfo.getVariableName();
                if (variableName != null && allConnectedOperators.contains(variableName)) {
                    GraphOperator op = getCorrectOpClass(variableName, variableInfo.getOperatorType());
                    if (variableInfo.getVariableData() != null) {
                        op.setIdentifier2(variableInfo.getVariableData().split("\"", 3)[1]);
                    }
                    operators.put(variableName, op);
                    methodData.add(new Pair<>(new Node<>(op, null),
                            new VisInfo(fileName, c.getName().asString(), method.getNameAsString(), variableInfo)));
                }
            }
        };
    }

    @Nonnull
    @Override
    VoidVisitorAdapter<Void> methodParserFindVariables(List<Pair<Node<GraphOperator>, VisInfo>> methodData, String fileName, ClassOrInterfaceDeclaration c, MethodDeclaration method) {
        return new VoidVisitorAdapter<>() {
            /**
             * Finds all query variables and their types
             */
            @Override
            public void visit(VariableDeclarator n, Void arg) {
                super.visit(n, arg);
                //System.out.println("decl: " + n.getType() + "-" + n.getNameAsString());
                if (n.getType().asString().equals("Query")) {
                    //System.out.println("found query with name: " + n.getNameAsString());
                    queryVariables.add(n.getNameAsString());
                } else {
                    variableClasses.put(n.getNameAsString(), n.getType().asString());
                    //System.out.println("put " + n.getNameAsString() + ";" + n.getType().asString());
                }
            }

            /**
             * Finds all connected methods
             */
            @Override
            public void visit(MethodCallExpr n, Void arg) {
                super.visit(n, arg);
                final boolean isConnectMethodCall = n.getArguments().size() == 2 && n.getNameAsString().equals("connect");
                //System.out.println(n.getName() + ", " + isConnectMethodCall + ", " + n.getClass() + ", " + n);
                // 0. hitta X i "Query X = new Query();"
                // 1. find all connect()ed operators (their names)
                // 2. search for all their names and get their type/definition
                if (isConnectMethodCall) {
                    //System.out.println("connected: " + n.getArguments());
                    String from = n.getArguments().get(0).toString();
                    String to = n.getArguments().get(1).toString();
                    addToConnectedMap(from, to);
                    String[] split = n.toString().split("\\."); // query connect(ID, r) connect(r, sink)
                    if (split.length > 2) {
                        for (int i = 0; i < split.length - 1; i++) { // dont include last, it was processed above
                            String s = split[i].replace(" ", "").trim();
                            if (split[i].startsWith("connect(")) {
                                s = s.replace("connect(", "").replace(")", "");
                                String[] split2 = s.split(",");
                                if (split2.length == 2) {
                                    addToConnectedMap(split2[0], split2[1]);
                                }
                            }
                        }
                    }
                }
            }
        };
    }

}
