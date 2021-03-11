package org.example.spe;

import org.example.utils.Files;
import org.example.utils.SPE;
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
    private static final String IMPORTS = "imports";
    private static final String DEFINITION = "definition";
    private static final String BASE = "base";

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
        System.out.println(fileObj.toString(2));

        final String name = fileObj.getString(NAME);
        final List<String> opsList = getStringListFromArr(fileObj.getJSONArray(OPERATORS));
        JSONObject objImports = fileObj.getJSONObject(IMPORTS);
        JSONObject objDefs = fileObj.getJSONObject(DEFINITION);
        final List<String> baseImports = getValuesIfExists(BASE, objImports);
        final List<String> baseDefinition = getValuesIfExists(BASE, objDefs);
        final Map<String, List<String>> operatorImportsMap = new HashMap<>();
        Map<String, ParsedOperator.Definition> definitionMap = new HashMap<>();
        for (String s : opsList) {
            if (objImports.has(s)) {
                operatorImportsMap.put(s, getStringListFromArr(objImports.getJSONArray(s)));
            }
            if (objDefs.has(s)) {

            }
        }

        return null;
    }

    private static List<String> getValuesIfExists(@Nonnull String key, @Nonnull JSONObject o) {
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
