package com.lightframework.ioc.exception;

import java.util.Arrays;
import java.util.Comparator;

public class NoSuchBeanDefinitionException extends BeansException {
    private final Class<?> requiredType;
    
    public NoSuchBeanDefinitionException(String name) {
        super("No bean named '" + name + "' available");
        this.requiredType = null;
    }
    
    public NoSuchBeanDefinitionException(String name, String[] availableNames) {
        super(buildMessageWithSuggestions(name, availableNames));
        this.requiredType = null;
    }
    
    public NoSuchBeanDefinitionException(Class<?> type) {
        super("No qualifying bean of type '" + type.getName() + "' available");
        this.requiredType = type;
    }
    
    public NoSuchBeanDefinitionException(Class<?> type, String message) {
        super(message);
        this.requiredType = type;
    }
    
    public Class<?> getRequiredType() {
        return this.requiredType;
    }

    // Phase 3: compute Levenshtein distance for smart suggestions
    private static String buildMessageWithSuggestions(String name, String[] availableNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("No bean named '").append(name).append("' available");
        if (availableNames != null && availableNames.length > 0) {
            String[] candidates = Arrays.stream(availableNames)
                .filter(n -> !n.equals(name))
                .sorted(Comparator.comparingInt(n -> levenshteinDistance(name, n)))
                .limit(3)
                .toArray(String[]::new);
            if (candidates.length > 0) {
                sb.append("\n  Did you mean?\n    -> ");
                sb.append(String.join("\n    -> ", candidates));
            }
        }
        sb.append("\n  Check:\n    - Is the class annotated with @Component?\n    - Is the package included in scan(\"...\")?");
        return sb.toString();
    }

    private static int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }
}