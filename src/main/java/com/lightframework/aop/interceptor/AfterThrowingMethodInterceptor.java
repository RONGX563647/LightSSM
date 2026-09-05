package com.lightframework.aop.interceptor;

import com.lightframework.aop.core.MethodInterceptor;
import com.lightframework.aop.core.MethodInvocation;
import com.lightframework.aop.core.JoinPoint;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public class AfterThrowingMethodInterceptor implements MethodInterceptor {

    private final MethodHandle adaptedHandle;

    public AfterThrowingMethodInterceptor(Method aspectMethod, Object aspectInstance) {
        try {
            MethodHandle mh = MethodHandles.lookup().unreflect(aspectMethod);
            mh = mh.bindTo(aspectInstance);
            this.adaptedHandle = mh.asType(MethodType.methodType(Object.class, JoinPoint.class));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to create MethodHandle for aspect method: " + aspectMethod, e);
        }
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // TODO [L2][练习] 实现 @AfterThrowing——当前 catch 中执行通知再抛出原异常；请补全 throwing() 属性把异常对象；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        // 绑定到通知参数，并写测试验证: 目标异常时通知执行且异常类型正确传播。
        // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（AfterThrowingMethodInterceptorTest 验证异常被通知捕获后重抛）。
        try {
            return invocation.proceed();
        } catch (Throwable t) {
            JoinPoint joinPoint = invocation.getJoinPoint();
            adaptedHandle.invoke(joinPoint);
            throw t;
        }
    }
}