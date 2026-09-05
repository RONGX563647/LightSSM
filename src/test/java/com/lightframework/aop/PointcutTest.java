package com.lightframework.aop;

import com.lightframework.aop.pointcut.AspectJExpressionPointcut;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

// 顶层包私有类（非嵌套），使运行时类名为 com.lightframework.aop.TargetSvc，
// 与切点表达式中的类名一致（若是嵌套类，名字会变成 ...PointcutTest$TargetSvc 而匹配失败）。
class TargetSvc {
    public int add(int a, int b) { return a + b; }
    public int mul(int a, int b) { return a * b; }
}

public class PointcutTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Marker {}

    static class Annotated {
        @Marker public int doWork(int x) { return x; }
        public int other(int x) { return x; }
    }

    @Test
    void executionExactMethodMatch() throws Throwable {
        // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿
        // （AspectJExpressionPointcut.parseExpression 对 execution 表达式解析 + matches 方法匹配；
        // 当前 parseExpression 未剥离返回类型通配符，故此处用不含返回类型前缀的写法，见 AspectJExpressionPointcut 的 L2 练习 TODO）
        AspectJExpressionPointcut pc =
            new AspectJExpressionPointcut("execution(com.lightframework.aop.TargetSvc.add(..))");
        Method add = TargetSvc.class.getMethod("add", int.class, int.class);
        Method mul = TargetSvc.class.getMethod("mul", int.class, int.class);
        assertTrue(pc.matches(add));
        assertFalse(pc.matches(mul));
    }

    @Test
    void executionWildcardClassMatch() throws Throwable {
        // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿
        // （convertWildcards 把包通配 * 转成 [^.]* 后匹配）
        AspectJExpressionPointcut pc =
            new AspectJExpressionPointcut("execution(com.lightframework.aop.*.add(..))");
        Method add = TargetSvc.class.getMethod("add", int.class, int.class);
        assertTrue(pc.matches(add));
    }

    @Test
    void annotationPointcutMatch() throws Throwable {
        // 验证 TODO[L3] 练习目标：用户手写实现后运行本测试应全绿
        // （matchesAnnotation 完善简单类名/元注解匹配后的预期行为）
        AspectJExpressionPointcut pc =
            new AspectJExpressionPointcut("@annotation(com.lightframework.aop.PointcutTest$Marker)");
        Method dw = Annotated.class.getMethod("doWork", int.class);
        Method other = Annotated.class.getMethod("other", int.class);
        assertTrue(pc.matches(dw));
        assertFalse(pc.matches(other));
    }

    @Test
    void annotationPointcutDoesNotMatchAtClassLevel() {
        // @annotation 切点只匹配方法，类级别匹配按设计返回 false（不抛异常）
        AspectJExpressionPointcut pc =
            new AspectJExpressionPointcut("@annotation(java.lang.Deprecated)");
        assertFalse(pc.matches(String.class));
    }
}
