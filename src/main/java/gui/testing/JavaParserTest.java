package gui.testing;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.FileNotFoundException;

public class JavaParserTest {
    private static final String FILE_PATH = "src/main/java/gui/ApacheFlink1617009959485A.java";

    public static void main(String[] args) throws FileNotFoundException {
        CompilationUnit cu = StaticJavaParser.parse(new File(FILE_PATH));
        //VoidVisitor<Void> methodNameVisitor = new MethodNamePrinter();
        //methodNameVisitor.visit(cu, null);

        ClassOrInterfaceDeclaration m = cu.getClassByName("ApacheFlink1617009959485A").get();

        for (MethodDeclaration method : m.getMethods()) {
            // Make the visitor go through everything inside the method.
            method.accept(new MethodCallVisitor(), null);
        }

    }

    private static class MethodNamePrinter extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(MethodDeclaration md, Void arg) {
            super.visit(md, arg);
            System.out.println("Method Name Printed: " + md.getName());
        }
    }

    private static class MethodCallVisitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(MethodCallExpr n, Void arg) {
            // Found a method call
            //System.out.println(n.getScope().get() + "." + n.getName());
            System.out.println(n.getName());
            //System.out.println(n.toString()); // whole method call, i.e. "scope.name(...)"
            //System.out.println(n.getParentNode()); // parent, i.e. variable in which result is saved
            if (n.getParentNode().isPresent()) {
                Node parentNode = n.getParentNode().get();
                //System.out.println(parentNode.toString().split("=")[0].trim()); // parent, i.e. variable in which result is saved
                if (parentNode.getParentNode().isPresent()) {
                    Node parentsParent = parentNode.getParentNode().get();
                    if (!parentsParent.toString().startsWith("{")) {
                        //System.out.println(parentsParent.toString().split("=")[0].trim()); // if parent has parent then this is a new variable, i.e. "Parent node = scope.name(..)"
                    }
                }
            }
            // Don't forget to call super, it may find more method calls inside the arguments of this method call, for example.
            super.visit(n, arg);
            System.out.println("-----------------------");
        }
    }
}
