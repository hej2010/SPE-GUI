package gui.graph.visualisation;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
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

public class VisualisationManager {

    public static List<Pair<Node<GraphOperator>, VisInfo>> projectFromFile(File file, ParsedSPE parsedSPE) {
        List<Pair<Node<GraphOperator>, VisInfo>> list = new LinkedList<>();

        JavaParser javaParser = new JavaParser();
        javaParser.getParserConfiguration().setAttributeComments(false); // ignore all comments
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
            List<Pair<Node<GraphOperator>, VisInfo>> methodData = new LinkedList<>();
            // Make the visitor go through everything inside the method.
            method.accept(new MethodParser(methodData, fileName, c, method), null);

            BlockStmt block = method.getBody().orElse(null);
            if (block == null) {
                System.err.println("Block empty in " + method.getNameAsString());
            } else {
                List<VariableDeclarator> list = block.findAll(VariableDeclarator.class);
                if (list != null && !list.isEmpty() && !methodData.isEmpty()) {
                    for (VariableDeclarator v : list) {
                        for (Pair<Node<GraphOperator>, VisInfo> p : methodData) {
                            String name = v.getNameAsString();
                            if (name.equals(p.getValue().variableInfo.variableName)) {
                                p.getValue().variableInfo.setVariableClass(v.getType().asString());
                                System.out.println("found match for " + name + " (" + v.getType().asString() + ")");
                                break;
                            }
                        }
                    }
                }
            }

            data.addAll(methodData);
        }

        return data;
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
                Pair<String, String> variableName = findLocalVariableName(m);
                //System.out.println("Found pair: " + variableName);
                final VisInfo.VariableInfo i = variableName == null ? new VisInfo.VariableInfo(null, null, null)
                        : new VisInfo.VariableInfo(variableName.getKey(), variableName.getValue(), null);
                VisInfo info = new VisInfo(fileName, c.getName().asString(), method.getNameAsString(), i);
                methodData.add(new Pair<>(n, info));
                ops.clear();
            }

            // https://stackoverflow.com/questions/51117783/how-to-find-type-of-a-variable-while-reading-a-java-source-file
        }

        private Pair<String, String> findLocalVariableName(com.github.javaparser.ast.Node n) {
            if (n.getParentNode().isPresent()) {
                com.github.javaparser.ast.Node parent = n.getParentNode().get();
                String s = parent.toString();
                if (s.startsWith("{")) { // no variable
                    return null;
                } else if (s.contains("=")) { // we found a variable
                    String[] strings = s.split("=", 2);
                    return new Pair<>(strings[0], strings[1].split("\\.", 2)[0]);
                } else { // no variable yet, search from parent
                    return findLocalVariableName(parent);
                }
            }
            return null;
        }
    }

    public static class VisInfo {
        @Nonnull
        private final String fileName, className, methodName;
        @Nonnull
        public final VariableInfo variableInfo;

        private VisInfo(@Nonnull String fileName, @Nonnull String className, @Nonnull String methodName, @Nonnull VariableInfo variableInfo) {
            this.fileName = fileName;
            this.className = className;
            this.methodName = methodName;
            this.variableInfo = new VariableInfo(variableInfo.variableName, variableInfo.calledWithVariableName, variableInfo.variableClass);
        }

        public static class VariableInfo {
            @Nullable
            private final String calledWithVariableName;
            @Nullable
            private String variableName, variableClass;
            public final boolean savedInExistingVariable, savedInNewVariable;

            private VariableInfo(@Nullable String variableName, @Nullable String calledWithVariableName, @Nullable String variableClass) {
                this.variableName = variableName == null ? null : variableName.trim();
                this.calledWithVariableName = calledWithVariableName == null ? null : calledWithVariableName.trim();
                this.variableClass = variableClass == null ? null : variableClass.trim();
                this.savedInExistingVariable = variableClass == null;
                this.savedInNewVariable = !savedInExistingVariable;
            }

            @Nullable
            public String getCalledWithVariableName() {
                return calledWithVariableName;
            }

            @Nullable
            public String getVariableName() {
                return variableName;
            }

            public void setVariableName(@Nullable String variableName) {
                this.variableName = variableName == null ? null : variableName.trim();
            }

            @Nullable
            public String getVariableClass() {
                return variableClass;
            }

            public void setVariableClass(@Nullable String variableClass) {
                this.variableClass = variableClass == null ? null : variableClass.trim();
            }
        }

        @Nonnull
        public String getFileName() {
            return fileName;
        }

        @Nonnull
        public String getClassName() {
            return className;
        }

        @Nonnull
        public String getMethodName() {
            return methodName;
        }

    }


                        /*

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


                        com.github.javaparser.ast.Node root = n;
                        int count = 0;
                        while (root.getParentNode().isPresent()) {
                            if (count == 2) {
                                break;
                            }
                            com.github.javaparser.ast.Node newRoot = root.getParentNode().get();
                            String s = newRoot.toString();
                            if (s.startsWith("{")) {
                                break;
                            }
                            count++;
                            root = newRoot;
                        }
                        System.err.println("found root: " + root);
                        return new MethodInfo(null,null,null);

                        com.github.javaparser.ast.Node n2 = n.getParentNode().get(); // n2 should be an Object (wanted) or start with { (no new variable)
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
                    }*/

}
