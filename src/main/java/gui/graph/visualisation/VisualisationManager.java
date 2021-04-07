package gui.graph.visualisation;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitorWithDefaults;
import gui.graph.dag.Node;
import gui.graph.data.GraphOperator;
import gui.graph.data.Operator;
import gui.spe.ParsedSPE;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

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
            method.accept(new VoidVisitorAdapter<Void>() {
                final List<GraphOperator> ops = new LinkedList<>();

                @Override
                public void visit(MethodCallExpr m, Void arg) {
                    String name = m.getName().asString();
                    //System.out.println(name);

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
                    //System.out.println(m);
                    //System.err.println(m.getParentNode().get());
                    if (n != null) {
                        MethodInfo variables = new MethodInfo(null, null, null);
                        if (m.getParentNode().isPresent()) {
                            System.out.println(m.getParentNode().get());
                            variables = findVariable(m.getParentNode().get());
                        }
                        VisInfo info = new VisInfo(fileName, c.getName().asString(), method.getNameAsString(), variables);
                        data.add(new Pair<>(n, info));
                        ops.clear();
                    }

                    //System.out.println("-------------");
                }

                private MethodInfo findVariable(com.github.javaparser.ast.Node n) {
                    if (n.getParentNode().isPresent()) {
                        com.github.javaparser.ast.Node n2 = n.getParentNode().get();
                        String s = n.toString();

                        if (!s.startsWith("{")) {
                            if (s.contains("=")) {
                                String variableName = s.split("=", 2)[0];
                                String variableClass = null;
                                if (n2.getParentNode().isPresent()) {
                                    String s2 = n2.getParentNode().get().toString();
                                    variableClass = s2.split(variableName, 2)[0];
                                }
                                n2.accept(new VoidVisitorAdapter<Void>() {
                                    @Override
                                    public void visit(ClassOrInterfaceType n, Void arg) {
                                        System.err.println(n.getNameAsString() + "\n------------------------");
                                        //super.visit(n, arg);
                                    }
                                }, null);
                                return extractVariable(variableName);
                            }
                            return findVariable(n.getParentNode().get());
                        } else {
                            //System.out.println(n.toString());
                            return extractVariable(n.toString());
                        }
                    } else {
                        //System.out.println(n);
                        return extractVariable(n.toString());
                    }
                }

                private MethodInfo extractVariable(String s) {
                    if (s.contains("=")) {
                        String[] s2 = s.split("=", 2);
                        String variableClass = null;
                        String variableName;
                        if (s2[0].contains(" ")) {
                            String[] s3 = s2[0].split(" ");
                            variableName = s3[s3.length - 1];
                            variableClass = s3[s3.length - 2];
                        } else {
                            variableName = s2[0];
                        }
                        return new MethodInfo(variableName, variableClass, s2[1].split("\\.", 2)[0]);
                    } else {
                        return new MethodInfo(null, null, s.split("\\.", 2)[0]);
                    }
                }
            }, null);
        }

        return data;
    }

    private static class MethodInfo {
        @Nullable
        private final String variableName, calledWithVariableName, variableClass;

        private MethodInfo(@Nullable String variableName, @Nullable String calledWithVariableName, @Nullable String variableClass) {
            this.variableName = variableName;
            this.calledWithVariableName = calledWithVariableName;
            this.variableClass = variableClass;
        }

        @Override
        public String toString() {
            return "MethodInfo{" +
                    "variableName='" + variableName + '\'' +
                    ", calledWithVariableName='" + calledWithVariableName + '\'' +
                    ", variableClass='" + variableClass + '\'' +
                    '}';
        }
    }

    public static class VisInfo {
        @Nonnull
        public final String fileName, className, methodName;
        @Nonnull
        public final VariableInfo variableInfo;

        private VisInfo(@Nonnull String fileName, @Nonnull String className, @Nonnull String methodName, @Nonnull MethodInfo methodInfo) {
            this.fileName = fileName;
            this.className = className;
            this.methodName = methodName;
            this.variableInfo = new VariableInfo(methodInfo.variableName, methodInfo.calledWithVariableName, methodInfo.variableClass);
        }

        public static class VariableInfo {
            @Nullable
            public final String variableName, calledWithVariableName, variableClass;
            public final boolean savedInExistingVariable, savedInNewVariable;

            private VariableInfo(@Nullable String variableName, @Nullable String calledWithVariableName, @Nullable String variableClass) {
                this.variableName = variableName;
                this.calledWithVariableName = calledWithVariableName;
                this.variableClass = variableClass;
                this.savedInExistingVariable = variableClass == null;
                this.savedInNewVariable = !savedInExistingVariable;
            }

        }
    }

}
