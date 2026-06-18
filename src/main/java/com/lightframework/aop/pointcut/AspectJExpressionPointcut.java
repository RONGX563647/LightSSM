package com.lightframework.aop.pointcut;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

public class AspectJExpressionPointcut {
    
    private String expression;
    private Pattern pattern;
    
    public AspectJExpressionPointcut(String expression) {
        this.expression = expression;
        parseExpression();
    }
    
    protected void parseExpression() {
        String regex = convertExpressionToRegex(this.expression);
        this.pattern = Pattern.compile(regex);
    }
    
    protected String convertExpressionToRegex(String expression) {
        if (expression.startsWith("execution(")) {
            // Strip the execution(...) wrapper; the inner content is what we match against
            String content = expression.substring("execution(".length(), expression.length() - 1).trim();
            // Remove the return-type part (up to the first space, e.g. "* " or "void ")
            int spaceIdx = content.indexOf(' ');
            if (spaceIdx > 0) {
                content = content.substring(spaceIdx + 1).trim();
            }
            // * matches a single segment (no dots), .. matches any segments
            String regex = content
                .replace(".", "\\.")
                .replace("*", "[^.]*")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace(" ", "\\s+");
            // .. was replaced by [^.]*[^.]* by the two above steps; collapse to .*
            regex = regex.replace("[^.]*[^.]*", ".*");
            return regex;
        }
        
        return expression.replace(".", "\\.").replace("*", "[^.]*").replace("..", ".*");
    }
    
    public boolean matches(Class<?> targetClass) {
        String className = targetClass.getName();
        return pattern.matcher(className).matches();
    }
    
    public boolean matches(Method method) {
        String methodSignature = method.getDeclaringClass().getName() + "." + method.getName();
        return pattern.matcher(methodSignature).matches();
    }
    
    public boolean matches(Class<?> targetClass, Method method) {
        return matches(targetClass) && matches(method);
    }
    
    public String getExpression() {
        return this.expression;
    }
}