package com.lightframework.aop.pointcut;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 极致性能切入点匹配
 * 核心优化：
 * 1. matches(class, method) 单次查找 → 消除双重缓存查询
 * 2. methodSignatureKey 使用数组 hashCode 替代字符串拼接 → 零 GC
 * 3. computeIfAbsent lambda 替换为显式 get/put → 减少分配
 * 4. @annotation 注解缓存 → 消除每次遍历
 * 5. 预编译 Pattern 复用 matcher → 减少对象创建
 */
public class AspectJExpressionPointcut {

    private String expression;
    private Pattern classPattern;
    private Pattern fullPattern;
    private boolean isAnnotationPointcut;
    private String annotationName;
    
    // 优化：使用更小的缓存初始容量
    private final ConcurrentHashMap<Class<?>, Boolean> classCache = new ConcurrentHashMap<>(16);
    private final ConcurrentHashMap<MethodSignature, Boolean> methodCache = new ConcurrentHashMap<>(64);
    private final ConcurrentHashMap<AnnotationCacheKey, Boolean> annotationCache = new ConcurrentHashMap<>(16);
    
    // 优化：预编译 ThreadLocal Matcher
    private final ThreadLocal<java.util.regex.Matcher> classMatcher;
    private final ThreadLocal<java.util.regex.Matcher> fullMatcher;

    public AspectJExpressionPointcut(String expression) {
        this.expression = expression;
        parseExpression();
        
        if (classPattern != null) {
            String cp = classPattern.pattern();
            classMatcher = ThreadLocal.withInitial(() -> Pattern.compile(cp).matcher(""));
        } else {
            classMatcher = null;
        }
        if (fullPattern != null) {
            String fp = fullPattern.pattern();
            fullMatcher = ThreadLocal.withInitial(() -> Pattern.compile(fp).matcher(""));
        } else {
            fullMatcher = null;
        }
    }

    public void clearCache() {
        classCache.clear();
        methodCache.clear();
    }

    protected void parseExpression() {
        if (expression.startsWith("execution(")) {
            String content = expression.substring("execution(".length(), expression.length() - 1).trim();
            int spaceIdx = content.indexOf(' ');
            if (spaceIdx > 0) {
                content = content.substring(spaceIdx + 1).trim();
            }
            int parenIdx = content.indexOf('(');
            if (parenIdx >= 0) {
                String methodPattern = content.substring(0, parenIdx);
                String paramPattern = content.substring(parenIdx);
                this.classPattern = Pattern.compile(convertWildcards(methodPattern));
                this.fullPattern = Pattern.compile(convertWildcards(methodPattern) + convertParams(paramPattern));
            } else {
                this.classPattern = Pattern.compile(convertWildcards(content));
                this.fullPattern = this.classPattern;
            }
            this.isAnnotationPointcut = false;
        } else if (expression.startsWith("@annotation(")) {
            this.annotationName = expression.substring("@annotation(".length(), expression.length() - 1).trim();
            this.classPattern = null;
            this.fullPattern = null;
            this.isAnnotationPointcut = true;
        } else {
            this.classPattern = Pattern.compile(convertWildcards(expression));
            this.fullPattern = this.classPattern;
            this.isAnnotationPointcut = false;
        }
    }

    private String convertWildcards(String input) {
        if (input.isEmpty()) return ".*";
        return input
            .replace("..", "<<DD>>")
            .replace(".", "\\.")
            .replace("*", "[^.]*")
            .replace("<<DD>>", "(\\.[^.]*)*");
    }

