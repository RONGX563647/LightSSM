package com.lightframework.aop.core;

import java.lang.reflect.Method;

/**
 * ProceedingJoinPoint - 用于 @Around 通知
 * 
 * 修复：持有 MethodInvocation 引用，proceed() 直接调用 invocation.proceed()
 * 这样可以正确传递到拦截器链的下一个拦截器，最终到达目标方法
 * 
 * 不再使用对象池（避免嵌套调用数据损坏），每次创建轻量实例
 */
public class ProceedingJoinPoint {
    
    private final MethodInvocation invocation;
    
    /**
     * 创建 ProceedingJoinPoint，绑定到当前 MethodInvocation
     */
    public ProceedingJoinPoint(MethodInvocation invocation) {
        this.invocation = invocation;
    }
    
    /**
     * 执行拦截器链的下一个拦截器（或目标方法）
     * 等价于 invocation.proceed()
     */
    // TODO [L1][练习] 理解 ProceedingJoinPoint.proceed() 如何委托到 MethodInvocation.proceed()；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（AroundMethodInterceptorTest 验证 @Around 中调用
    // proceed() 一次只执行一次目标、多次调用 proceed() 会重复执行目标）。
    public Object proceed() throws Throwable {
        return invocation.proceed();
    }
    
    /**
     * 使用新参数执行拦截器链的下一个拦截器（或目标方法）
     * 可以修改传递给目标方法的参数
     */
    // TODO [L2][练习] 实现 proceed(Object[] args) 的参数改写——当前 setArgs 后 proceed，请验证改写后的 args；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    // 能真正传到目标方法且 JoinPoint.getArgs() 同步可见。验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿
    // （AroundMethodInterceptorTest 验证 proceed(newArgs) 改写参数后目标收到新参数）。
    public Object proceed(Object[] args) throws Throwable {
        invocation.setArgs(args);
        return invocation.proceed();
    }
    
    // ========== JoinPoint 委托方法 ==========
    
    public Object getTarget() { return invocation.getTarget(); }
    public Method getMethod() { return invocation.getMethod(); }
    public Object[] getArgs() { return invocation.getArgs(); }
    public Object getProxy() { return invocation.getProxy(); }
    
    public String getMethodName() { return invocation.getMethod().getName(); }
    public Class<?> getTargetClass() { return invocation.getTarget().getClass(); }
    
    public String getSignature() {
        return getTargetClass().getName() + '.' + getMethodName();
    }
    
    public String getArgsString() {
        return java.util.Arrays.toString(getArgs());
    }
}
