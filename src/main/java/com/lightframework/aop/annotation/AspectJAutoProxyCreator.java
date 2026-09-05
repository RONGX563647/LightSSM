package com.lightframework.aop.annotation;

import com.lightframework.aop.annotation.After;
import com.lightframework.aop.annotation.AfterReturning;
import com.lightframework.aop.annotation.AfterThrowing;
import com.lightframework.aop.annotation.Around;
import com.lightframework.aop.annotation.Aspect;
import com.lightframework.aop.annotation.Before;
import com.lightframework.aop.annotation.Pointcut;
import com.lightframework.aop.core.ProxyFactory;
import com.lightframework.aop.interceptor.AfterMethodInterceptor;
import com.lightframework.aop.interceptor.AfterReturningMethodInterceptor;
import com.lightframework.aop.interceptor.AfterThrowingMethodInterceptor;
import com.lightframework.aop.interceptor.AroundMethodInterceptor;
import com.lightframework.aop.interceptor.BeforeMethodInterceptor;
import com.lightframework.aop.core.MethodInterceptor;
import com.lightframework.aop.pointcut.AspectJExpressionPointcut;
import com.lightframework.di.annotation.Component;
import com.lightframework.di.annotation.Order;
import com.lightframework.ioc.core.BeanPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AspectJ 自动代理创建器
 * 
 * 修复：
 * 1. 拦截器排序：Before → Around → After（修复之前 After 在 Around 之前执行的错误）
 * 2. 通知执行顺序：Before → Around.start → Target → Around.end → After
 * 3. parseAspect 使用 getDeclaredMethods() 避免继承方法
 * 4. 实现 @Order 排序支持
 */
@Component
public class AspectJAutoProxyCreator implements BeanPostProcessor {

    // TODO [L3][优化-观察者模式] AspectJAutoProxyCreator 实现 BeanPostProcessor，在 IOC 容器；写对标志：按观察者模式完成实现，新增单测覆盖“主题状态变更后所有观察者被通知”的主路径与一条注销后不再收到通知的路径。
    // "Bean 提前引用(getEarlyBeanReference)"与"初始化后(postProcessAfterInitialization)"两个生命周期钩子上织入代理，
    // 本质是观察者/监听器模式监听容器事件。可抽取 AopProxyCreator 监听接口，让事务/异步等多个 Creator 统一监听
    // Bean 生命周期事件，而不是各自实现 BeanPostProcessor 重复注册。

    private static final Logger logger = LoggerFactory.getLogger(AspectJAutoProxyCreator.class);

    // 通知类型排序优先级（越小越先执行）
    // 拦截器链顺序: Before → After → Around
    // 实际执行: Before → Around.start → target → Around.end → After
    private static final int ORDER_BEFORE = 0;
    private static final int ORDER_AFTER = 10;
    private static final int ORDER_AFTER_RETURNING = 11;
    private static final int ORDER_AFTER_THROWING = 12;
    private static final int ORDER_AROUND = 20;

