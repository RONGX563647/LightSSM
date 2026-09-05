package com.lightframework.aop.interceptor;

import com.lightframework.aop.core.MethodInterceptor;
import com.lightframework.aop.core.MethodInvocation;
import com.lightframework.aop.core.JoinPoint;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public class AfterReturningMethodInterceptor implements MethodInterceptor {

    private final MethodHandle adaptedHandle;

    public AfterReturningMethodInterceptor(Method aspectMethod, Object aspectInstance) {
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
        // TODO [L2][练习] 实现 @AfterReturning——当前 proceed() 取返回值后执行通知(仅传 JoinPoint)；请补全；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        // returning() 属性把目标返回值回传到通知方法参数(当前 adaptedHandle 只传 JoinPoint)，并写测试验证通知能拿到返回值。
        // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（AfterReturningMethodInterceptorTest 验证通知拿到返回值）。
        Object result = invocation.proceed();
        JoinPoint joinPoint = invocation.getJoinPoint();
        adaptedHandle.invoke(joinPoint);
        return result;
    }
}