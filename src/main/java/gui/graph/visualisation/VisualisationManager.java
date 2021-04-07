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
import gui.spe.ParsedFlinkSPE;
import gui.spe.ParsedLiebreSPE;
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

        Visualiser vis = null;
        if (parsedSPE instanceof ParsedLiebreSPE) {
            vis = new LiebreVisualiser();
        } else if (parsedSPE instanceof ParsedFlinkSPE) {
            vis = new FlinkVisualiser();
        }
        if (vis == null) {
            throw new IllegalStateException("Not implemented");
        }
        //return vis == null ? new LinkedList<>() : vis.fixList(list);

        JavaParser javaParser = new JavaParser();
        javaParser.getParserConfiguration().setAttributeComments(false); // ignore all comments
        CompilationUnit cu;
        try {
            cu = javaParser.parse(file).getResult().get();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return list;
        }

        List<ClassOrInterfaceDeclaration> classes = vis.findClasses(cu);

        for (ClassOrInterfaceDeclaration c : classes) {
            list.addAll(vis.getClassData(file.getName(), c));
        }

        list = vis.fixList(list);

        return list;
    }

}