    private final Map<String, AspectInfo> aspectInfos = new LinkedHashMap<>();
    private final java.util.Set<String> proxyBeanNames = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<Class<?>, Integer> classMatchCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public Object getEarlyBeanReference(Object bean, String beanName) {
        try {
            return wrapIfNeeded(bean, beanName);
        } catch (Exception e) {
            logger.warn("Failed to create early AOP proxy for bean: {}", beanName, e);
            return bean;
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws Exception {
        if (proxyBeanNames.contains(beanName)) {
            return bean;
        }
        return wrapIfNeeded(bean, beanName);
    }

    // TODO [L1][缺陷] 惰性解析导致"切面须先于目标创建"：若目标 Bean 在 @Aspect Bean 之前实例化，；写对标志：先写一个“修复前失败、修复后通过”的复现单测；在注释记录根因、触发条件与边界（如并发/空值/创建顺序），CI 全绿。
    // 此时 aspectInfos 仍为空，wrapIfNeeded 第 81 行直接返回原始 Bean 不再织入（见 docs/ARCHITECTURE.md P1）。
    // 建议：在 preInstantiateSingletons 之前主动扫描并解析全部 @Aspect Bean，消除此顺序依赖。

    private Object wrapIfNeeded(Object bean, String beanName) throws Exception {
        if (aspectInfos.isEmpty() && !isAspect(bean)) {
            return bean;
        }

        if (isAspect(bean)) {
            parseAspect(bean, beanName);
            return bean;
        }

        int classMatchMask = computeClassMatchMask(bean.getClass());
        if (classMatchMask == 0) {
            return bean;
        }

        Map<Method, List<MethodInterceptor>> methodInterceptorMap = buildInterceptorMap(bean, classMatchMask);

        if (methodInterceptorMap.isEmpty()) {
            return bean;
        }

        ProxyFactory proxyFactory = new ProxyFactory(bean);
        for (Map.Entry<Method, List<MethodInterceptor>> entry : methodInterceptorMap.entrySet()) {
            proxyFactory.addInterceptors(entry.getKey(), entry.getValue());
        }

        proxyBeanNames.add(beanName);
        logger.info("Created AOP proxy for bean: {} with {} methods", beanName, methodInterceptorMap.size());
        return proxyFactory.getProxy();
    }

    // TODO [L2][练习] computeClassMatchMask 当前用 int 位掩码(bitIndex<31)只支持最多 31 个 advice，；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    // 超出则静默丢弃(位左移溢出)。请改用 boolean[] 或 java.util.BitSet 解除上限，并补全 aspectInfos 变化时
    // (新增切面) classMatchCache 的失效逻辑，避免旧掩码命中错误类。
    private int computeClassMatchMask(Class<?> beanClass) {
        Integer cached = classMatchCache.get(beanClass);
        if (cached != null) {
            return cached;
        }

        int mask = 0;
        int bitIndex = 0;

        for (Map.Entry<String, AspectInfo> entry : aspectInfos.entrySet()) {
            AspectInfo aspectInfo = entry.getValue();
            for (AdviceInfo advice : aspectInfo.getAdvices()) {
                if (advice.pointcut.matches(beanClass)) {
                    if (bitIndex < 31) {
                        mask |= (1 << bitIndex);
                    }
                }
                bitIndex++;
            }
        }

        classMatchCache.put(beanClass, mask);
        return mask;
    }

    // TODO [L3][练习] 修复拦截器链排序 bug，使 @Before/@After/@Around 按 Spring 语义正确织入；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    // （@Around 最外层，@Before 在目标前，@After 在目标后），并支持 @Order 跨切面排序。
    // 验证点：① buildInterceptorMap 当前用 getOrder() 仅按拦截器"类名前缀"排序，且把 @Around 排到最后
    // （ORDER_AROUND=20），导致 @Around 跑到最内层而非最外层；② 同一方法上 @After(ORDER_AFTER=10) 排在
    // @AfterReturning(11)/@AfterThrowing(12) 之前，违反 Spring 中 @After 应最后(innermost finally)执行的语义；
    // ③ 跨切面 @Order 完全没生效——AspectInfo.aspectOrder 已解析却从未参与排序。
    // 练习目标：重写排序，使链顺序为 [Around(外层) ... Before ... 目标 ... AfterReturning/AfterThrowing ... After(内层)]，
    // 并把 aspectOrder 作为第一排序键（同切面内再按通知类型），用 OrderComparator 统一比较。
    private Map<Method, List<MethodInterceptor>> buildInterceptorMap(Object bean, int classMatchMask) {
        Map<Method, List<MethodInterceptor>> methodInterceptorMap = new LinkedHashMap<>();
        List<Method> targetMethods = collectTargetMethods(bean.getClass());

        for (Method beanMethod : targetMethods) {
            List<MethodInterceptor> methodInterceptors = null;

            int bitIndex = 0;
            for (Map.Entry<String, AspectInfo> entry : aspectInfos.entrySet()) {
                AspectInfo aspectInfo = entry.getValue();
                for (AdviceInfo advice : aspectInfo.getAdvices()) {
                    if (bitIndex < 31 && (classMatchMask & (1 << bitIndex)) == 0) {
                        bitIndex++;
                        continue;
                    }

                    if (advice.pointcut.matches(beanMethod)) {
                        if (methodInterceptors == null) {
                            methodInterceptors = new ArrayList<>();
                        }
                        methodInterceptors.add(advice.interceptor);
                    }
                    bitIndex++;
                }
            }

            if (methodInterceptors != null && !methodInterceptors.isEmpty()) {
                // 修复：按通知类型排序 Before → Around → After
                methodInterceptors.sort((a, b) -> Integer.compare(getOrder(a), getOrder(b)));
                methodInterceptorMap.put(beanMethod, methodInterceptors);
            }
        }

        return methodInterceptorMap;
    }

    /** 获取拦截器排序优先级 */
    // TODO [L3][优化-责任链] 拦截器链本身就是责任链；当前 getOrder() 只按拦截器类名前缀；写对标志：按责任链模式完成实现，新增单测覆盖“链上节点处理/传递/终止”的主路径与一条全链放行的路径。
    // (Before/After/Around…) 排序，且完全忽略 aspectOrder 字段，导致 @Order 跨切面未生效。
    // 建议引入 OrderComparator/Advisor 排序器，把"切面顺序 aspectOrder"与"通知类型顺序"组合为统一排序键
    // （先按切面 @Order，再按通知类型），并消除此处对类名的字符串前缀判断（可改为每个拦截器携带 order 元数据）。
    private int getOrder(MethodInterceptor interceptor) {
        String name = interceptor.getClass().getSimpleName();
        if (name.startsWith("Before")) return ORDER_BEFORE;
        if (name.startsWith("AfterReturning")) return ORDER_AFTER_RETURNING;
        if (name.startsWith("AfterThrowing")) return ORDER_AFTER_THROWING;
        if (name.startsWith("After")) return ORDER_AFTER;
        if (name.startsWith("Around")) return ORDER_AROUND;
        return Integer.MAX_VALUE;
    }

    private List<Method> collectTargetMethods(Class<?> clazz) {
        Map<String, Method> methodMap = new LinkedHashMap<>();

        for (Class<?> iface : clazz.getInterfaces()) {
            for (Method m : iface.getMethods()) {
                methodMap.putIfAbsent(methodKey(m), m);
            }
        }

        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Method m : current.getDeclaredMethods()) {
                methodMap.putIfAbsent(methodKey(m), m);
            }
            current = current.getSuperclass();
        }

        return new ArrayList<>(methodMap.values());
    }

