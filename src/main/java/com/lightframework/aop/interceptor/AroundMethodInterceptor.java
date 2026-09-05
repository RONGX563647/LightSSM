package com.lightframework.aop.interceptor;

import com.lightframework.aop.core.MethodInterceptor;
import com.lightframework.aop.core.MethodInvocation;
import com.lightframework.aop.core.ProceedingJoinPoint;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * 环绕通知拦截器
 * 包裹目标方法执行，可控制目标方法是否执行、何时执行、参数修改
 * 
 * 修复：创建 ProceedingJoinPoint 包装当前 MethodInvocation
 * 当切面调用 joinPoint.proceed() 时，正确传递到下一个拦截器
 */
public class AroundMethodInterceptor implements MethodInterceptor {

    private final MethodHandle aspectMethodHandle;
    private final MethodHandle adaptedHandle;

    public AroundMethodInterceptor(Method aspectMethod, Object aspectInstance) {
        try {
            MethodHandle mh = MethodHandles.lookup().unreflect(aspectMethod);
            this.aspectMethodHandle = mh.bindTo(aspectInstance);
            this.adaptedHandle = this.aspectMethodHandle.asType(
                MethodType.methodType(Object.class, ProceedingJoinPoint.class));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to create MethodHandle for aspect method: " + aspectMethod, e);
        }
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // TODO [L2][练习] 实现 @Around 对 ProceedingJoinPoint 的包装——当前 new ProceedingJoinPoint(invocation)；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        // 后调用 adaptedHandle；请写测试验证 @Around 通知里不调用 proceed() 时目标方法被跳过、调用一次则执行一次。
        // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（AroundMethodInterceptorTest 验证 proceed 控制目标执行）。
        // 创建 ProceedingJoinPoint 包装当前 MethodInvocation
        ProceedingJoinPoint joinPoint = new ProceedingJoinPoint(invocation);

        return (Object) adaptedHandle.invoke(joinPoint);
    }
}
