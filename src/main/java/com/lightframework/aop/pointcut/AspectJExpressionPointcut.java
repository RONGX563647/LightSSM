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
            String content = expression.substring("execution(".length(), expression.length() - 1);
            
            content = content.replace("*", ".*");
            content = content.replace("..", ".*");
            content = content.replace("(", "\\(");
            content = content.replace(")", "\\)");
            
            return content;
        }
        
        return expression.replace("*", ".*").replace("..", ".*");
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
        return matches(targetClass) || matches(method);
    }
    
    public String getExpression() {
        return this.expression;
    }
}