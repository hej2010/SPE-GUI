package se.chalmers.datx05.spe;

import org.json.JSONArray;
import org.json.JSONObject;
import se.chalmers.datx05.utils.Files;
import se.chalmers.datx05.utils.SPE;

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

    public static void main(String[] args) {
        try {
            parseSPE(SPE.LIEBRE);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static ParsedSPE parseSPE(@Nonnull SPE spe) throws IOException, URISyntaxException {
        URL url = SPEParser.class.getClassLoader().getResource("org/example");
        if (url == null) {
            throw new RuntimeException("Resources not found");
        }
        String file = Files.readFile(Paths.get(Paths.get(url.toURI()).toString(), spe.fileName).toString());

        JSONObject fileObj = new JSONObject(file);
        //System.out.println(fileObj.toString(2));

        final String name = fileObj.getString(NAME);
        final List<String> opsList = getStringListFromArr(fileObj.getJSONArray(OPERATORS));
        JSONObject objImports = fileObj.getJSONObject(IMPORTS);
        JSONObject objDefs = fileObj.getJSONObject(DEFINITION);
        final List<String> baseImports = getValuesIfExistsOrEmpty(BASE, objImports);
        final List<String> baseDefinition = getValuesIfExistsOrEmpty(BASE, objDefs);
        final Map<String, List<String>> operatorImportsMap = new HashMap<>();
        Map<String, ParsedOperator.Definition> definitionMap = new HashMap<>();
        List<ParsedOperator> parsedOperators = new LinkedList<>();
        for (String s : opsList) {
            if (objImports.has(s)) {
                operatorImportsMap.put(s, getStringListFromArr(objImports.getJSONArray(s)));
            }
            if (!objDefs.has(s)) {
                System.err.println("Missing definition for " + s + " in " + spe.fileName);
                //throw new RuntimeException("Missing definition for " + s + " in " + spe.fileName);
                continue;
            }
            parsedOperators.add(new ParsedOperator(s, getDefinition(objDefs.getJSONObject(s))));
        }


        if (spe == SPE.FLINK) {
            return new ParsedFlinkSPE(name, parsedOperators, baseImports, baseDefinition, operatorImportsMap);
        } else if (spe == SPE.LIEBRE) {
            return new ParsedLiebreSPE(name, parsedOperators, baseImports, baseDefinition, operatorImportsMap);
        }

        throw new RuntimeException();
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
        return new ParsedOperator.Definition(codeBefore, codeMiddle, codeAfter, input, output, identifier);
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
