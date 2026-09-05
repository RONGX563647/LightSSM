package com.lightframework.mvc.handler;

import com.lightframework.mvc.annotation.*;
import com.lightframework.mvc.convert.Converter;
import com.lightframework.mvc.convert.ConverterRegistry;
import com.lightframework.mvc.convert.StringToBooleanConverter;
import com.lightframework.mvc.convert.StringToDoubleConverter;
import com.lightframework.mvc.convert.StringToIntegerConverter;
import com.lightframework.mvc.convert.StringToLongConverter;
import com.lightframework.mvc.core.HandlerAdapter;
import com.lightframework.mvc.core.ModelAndView;
import com.lightframework.mvc.multipart.MultipartFile;
import com.lightframework.mvc.multipart.MultipartResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RequestMappingHandlerAdapter implements HandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RequestMappingHandlerAdapter.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ConverterRegistry converterRegistry = new ConverterRegistry();

    private final MultipartResolver multipartResolver = new MultipartResolver();

    private final Map<Method, MethodHandle> methodHandleCache = new ConcurrentHashMap<>(256);

    private final Map<Class<?>, ConverterCacheEntry> converterCache = new ConcurrentHashMap<>(64);

    private final Map<Class<?>, ExceptionHandlerRegistry> exceptionHandlerCache = new ConcurrentHashMap<>(32);

    private final Map<Method, ArgMeta[]> argsCache = new ConcurrentHashMap<>(256);

    // TODO [L2][练习] 本类分散维护 methodHandleCache / converterCache / exceptionHandlerCache / argsCache 四套 ConcurrentHashMap；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    //   请抽取一个统一的 CacheManager（或泛型 Cache<K,V>）来集中管理容量、失效与统计，避免重复缓存结构。

    private record ConverterCacheEntry(Converter<String, ?> converter, Class<?> targetType) {};

    enum ArgType { REQUEST, RESPONSE, MULTIPART, PATH_VARIABLE, REQUEST_PARAM, REQUEST_BODY, REQUEST_HEADER, COOKIE_VALUE, DEFAULT }

    record ArgMeta(ArgType type, String name, Class<?> targetType, boolean required, String defaultValue) {
        static ArgMeta forServlet(Class<?> type) {
            return new ArgMeta(ArgType.REQUEST, "", type, false, "");
        }
        static ArgMeta forResponse(Class<?> type) {
            return new ArgMeta(ArgType.RESPONSE, "", type, false, "");
        }
        static ArgMeta forMultipart(String name, Class<?> type) {
            return new ArgMeta(ArgType.MULTIPART, name, type, false, "");
        }
        static ArgMeta forAnnotation(ArgType type, String name, Class<?> targetType, boolean required, String defaultValue) {
            return new ArgMeta(type, name, targetType, required, defaultValue);
        }
        static ArgMeta forDefault(Class<?> type) {
            String name = type.getSimpleName();
            name = name.substring(0, 1).toLowerCase() + name.substring(1);
            return new ArgMeta(ArgType.DEFAULT, name, type, false, "");
        }
    }

    public RequestMappingHandlerAdapter() {
        converterRegistry.addConverter(new StringToIntegerConverter());
        converterRegistry.addConverter(new StringToLongConverter());
        converterRegistry.addConverter(new StringToDoubleConverter());
        converterRegistry.addConverter(new StringToBooleanConverter());
    }

    @Override
    public boolean supports(Object handler) {
        return handler instanceof HandlerMethod;
    }

    @Override
    public ModelAndView handle(HttpServletRequest request, HttpServletResponse response,
        Object handler) throws Exception {
        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // TODO [L3][优化-模板方法] handle() 三步骨架（解析参数 → 调用 → 处理返回值，异常走 handleException）已接近模板方法模式；写对标志：按模板方法模式完成实现，新增单测覆盖“钩子方法被回调、算法骨架固定”的主路径与一条异常路径，断言执行顺序与结果正确。
        //   可把骨架抽象为 abstract 模板，将"参数解析/返回值处理"暴露为可覆写的钩子，使子类（如异步、响应式适配器）复用流程、定制步骤。
        try {
            Object[] args = resolveMethodArguments(handlerMethod, request, response);
            Object returnValue = invokeHandlerMethod(handlerMethod, args);
            return handleReturnValue(returnValue, handlerMethod, request, response);
        } catch (Exception ex) {
            return handleException(ex, handlerMethod, request, response);
        }
    }

    protected ModelAndView handleException(Exception ex, HandlerMethod handlerMethod,
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        // TODO [L3][练习] 当前异常只查找"当前 Controller 内"的 @ExceptionHandler；请补充 @ControllerAdvice 全局异常处理器的发现与优先级：；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        //   先匹配类内处理器，再匹配全局 advice 中更具体的异常类型。验收标准：类内无匹配时，全局 @ExceptionHandler 能兜住对应异常。
        Throwable cause = ex;
        if (cause instanceof java.lang.reflect.InvocationTargetException) {
            cause = ((java.lang.reflect.InvocationTargetException) cause).getTargetException();
        }

        ExceptionHandlerRegistry registry = findExceptionRegistry(handlerMethod);
        if (registry != null) {
            HandlerMethod exceptionHandlerMethod = registry.findHandler(cause);
            if (exceptionHandlerMethod != null) {
                Object bean = exceptionHandlerMethod.getBean();
                Method method = exceptionHandlerMethod.getMethod();

                Object[] args = resolveExceptionMethodArgs(method, cause, request, response);
                try {
                    Object returnValue = invokeReflectiveMethod(bean, method, args);
                    return handleReturnValue(returnValue, new HandlerMethod(bean, method), request, response);
                } catch (Exception e) {
                    throw e;
                } catch (Throwable t) {
                    throw new RuntimeException("Exception handler invocation failed", t);
                }
            }
        }
        throw ex;
    }

    private Object invokeReflectiveMethod(Object bean, Method method, Object[] args) throws Throwable {
        MethodHandle handle = methodHandleCache.get(method);
        if (handle == null) {
            method.setAccessible(true);
            handle = MethodHandles.lookup().unreflect(method);
            methodHandleCache.put(method, handle);
        }
        // 将 bean 与参数展开为完整实参数组后用 invokeWithArguments 调用，
        // 避免 handle.invoke(bean, args) 把 args 整个当作单个参数（导致参数无法正确展开）。
        Object[] fullArgs = new Object[args.length + 1];
        fullArgs[0] = bean;
        System.arraycopy(args, 0, fullArgs, 1, args.length);
        return handle.invokeWithArguments(fullArgs);
    }

    protected Object[] resolveExceptionMethodArgs(Method method, Throwable ex,
        HttpServletRequest request, HttpServletResponse response) {
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Class<?> type = params[i].getType();
            if (HttpServletRequest.class.isAssignableFrom(type)) {
                args[i] = request;
            } else if (HttpServletResponse.class.isAssignableFrom(type)) {
                args[i] = response;
            } else if (Throwable.class.isAssignableFrom(type)) {
                args[i] = ex;
            } else {
                args[i] = null;
            }
        }
        return args;
    }

    protected ExceptionHandlerRegistry findExceptionRegistry(HandlerMethod handlerMethod) {
        Class<?> controllerType = handlerMethod.getBeanType();
        ExceptionHandlerRegistry registry = exceptionHandlerCache.get(controllerType);
        if (registry != null) {
            return registry.isEmpty() ? null : registry;
        }
        registry = new ExceptionHandlerRegistry();
        Object controller = handlerMethod.getBean();
        registry.registerFromController(controller, controllerType);
        exceptionHandlerCache.put(controllerType, registry);
        return registry.isEmpty() ? null : registry;
    }

    private ArgMeta[] resolveArgMetas(Method method) {
        ArgMeta[] cached = argsCache.get(method);
        if (cached != null) return cached;

        Parameter[] parameters = method.getParameters();
        ArgMeta[] metas = new ArgMeta[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();

            if (HttpServletRequest.class.isAssignableFrom(paramType)) {
                metas[i] = ArgMeta.forServlet(paramType);
            } else if (HttpServletResponse.class.isAssignableFrom(paramType)) {
                metas[i] = ArgMeta.forResponse(paramType);
            } else if (MultipartFile.class.isAssignableFrom(paramType)) {
                String name = param.getName();
                metas[i] = ArgMeta.forMultipart(name, paramType);
            } else {
                RequestParam rp = param.getAnnotation(RequestParam.class);
                PathVariable pv = param.getAnnotation(PathVariable.class);
                RequestBody rb = param.getAnnotation(RequestBody.class);
                RequestHeader rh = param.getAnnotation(RequestHeader.class);
                CookieValue cv = param.getAnnotation(CookieValue.class);

                if (rb != null) {
                    metas[i] = ArgMeta.forAnnotation(ArgType.REQUEST_BODY, "", paramType, rb.required(), "");
                } else if (rh != null) {
                    String name = rh.value().isEmpty() ? rh.name() : rh.value();
                    metas[i] = ArgMeta.forAnnotation(ArgType.REQUEST_HEADER, name, paramType, rh.required(), rh.defaultValue());
                } else if (cv != null) {
                    String name = cv.value().isEmpty() ? cv.name() : cv.value();
                    metas[i] = ArgMeta.forAnnotation(ArgType.COOKIE_VALUE, name, paramType, cv.required(), cv.defaultValue());
                } else if (rp != null) {
                    String name = rp.value().isEmpty() ? rp.name() : rp.value();
                    metas[i] = ArgMeta.forAnnotation(ArgType.REQUEST_PARAM, name, paramType, rp.required(), rp.defaultValue());
                } else if (pv != null) {
                    String name = pv.value().isEmpty() ? pv.name() : pv.value();
                    metas[i] = ArgMeta.forAnnotation(ArgType.PATH_VARIABLE, name, paramType, pv.required(), "");
                } else {
                    // TODO [L2][练习] 当前 DEFAULT 分支（无注解参数）只能按参数名从 request 取单个简单类型；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
                    //   请支持 @ModelAttribute / 表单对象绑定：当参数是一个自定义 POJO 时，按字段名批量从 request 参数中取值并实例化该对象。
                    //   验收标准：Controller 方法接收 UserForm user 时，user.getName()/user.getAge() 被正确填充。
                    metas[i] = ArgMeta.forDefault(paramType);
                }
            }
        }
        argsCache.put(method, metas);
        return metas;
    }

    protected Object[] resolveMethodArguments(HandlerMethod handlerMethod,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        Method method = handlerMethod.getMethod();
        ArgMeta[] metas = resolveArgMetas(method);
        Object[] args = new Object[metas.length];

        Map<String, List<MultipartFile>> multipartFiles = null;

        // TODO [L3][优化-策略模式] 当前用 switch(meta.type()) 分发不同参数解析（RequestParam/PathVariable/RequestBody...）；写对标志：按策略模式完成实现，新增单测覆盖“运行时切换不同策略得到不同结果”的主路径与一条未知策略的异常路径。
        //   可抽象出 HandlerMethodArgumentResolver 策略接口（supportsParameter + resolveArgument），用工厂注册并以责任链方式依次匹配，
        //   消除 switch 分支，新增参数注解（如 @SessionAttribute）零改动接入。
        for (int i = 0; i < metas.length; i++) {
            ArgMeta meta = metas[i];
            Class<?> paramType = meta.targetType();

            switch (meta.type()) {
                case REQUEST -> args[i] = request;
                case RESPONSE -> args[i] = response;
                case MULTIPART -> {
                    if (multipartFiles == null && multipartResolver.isMultipart(request)) {
                        multipartFiles = multipartResolver.resolveMultipart(request);
                    }
                    args[i] = resolveMultipartArgument(meta.name(), multipartFiles);
                }
                case REQUEST_BODY -> args[i] = resolveRequestBody(paramType, request);
                case REQUEST_HEADER -> args[i] = resolveRequestHeader(meta.name(), paramType, meta.required(), meta.defaultValue(), request);
                case COOKIE_VALUE -> args[i] = resolveCookieValue(meta.name(), paramType, meta.required(), meta.defaultValue(), request);
                case REQUEST_PARAM -> args[i] = resolveRequestParam(meta.name(), paramType, meta.required(), meta.defaultValue(), request);
                case PATH_VARIABLE -> args[i] = resolvePathVariable(meta.name(), paramType, request);
                case DEFAULT -> args[i] = resolveDefaultArgument(meta.name(), paramType, request);
            }
        }

        return args;
    }

    protected Object resolveMultipartArgument(String paramName, Map<String, List<MultipartFile>> files) {
        if (files == null) return null;
        List<MultipartFile> list = files.get(paramName);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    protected Object resolveRequestBody(Class<?> paramType,
        HttpServletRequest request) throws Exception {
        // TODO [L2][练习] 当前 @RequestBody 只支持 JSON（及 String 原文）；请补充对 application/x-www-form-urlencoded 表单体的解析，；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        //   按 key=value&... 拆分后绑定到一个 Map 或目标对象。验收标准：POST 表单体能被正确解析为方法参数。
        if (paramType.equals(String.class)) {
            StringBuilder sb = new StringBuilder();
            try (java.io.BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            return sb.toString();
        }
        return objectMapper.readValue(request.getInputStream(), paramType);
    }

    protected Object resolveRequestHeader(String headerName, Class<?> paramType,
        boolean required, String defaultValue, HttpServletRequest request) {
        String value = request.getHeader(headerName);
        if (value == null) {
            if (required) {
                throw new IllegalArgumentException("Required header '" + headerName + "' is missing");
            }
            if (!defaultValue.isEmpty()) {
                return convertValue(defaultValue, paramType);
            }
            return null;
        }
        return convertValue(value, paramType);
    }

    protected Object resolveCookieValue(String cookieName, Class<?> paramType,
        boolean required, String defaultValue, HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    return convertValue(cookie.getValue(), paramType);
                }
            }
        }
        if (required) {
            throw new IllegalArgumentException("Required cookie '" + cookieName + "' is missing");
        }
        if (!defaultValue.isEmpty()) {
            return convertValue(defaultValue, paramType);
        }
        return null;
    }

    protected Object resolveRequestParam(String paramName, Class<?> paramType,
        boolean required, String defaultValue, HttpServletRequest request) {
        String value = request.getParameter(paramName);
        if (value == null) {
            if (required) {
                throw new IllegalArgumentException("Required parameter '" + paramName + "' is missing");
            }
            if (!defaultValue.isEmpty()) {
                return convertValue(defaultValue, paramType);
            }
            return null;
        }
        return convertValue(value, paramType);
    }

    protected Object resolvePathVariable(String varName, Class<?> paramType,
        HttpServletRequest request) {
        Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(
            RequestMappingInfo.PATH_VARIABLES_ATTRIBUTE);
        if (pathVariables != null && pathVariables.containsKey(varName)) {
            return convertValue(pathVariables.get(varName), paramType);
        }
        return null;
    }

    protected Object resolveDefaultArgument(String paramName, Class<?> paramType,
        HttpServletRequest request) {
        String value = request.getParameter(paramName);
        if (value == null) {
            return null;
        }
        return convertValue(value, paramType);
    }

    protected Object convertValue(String value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType == String.class) {
            return value;
        }

        // TODO [L1][练习] convertValue 对 int/long/double/boolean 用了多条 if 链；请改为"注册式/查表"转换；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        //   （复用 ConverterRegistry 或 Map<Class<?>, Function<String,?>>），让新增基础类型转换不必修改本方法。
        //   验收标准：行为与现在完全一致，且新增 StringToShortConverter 后无需改这里即可生效。
        try {
            if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(value);
            }
            if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(value);
            }
            if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(value);
            }
            if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(value);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Failed to convert value '" + value + "' to " + targetType.getSimpleName(), e);
        }

        ConverterCacheEntry entry = converterCache.get(targetType);
        if (entry == null) {
            Converter<String, ?> converter = converterRegistry.findConverter(targetType);
            entry = new ConverterCacheEntry(converter, targetType);
            converterCache.put(targetType, entry);
        }

        if (entry.converter() != null) {
            return entry.converter().convert(value);
        }

        return value;
    }

    protected Object invokeHandlerMethod(HandlerMethod handlerMethod, Object[] args)
        throws Exception {
        try {
            return invokeReflectiveMethod(handlerMethod.getBean(), handlerMethod.getMethod(), args);
        } catch (Exception e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Handler method invocation failed", t);
        }
    }

    protected ModelAndView handleReturnValue(Object returnValue, HandlerMethod handlerMethod,
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (returnValue == null) {
            return null;
        }

        Method method = handlerMethod.getMethod();
        ResponseBody responseBody = method.getAnnotation(ResponseBody.class);
        if (responseBody == null) {
            responseBody = handlerMethod.getBeanType().getAnnotation(ResponseBody.class);
        }

        ResponseStatus responseStatus = method.getAnnotation(ResponseStatus.class);
        if (responseStatus == null) {
            responseStatus = handlerMethod.getBeanType().getAnnotation(ResponseStatus.class);
        }

        if (responseBody != null) {
            if (responseStatus != null) {
                response.setStatus(responseStatus.value());
            }
            handleResponseBody(returnValue, response);
            return null;
        }

        // TODO [L2][练习] 当前按 instanceof 顺序处理 ModelAndView / String / Map 返回值；请补充对常见返回类型的处理，；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        //   例如 @ModelAttribute 标注的 POJO（当作 model 属性），以及统一响应包装（如 ApiResult<T> 自动拆壳后写 JSON）。
        //   验收标准：返回自定义包装对象时仍能被 @ResponseBody 正确序列化为 JSON。
        if (returnValue instanceof ModelAndView) {
            return (ModelAndView) returnValue;
        }

        if (returnValue instanceof String) {
            String viewName = (String) returnValue;
            return new ModelAndView(viewName);
        }

        if (returnValue instanceof Map) {
            Map<String, Object> model = (Map<String, Object>) returnValue;
            return new ModelAndView().addAllObjects(model);
        }

        return null;
    }

    protected void handleResponseBody(Object returnValue, HttpServletResponse response)
        throws Exception {
        response.setContentType("application/json;charset=UTF-8");

        String json = objectMapper.writeValueAsString(returnValue);

        PrintWriter writer = response.getWriter();
        writer.write(json);
        writer.flush();

        logger.debug("Written JSON response: {}", json);
    }

    @Override
    public long getLastModified(HttpServletRequest request, Object handler) {
        return -1;
    }

    public ConverterRegistry getConverterRegistry() {
        return converterRegistry;
    }

    public MultipartResolver getMultipartResolver() {
        return multipartResolver;
    }
}