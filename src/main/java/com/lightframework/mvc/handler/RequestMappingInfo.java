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
    private final String method;

    public RequestMappingInfo(String pattern) {
        this(pattern, "");
    }

    public RequestMappingInfo(String pattern, String method) {
        this.pattern = pattern;
        this.variableNames = new ArrayList<>();
        this.compiledPattern = compilePattern(pattern);
        this.method = method != null ? method.toUpperCase() : "";
    }

    private Pattern compilePattern(String pattern) {
        // TODO [L1][练习] 当前 {var} 只编译成 ([^/]+)；请支持可选变量 {var?}（正则用 (/[^/]+)?）与带约束变量 {var:\\d+}（用其中的正则），；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        //   让路由既能匹配带/不带后缀，也能对变量做格式限制。验收标准：/user/{id:\\d+} 只匹配数字 id。
        // TODO [L2][练习] 当前只对 . * ( ) + ^ $ 做了转义；请补全对 ? 与 [ ] 等正则元字符的转义（或统一用 Pattern.quote 处理字面量段），；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        //   避免用户路径中的特殊字符被当成正则、造成正则注入或误匹配。验收标准：路径含 "?" 能被当作普通字符精确匹配。
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

    public Map<String, String> match(String uri, String httpMethod) {
        Matcher matcher = compiledPattern.matcher(uri);
        if (!matcher.matches()) {
            return null;
        }
        // TODO [L2][练习] 当前 match 仅按路径模板与 HTTP 方法匹配；请支持附加条件（请求头、consumes/produces），；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        //   使 /user/{id} 这类映射能在匹配成功后进一步按 headers/Content-Type 过滤。验收标准：带必需要求头的请求才命中，否则返回 null。
        if (!method.isEmpty() && httpMethod != null && !method.equalsIgnoreCase(httpMethod)) {
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

    public String getMethod() {
        return method;
    }
}
