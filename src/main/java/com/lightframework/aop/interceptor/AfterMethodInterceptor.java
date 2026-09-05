package com.lightframework.aop.interceptor;

import com.lightframework.aop.core.MethodInterceptor;
import com.lightframework.aop.core.MethodInvocation;
import com.lightframework.aop.core.JoinPoint;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * 后置通知拦截器
 * 在目标方法执行后运行（无论是否抛出异常）
 */
public class AfterMethodInterceptor implements MethodInterceptor {

    private final MethodHandle adaptedHandle;
    private final boolean takesJoinPoint;

    public AfterMethodInterceptor(Method aspectMethod, Object aspectInstance) {
        this.takesJoinPoint = aspectMethod.getParameterCount() > 0;

        try {
            MethodHandle mh = MethodHandles.lookup().unreflect(aspectMethod);
            MethodHandle aspectMethodHandle = mh.bindTo(aspectInstance);
            this.adaptedHandle = takesJoinPoint
                ? aspectMethodHandle.asType(MethodType.methodType(Object.class, JoinPoint.class))
                : aspectMethodHandle.asType(MethodType.methodType(Object.class));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to create MethodHandle for aspect method: " + aspectMethod, e);
        }
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // TODO [L2][练习] 实现 @After 的 finally 语义——当前 try{ proceed } finally{ 执行 after 通知 }；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        // 请写测试验证: 目标方法抛异常时 @After 通知仍执行、且异常继续向上传播(不被吞掉)。
        // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（AfterMethodInterceptorTest 验证异常时 @After 仍执行）。
        Object result;
        try {
            result = invocation.proceed();
        } finally {
            if (takesJoinPoint) {
                JoinPoint joinPoint = invocation.getJoinPoint();
                adaptedHandle.invoke(joinPoint);
            } else {
                adaptedHandle.invoke();
            }
        }
        return result;
    }
}
