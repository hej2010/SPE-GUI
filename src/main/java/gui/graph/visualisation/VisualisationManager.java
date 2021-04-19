package gui.graph.visualisation;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import gui.graph.dag.Node;
import gui.graph.data.GraphOperator;
import gui.spe.ParsedFlinkSPE;
import gui.spe.ParsedLiebreSPE;
import gui.spe.ParsedSPE;
import javafx.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;

public class VisualisationManager {

    public static List<Pair<Node<GraphOperator>, VisInfo>> projectFromFile(File file, ParsedSPE parsedSPE) {
        List<Pair<Node<GraphOperator>, VisInfo>> list = new LinkedList<>();

        Visualiser vis = null;
        if (parsedSPE instanceof ParsedLiebreSPE) {
            vis = new LiebreVisualiser(parsedSPE);
        } else if (parsedSPE instanceof ParsedFlinkSPE) {
            vis = new FlinkVisualiser(parsedSPE);
        }
        if (vis == null) {
            throw new IllegalStateException("Not implemented");
        }

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