    private String methodKey(Method method) {
        StringBuilder sb = new StringBuilder(method.getName());
        sb.append('(');
        for (Class<?> param : method.getParameterTypes()) {
            sb.append(param.getName()).append(',');
        }
        sb.append(')');
        return sb.toString();
    }

    private boolean isAspect(Object bean) {
        return bean.getClass().isAnnotationPresent(Aspect.class);
    }

    // TODO [L2][练习] 在 parseAspect 中把同一切面类里的 @Before/@After/@Around/@AfterReturning/@AfterThrowing；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    // 正确归类并各自构建对应拦截器(BeforeMethodInterceptor 等)，并附带正确 order 值；注意处理一个方法上多个注解共存、
    // 以及命名切点引用解析失败(resolvePointcut 返回 null)时的跳过逻辑。当前实现已逐个 if 判断注解，请补全
    // "同一切面内 advice 顺序"与"重复 advice"的去重/校验。
    private void parseAspect(Object aspectInstance, String beanName) throws Exception {
        Class<?> aspectClass = aspectInstance.getClass();
        AspectInfo aspectInfo = new AspectInfo(beanName);

        Order orderAnn = aspectClass.getAnnotation(Order.class);
        if (orderAnn != null) {
            aspectInfo.aspectOrder = orderAnn.value();
        }

        Map<String, AspectJExpressionPointcut> pointcutMap = new LinkedHashMap<>();
        Method[] declaredMethods = aspectClass.getDeclaredMethods();
        for (Method method : declaredMethods) {
            Pointcut pointcutAnn = method.getAnnotation(Pointcut.class);
            if (pointcutAnn != null) {
                pointcutMap.put(method.getName(), new AspectJExpressionPointcut(pointcutAnn.value()));
            }
        }

        for (Method method : declaredMethods) {
            Before before = method.getAnnotation(Before.class);
            After after = method.getAnnotation(After.class);
            Around around = method.getAnnotation(Around.class);
            AfterReturning afterReturning = method.getAnnotation(AfterReturning.class);
            AfterThrowing afterThrowing = method.getAnnotation(AfterThrowing.class);

            if (before != null) {
                AspectJExpressionPointcut pointcut = resolvePointcut(before.value(), pointcutMap);
                if (pointcut != null) {
                    aspectInfo.addAdvice(new AdviceInfo(pointcut, new BeforeMethodInterceptor(method, aspectInstance), ORDER_BEFORE));
                }
            }
            if (around != null) {
                AspectJExpressionPointcut pointcut = resolvePointcut(around.value(), pointcutMap);
                if (pointcut != null) {
                    aspectInfo.addAdvice(new AdviceInfo(pointcut, new AroundMethodInterceptor(method, aspectInstance), ORDER_AROUND));
                }
            }
            if (afterReturning != null) {
                AspectJExpressionPointcut pointcut = resolvePointcut(afterReturning.value(), pointcutMap);
                if (pointcut != null) {
                    aspectInfo.addAdvice(new AdviceInfo(pointcut, new AfterReturningMethodInterceptor(method, aspectInstance), ORDER_AFTER_RETURNING));
                }
            }
            if (afterThrowing != null) {
                AspectJExpressionPointcut pointcut = resolvePointcut(afterThrowing.value(), pointcutMap);
                if (pointcut != null) {
                    aspectInfo.addAdvice(new AdviceInfo(pointcut, new AfterThrowingMethodInterceptor(method, aspectInstance), ORDER_AFTER_THROWING));
                }
            }
            if (after != null) {
                AspectJExpressionPointcut pointcut = resolvePointcut(after.value(), pointcutMap);
                if (pointcut != null) {
                    aspectInfo.addAdvice(new AdviceInfo(pointcut, new AfterMethodInterceptor(method, aspectInstance), ORDER_AFTER));
                }
            }
        }

        aspectInfo.sortAdvices();
        aspectInfos.put(beanName, aspectInfo);
        logger.info("Parsed @Aspect: {} with {} advices", beanName, aspectInfo.getAdvices().size());
    }