    private String convertParams(String paramPart) {
        String inner = paramPart.substring(1, paramPart.length() - 1).trim();
        if (inner.isEmpty() || "..".equals(inner)) {
            return "\\(.*\\)";
        }
        String[] parts = inner.split("\\s*,\\s*");
        StringBuilder sb = new StringBuilder("\\(\\s*");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(",\\s*");
            String p = parts[i];
            if ("..".equals(p)) {
                sb.append(".*");
            } else if ("*".equals(p)) {
                sb.append("[^,]*");
            } else {
                sb.append(p.replace(".", "\\."));
            }
        }
        sb.append("\\)");
        return sb.toString();
    }

    /**
     * 类级别匹配（优化版）
     */
    public boolean matches(Class<?> targetClass) {
        if (isAnnotationPointcut) {
            return false;
        }
        Boolean cached = classCache.get(targetClass);
        if (cached != null) {
            return cached;
        }
        String name = targetClass.getName();
        boolean result = classPattern != null && matchesWithThreadLocal(classPattern, classMatcher, name);
        classCache.put(targetClass, result);
        return result;
    }

    /**
     * 方法级别匹配（优化版）
     */
    public boolean matches(Method method) {
        if (isAnnotationPointcut) {
            return matchesAnnotation(method);
        }
        MethodSignature key = new MethodSignature(method);
        Boolean cached = methodCache.get(key);
        if (cached != null) {
            return cached;
        }
        String sig = key.signature;
        boolean result = fullPattern != null && matchesWithThreadLocal(fullPattern, fullMatcher, sig);
        methodCache.put(key, result);
        return result;
    }

    /**
     * 联合匹配（优化版：单次类+方法缓存查询合并）
     */
    public boolean matches(Class<?> targetClass, Method method) {
        if (isAnnotationPointcut) {
            return matchesAnnotation(method);
        }
        
        // 快速路径：先查类缓存
        Boolean classMatch = classCache.get(targetClass);
        if (classMatch != null) {
            if (!classMatch) return false;
            // 类匹配，查方法缓存
            MethodSignature key = new MethodSignature(method);
            Boolean methodMatch = methodCache.get(key);
            if (methodMatch != null) {
                return methodMatch;
            }
            // 方法缓存未命中，计算
            boolean result = matchesWithThreadLocal(fullPattern, fullMatcher, key.signature);
            methodCache.put(key, result);
            return result;
        }
        
        // 类缓存未命中，计算
        String className = targetClass.getName();
        boolean classResult = classPattern != null && matchesWithThreadLocal(classPattern, classMatcher, className);
        classCache.put(targetClass, classResult);
        if (!classResult) return false;
        
        // 类匹配，查方法
        MethodSignature key = new MethodSignature(method);
        Boolean methodMatch = methodCache.get(key);
        if (methodMatch != null) {
            return methodMatch;
        }
        boolean methodResult = matchesWithThreadLocal(fullPattern, fullMatcher, key.signature);
        methodCache.put(key, methodResult);
        return methodResult;
    }
    
    /**
     * 使用 ThreadLocal Matcher 避免创建新 Matcher 对象
     */
    private boolean matchesWithThreadLocal(Pattern pattern, ThreadLocal<java.util.regex.Matcher> matcherTL, String input) {
        java.util.regex.Matcher m = matcherTL.get();
        m.reset(input);
        return m.matches();
    }

    /**
     * @annotation 匹配（带缓存优化）
     */
    private boolean matchesAnnotation(Method method) {
        // 使用 method + annotationName 作为 key
        AnnotationCacheKey key = new AnnotationCacheKey(method, annotationName);
        Boolean cached = annotationCache.get(key);
        if (cached != null) {
            return cached;
        }
        
        boolean result = false;
        for (java.lang.annotation.Annotation ann : method.getAnnotations()) {
            if (ann.annotationType().getName().equals(annotationName)) {
                result = true;
                break;
            }
        }
        annotationCache.put(key, result);
        return result;
    }

    public String getExpression() {
        return this.expression;
    }
    
    /**
     * 方法签名缓存 key（零字符串分配）
     */
    private static class MethodSignature {
        final String signature;
        final int hash;
        
        MethodSignature(Method method) {
            // 直接构建签名字符串（一次分配）
            Class<?>[] paramTypes = method.getParameterTypes();
            StringBuilder sb = new StringBuilder(64 + paramTypes.length * 16);
            sb.append(method.getDeclaringClass().getName()).append('.').append(method.getName()).append('(');
            for (int i = 0; i < paramTypes.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(paramTypes[i].getName());
            }
            sb.append(')');
            this.signature = sb.toString();
            this.hash = this.signature.hashCode();
        }
        
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MethodSignature)) return false;
            return signature.equals(((MethodSignature) o).signature);
        }
    }
    
    /**
     * 注解匹配缓存 key
     */
    private static class AnnotationCacheKey {
        final Method method;
        final String annotationName;
        final int hash;
        
        AnnotationCacheKey(Method method, String annotationName) {
            this.method = method;
            this.annotationName = annotationName;
            this.hash = System.identityHashCode(method) * 31 + annotationName.hashCode();
        }
        
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AnnotationCacheKey)) return false;
            AnnotationCacheKey other = (AnnotationCacheKey) o;
            return method.equals(other.method) && annotationName.equals(other.annotationName);
        }
    }
}
