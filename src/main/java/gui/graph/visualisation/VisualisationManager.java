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

    public static List<Pair<Node<GraphOperator>, VisInfo>> visualiseFromFile(File file, ParsedSPE parsedSPE) {
        List<Pair<Node<GraphOperator>, VisInfo>> list = new LinkedList<>();

        Visualiser visualiser = null;
        if (parsedSPE instanceof ParsedLiebreSPE) {
            visualiser = new LiebreVisualiser(parsedSPE);
        } else if (parsedSPE instanceof ParsedFlinkSPE) {
            visualiser = new FlinkVisualiser(parsedSPE);
        }
        if (visualiser == null) {
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

        List<ClassOrInterfaceDeclaration> classes = visualiser.findClasses(cu);

        for (ClassOrInterfaceDeclaration c : classes) {
            list.addAll(visualiser.getClassData(file.getName(), c));
        }

        list = visualiser.fixList(list);

        return list;
    }

}
