package gui.graph.visualisation;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import gui.graph.dag.Node;
import gui.graph.data.GraphOperator;
import gui.graph.data.Operator;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

public class FlinkVisualiser extends Visualiser {
    @Nonnull
    @Override
    List<Pair<Node<GraphOperator>, VisInfo>> fixList(List<Pair<Node<GraphOperator>, VisInfo>> list) {
        return list;
    }

    @NotNull
    @Override
    VoidVisitorAdapter<Void> methodParserInit(List<Pair<Node<GraphOperator>, VisInfo>> methodData, String fileName, ClassOrInterfaceDeclaration c, MethodDeclaration method) {
        return new VoidVisitorAdapter<>() {
        };
    }

    @Nonnull
    @Override
    VoidVisitorAdapter<Void> methodParser(List<Pair<Node<GraphOperator>, VisInfo>> methodData, String fileName, ClassOrInterfaceDeclaration c, MethodDeclaration method) {
        return new MethodParser(methodData, fileName, c, method);
    }

    private static class MethodParser extends VoidVisitorAdapter<Void> {
        private final List<GraphOperator> ops = new LinkedList<>();
        private final List<Pair<Node<GraphOperator>, VisInfo>> methodData;
        private final String fileName;
        private final ClassOrInterfaceDeclaration c;
        private final MethodDeclaration method;

        public MethodParser(List<Pair<Node<GraphOperator>, VisInfo>> methodData, String fileName, ClassOrInterfaceDeclaration c, MethodDeclaration method) {
            this.methodData = methodData;
            this.fileName = fileName;
            this.c = c;
            this.method = method;
        }

        @Override
        public void visit(MethodCallExpr m, Void arg) {
            String name = m.getName().asString();

            ops.add(new Operator(name));

            super.visit(m, arg);

            Node<GraphOperator> n = null;
            for (GraphOperator op : ops) { // finds chained method calls
                if (n == null) {
                    n = new Node<>(op, null);
                } else {
                    List<Node<GraphOperator>> successors = new LinkedList<>();
                    successors.add(n);
                    n = new Node<>(op, successors);
                }
            }

            if (n != null) {
                Pair<String, String> pair = findLocalVariableInfo(m);
                if (pair != null) {
                    System.out.println("Found pair: " + pair);
                }
                final VisInfo.VariableInfo i = pair == null ? new VisInfo.VariableInfo(null, null, null)
                        : new VisInfo.VariableInfo(pair.getKey(), pair.getValue(), null);
                //VisInfo info = new VisInfo(fileName, c.getName().asString(), method.getNameAsString(), i);
                //methodData.add(new Pair<>(n, info));
                ops.clear();
            }

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
    }
}
