package com.lightframework.aop.interceptor;

import com.lightframework.aop.core.MethodInterceptor;
import com.lightframework.aop.core.MethodInvocation;
import com.lightframework.aop.core.JoinPoint;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * 前置通知拦截器
 * 在目标方法执行前运行
 */
// TODO [L3][优化-工厂方法] 五个通知拦截器(Before/After/Around/AfterReturning/AfterThrowing)构造里；写对标志：按工厂方法模式完成实现，新增单测覆盖“按类型/参数创建不同产品”的主路径与一条异常路径，断言返回对象类型与属性正确。
// "unreflect→bindTo→asType 适配"几乎完全重复；可抽取 AdviceMethodHandleAdapter 工厂，按通知类型
// (Before/After/…)策略化生成 MethodHandle，消除每类拦截器的重复构造代码。
public class BeforeMethodInterceptor implements MethodInterceptor {

    private final MethodHandle aspectMethodHandle;
    private final MethodHandle adaptedHandle;
    private final boolean takesJoinPoint;

    public BeforeMethodInterceptor(Method aspectMethod, Object aspectInstance) {
        this.takesJoinPoint = aspectMethod.getParameterCount() > 0;

        // TODO [L1][练习] 在构造中理解 MethodHandle 适配——当前按参数个数决定是否 asType 为 (JoinPoint) 或 ()；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（BeforeMethodInterceptorTest 验证：通知方法无参时
        // 正常执行、带 JoinPoint 参数时收到正确 JoinPoint，且不会因签名不符抛 WrongMethodTypeException）。
        try {
            MethodHandle mh = MethodHandles.lookup().unreflect(aspectMethod);
            this.aspectMethodHandle = mh.bindTo(aspectInstance);
            this.adaptedHandle = takesJoinPoint
                ? this.aspectMethodHandle.asType(MethodType.methodType(Object.class, JoinPoint.class))
                : this.aspectMethodHandle.asType(MethodType.methodType(Object.class));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to create MethodHandle for aspect method: " + aspectMethod, e);
        }
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // TODO [L2][练习] 实现 @Before 的通知执行与链推进——当前 takesJoinPoint 时传 joinPoint 否则无参调用；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        // adaptedHandle，再 invocation.proceed()；请写测试验证 @Before 在目标方法之前执行、且 proceed 后目标返回值不受影响。
        // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（BeforeMethodInterceptorTest 验证 @Before 先于目标执行）。
        if (takesJoinPoint) {
            JoinPoint joinPoint = invocation.getJoinPoint();
            adaptedHandle.invoke(joinPoint);
        } else {
            adaptedHandle.invoke();
        }

        return invocation.proceed();
    }
}
