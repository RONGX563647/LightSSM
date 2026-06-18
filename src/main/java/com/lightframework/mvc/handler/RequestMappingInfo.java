package com.lightframework.mvc.handler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestMappingInfo {

    public static final String PATH_VARIABLES_ATTRIBUTE = "com.lightframework.mvc.PATH_VARIABLES";

    private final String pattern;
    private final Pattern compiledPattern;
    private final List<String> variableNames;

    public RequestMappingInfo(String pattern) {
        this.pattern = pattern;
        this.variableNames = new ArrayList<>();
        this.compiledPattern = compilePattern(pattern);
    }

    private Pattern compilePattern(String pattern) {
        StringBuilder regex = new StringBuilder();
        int i = 0;
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            if (c == '{') {
                int end = pattern.indexOf('}', i);
                if (end != -1) {
                    String varName = pattern.substring(i + 1, end);
                    variableNames.add(varName);
                    regex.append("([^/]+)");
                    i = end + 1;
                    continue;
                }
            }
            if (c == '.' || c == '*' || c == '(' || c == ')' || c == '+' || c == '^' || c == '$') {
                regex.append('\\');
            }
            regex.append(c);
            i++;
        }
        return Pattern.compile("^" + regex + "$");
    }

    public Map<String, String> match(String uri) {
        Matcher matcher = compiledPattern.matcher(uri);
        if (!matcher.matches()) {
            return null;
        }
        Map<String, String> variables = new LinkedHashMap<>();
        for (int j = 0; j < variableNames.size(); j++) {
            variables.put(variableNames.get(j), matcher.group(j + 1));
        }
        return variables;
    }

    public String getPattern() {
        return pattern;
    }
}
