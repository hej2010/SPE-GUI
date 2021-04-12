package gui.spe;

import gui.graph.data.GraphOperator;
import gui.graph.data.Operator;
import gui.graph.data.SinkOperator;
import gui.graph.data.SourceOperator;
import gui.utils.Files;
import gui.utils.SPE;
import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class SPEParser {
    private static final String NAME = "name";
    private static final String OPERATORS = "operators";
    private static final String SOURCES = "sources";
    private static final String SINKS = "sinks";
    private static final String REGULAR = "regular";
    private static final String IMPORTS = "imports";
    private static final String DEFINITION = "definition";
    private static final String BASE = "base";
    private static final String BEFORE = "before";
    private static final String MIDDLE = "middle";
    private static final String AFTER = "after";
    private static final String PLACEHOLDERS = "placeholders";
    private static final String INPUT = "input";
    private static final String OUTPUT = "output";
    private static final String IDENTIFIER = "identifier";
    private static final String PREV_IDENTIFIER = "prev_identifier";
    private static final String LINKS = "links";
    private static final String OP = "op";
    private static final String SOURCE = "source";
    private static final String SINK = "sink";

    public static ParsedSPE parseSPE(@Nonnull SPE spe) throws IOException, URISyntaxException {
        URL url = SPEParser.class.getClassLoader().getResource("gui");
        if (url == null) {
            throw new RuntimeException("Resources not found");
        }
        String file = Files.readFile(Paths.get(Paths.get(url.toURI()).toString(), spe.fileName).toString());

        JSONObject fileObj = new JSONObject(file);
        //System.out.println(fileObj.toString(2));

        final String name = fileObj.getString(NAME);
        final JSONObject operators = fileObj.getJSONObject(OPERATORS);
        final List<String> opsSources = getStringListFromArr(operators.getJSONArray(SOURCES));
        final List<String> opsSinks = getStringListFromArr(operators.getJSONArray(SINKS));
        final List<String> opsRegular = getStringListFromArr(operators.getJSONArray(REGULAR));
        JSONObject objImports = fileObj.getJSONObject(IMPORTS);
        JSONObject objDefs = fileObj.getJSONObject(DEFINITION);
        final List<String> baseImports = getValuesIfExistsOrEmpty(BASE, objImports);
        final List<String> baseDefinition = getValuesIfExistsOrEmpty(BASE, objDefs);
        final Map<String, List<String>> operatorImportsMap = new HashMap<>();

        List<ParsedOperator> parsedOperators = new LinkedList<>();
        addParsedOperatorToList(parsedOperators, opsSources, ParsedOperator.TYPE_SOURCE_OPERATOR, objImports, operatorImportsMap, objDefs, spe);
        addParsedOperatorToList(parsedOperators, opsRegular, ParsedOperator.TYPE_REGULAR_OPERATOR, objImports, operatorImportsMap, objDefs, spe);
        addParsedOperatorToList(parsedOperators, opsSinks, ParsedOperator.TYPE_SINK_OPERATOR, objImports, operatorImportsMap, objDefs, spe);
        Map<String, Pair<Class<? extends GraphOperator>, String>> codeToOpMap = getCodeToOpMap(fileObj.getJSONObject(LINKS));

        if (spe == SPE.FLINK) {
            return new ParsedFlinkSPE(name, parsedOperators, baseImports, baseDefinition, operatorImportsMap, codeToOpMap);
        } else if (spe == SPE.LIEBRE) {
            return new ParsedLiebreSPE(name, parsedOperators, baseImports, baseDefinition, operatorImportsMap, codeToOpMap);
        }

        throw new RuntimeException();
    }

    private static Map<String, Pair<Class<? extends GraphOperator>, String>> getCodeToOpMap(JSONObject links) {
        Map<String, Pair<Class<? extends GraphOperator>, String>> map = new HashMap<>();
        for (String key : links.keySet()) {
            String[] s = links.getString(key).split(":");
            String type = s[0];
            String op = s[1];
            Class<? extends GraphOperator> c;
            if (type.equals(OP)) {
                c = Operator.class;
            } else if (type.equals(SOURCE)) {
                c = SourceOperator.class;
            } else {
                c = SinkOperator.class;
            }
            map.put(key, new Pair<>(c, op));
        }
        return map;
    }

    private static void addParsedOperatorToList(List<ParsedOperator> parsedOperators, List<String> list, int type, JSONObject objImports, Map<String, List<String>> operatorImportsMap, JSONObject objDefs, SPE spe) {
        for (String s : list) {
            if (objImports.has(s)) {
                operatorImportsMap.put(s, getStringListFromArr(objImports.getJSONArray(s)));
            }
            if (!objDefs.has(s)) {
                System.err.println("Missing definition for " + s + " in " + spe.fileName);
                continue;
            }
            parsedOperators.add(new ParsedOperator(s, getDefinition(objDefs.getJSONObject(s)), type));
        }
    }

    @Nonnull
    private static ParsedOperator.Definition getDefinition(@Nonnull JSONObject o) {
        String codeBefore = getStringOrEmpty(BEFORE, o);
        String codeMiddle = getStringOrEmpty(MIDDLE, o);
        String codeAfter = getStringOrEmpty(AFTER, o);
        o = o.getJSONObject(PLACEHOLDERS);
        List<String> input = getStringListFromArr(o.getJSONArray(INPUT));
        List<String> output = getStringListFromArr(o.getJSONArray(OUTPUT));
        String identifier = getStringOrEmpty(IDENTIFIER, o);
        String prevIdentifier = getStringOrEmpty(PREV_IDENTIFIER, o);
        if (prevIdentifier.isEmpty()) {
            prevIdentifier = null;
        }
        return new ParsedOperator.Definition(codeBefore, codeMiddle, codeAfter, input, output, identifier, prevIdentifier);
    }

    private static String getStringOrEmpty(String s, JSONObject o) {
        if (o.has(s)) {
            return o.getString(s);
        }
        return "";
    }

    @Nonnull
    private static List<String> getValuesIfExistsOrEmpty(@Nonnull String key, @Nonnull JSONObject o) {
        List<String> baseDefinition;
        if (o.has(BASE)) {
            baseDefinition = getStringListFromArr(o.getJSONArray(BASE));
        } else {
            baseDefinition = new LinkedList<>();
        }
        return baseDefinition;
    }

    @Nonnull
    private static List<String> getStringListFromArr(@Nonnull JSONArray arr) {
        List<String> list = new LinkedList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(arr.getString(i));
        }
        return list;
    }

}