    // TODO [L1][练习] resolvePointcut 当 value 是命名切点(如 "servicePointcut")时需从 pointcutMap 取出；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    // AspectJExpressionPointcut；当前已修复"尾部括号"问题（"pointcutName()" 与 "pointcutName" 统一按方法名查表），
    // 但对无法解析的命名引用仍静默返回 null(上层 if(pointcut!=null)跳过)。可进一步为无法解析的命名引用补一个清晰异常
    // (如 IllegalArgumentException)，并写测试验证 execution(...) 内联写法与命名引用写法都能正确匹配同一方法。
    private AspectJExpressionPointcut resolvePointcut(String value, Map<String, AspectJExpressionPointcut> pointcutMap) {
        if (value.startsWith("execution(") || value.startsWith("@annotation(")) {
            return new AspectJExpressionPointcut(value);
        }
        // 命名切点引用：支持 "pointcutName" 与 "pointcutName()" 两种写法，统一去掉尾部括号再查表
        String key = value.endsWith("()") ? value.substring(0, value.length() - 2) : value;
        return pointcutMap.get(key);
    }

    private static class AspectInfo {
        private final String beanName;
        private final List<AdviceInfo> advices = new ArrayList<>();
        int aspectOrder = 0;

        AspectInfo(String beanName) {
            this.beanName = beanName;
        }

        void addAdvice(AdviceInfo advice) {
            this.advices.add(advice);
        }

        void sortAdvices() {
            advices.sort((a, b) -> Integer.compare(a.order, b.order));
        }

        List<AdviceInfo> getAdvices() {
            return advices;
        }
    }

    private static class AdviceInfo {
        final AspectJExpressionPointcut pointcut;
        final MethodInterceptor interceptor;
        final int order;

        AdviceInfo(AspectJExpressionPointcut pointcut, MethodInterceptor interceptor, int order) {
            this.pointcut = pointcut;
            this.interceptor = interceptor;
            this.order = order;
        }
    }
}
