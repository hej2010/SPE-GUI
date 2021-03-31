package gui.graph.visualisation;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import gui.graph.dag.Node;
import gui.graph.data.GraphOperator;
import gui.graph.data.Operator;
import gui.spe.ParsedSPE;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;

public class VisualisationManager {

    public static List<Pair<Node<GraphOperator>, VisInfo>> projectFromFile(File file, ParsedSPE parsedSPE) {
        List<Pair<Node<GraphOperator>, VisInfo>> list = new LinkedList<>();

        JavaParser javaParser = new JavaParser();
        CompilationUnit cu;
        try {
            cu = javaParser.parse(file).getResult().get();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return list;
        }

        List<ClassOrInterfaceDeclaration> classes = findClasses(cu);

        for (ClassOrInterfaceDeclaration c : classes) {
            list.addAll(getClassData(file.getName(), c));
        }

        return list;
    }

    @Nonnull
    private static List<ClassOrInterfaceDeclaration> findClasses(CompilationUnit cu) {
        List<ClassOrInterfaceDeclaration> classes = new LinkedList<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                System.out.println(n.getName()); // Name of class
                classes.add(n);
                super.visit(n, arg);
            }
        }, null);

        return classes;
    }

    @Nonnull
    private static List<Pair<Node<GraphOperator>, VisInfo>> getClassData(String fileName, ClassOrInterfaceDeclaration c) {
        List<Pair<Node<GraphOperator>, VisInfo>> data = new LinkedList<>();

        for (MethodDeclaration method : c.getMethods()) {
            // Make the visitor go through everything inside the method.
            VisInfo info = new VisInfo(fileName, c.getName().asString(), method.getNameAsString());
            method.accept(new VoidVisitorAdapter<Void>() {
                final List<GraphOperator> ops = new LinkedList<>();

                @Override
                public void visit(MethodCallExpr methodCallExpr, Void arg) {
                    String name = methodCallExpr.getName().asString();
                    System.out.println(name);

                    ops.add(new Operator(name));

                    super.visit(methodCallExpr, arg);

                    Node<GraphOperator> n = null;
                    for (GraphOperator op : ops) {
                        if (n == null) {
                            n = new Node<>(op, null);
                        } else {
                            List<Node<GraphOperator>> successors = new LinkedList<>();
                            successors.add(n);
                            n = new Node<>(op, successors);
                        }
                    }
                    if (n != null) {
                        System.out.println(n);
                        data.add(new Pair<>(n, info));
                        ops.clear();
                    }

                    System.out.println("-------------");
                }
            }, null);
        }

        return data;
    }

    public static class VisInfo {
        public final String fileName, className, methodName;

        private VisInfo(String fileName, String className, String methodName) {
            this.fileName = fileName;
            this.className = className;
            this.methodName = methodName;
        }
    }

}
