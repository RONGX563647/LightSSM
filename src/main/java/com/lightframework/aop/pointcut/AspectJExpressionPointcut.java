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

    // TODO [L2][练习] 实现 parseExpression 对 execution 表达式的完整解析——当前按"返回类型 包.类.方法(参数)"拆解并转；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    // Pattern；请补全对修饰符(public/private)、异常声明 throws 的解析，并写测试验证
    // "execution(public * com.x.*.service.*(..))" 类表达式能匹配。验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿
    // （PointcutTest 验证 execution 修饰符/包通配匹配）。
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
                // 关键修复：classPattern 必须只匹配"类名"，不能把方法名带进去。
                // 旧实现把整个 "类.方法" 当作 classPattern，导致 matches(Class) 永远匹配不上
                // 真实类名（真实类名没有 .方法 后缀），于是 AspectJAutoProxyCreator.computeClassMatchMask
                // 恒为 0，切面在真实容器内永远不织入。这里按最后一个 '.' 拆分出类部分与方法名。
                int lastDot = methodPattern.lastIndexOf('.');
                String classPart = lastDot > 0 ? methodPattern.substring(0, lastDot) : methodPattern;
                this.classPattern = Pattern.compile(convertWildcards(classPart));
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

    // TODO [L2][练习] 实现 convertParams 对参数通配符的处理——当前支持 * 与 ..；请补全泛型参数、数组参数(int[])、；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    // 以及 "execution(* *(String, ..))" 中 .. 在中间位置的匹配。验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿
    // （PointcutTest 验证 "*(String, ..)" 与数组参数匹配）。
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
        // TODO [L3][优化-策略模式] matches(Class)/matches(Method)/matches(Class,Method) 三态匹配 + execution/@annotation；写对标志：按策略模式完成实现，新增单测覆盖“运行时切换不同策略得到不同结果”的主路径与一条未知策略的异常路径。
        // 两套解析逻辑混在 if-else 中；可把"表达式类型"抽象为 PointcutMatcher 策略，由 PointcutParser 工厂按前缀
        // (execution/@annotation/其他)创建对应 Matcher，消除分支、便于扩展如 @within/@args 等新切点。
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
        // TODO [L3][练习] 完善 @annotation 切点匹配——当前只按注解全限定名字符串逐一比较方法上的注解；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        // 请支持按注解简单类名匹配、按元注解(被 @X 标记的注解)匹配，并补全是 annotationCache 在切面重新 parse 时的失效逻辑。
        // 验证 TODO[L3] 练习目标：用户手写实现后运行本测试应全绿（PointcutTest 验证 @annotation 简单类名/元注解匹配）。
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
