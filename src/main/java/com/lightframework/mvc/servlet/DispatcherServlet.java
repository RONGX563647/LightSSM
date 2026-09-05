package com.lightframework.mvc.servlet;

import com.lightframework.ioc.context.ApplicationContext;
import com.lightframework.mvc.core.ContentNegotiationManager;
import com.lightframework.mvc.core.CorsProcessor;
import com.lightframework.mvc.core.HandlerAdapter;
import com.lightframework.mvc.core.HandlerExecutionChain;
import com.lightframework.mvc.core.HandlerMapping;
import com.lightframework.mvc.core.ModelAndView;
import com.lightframework.mvc.handler.RequestMappingHandlerAdapter;
import com.lightframework.mvc.handler.RequestMappingHandlerMapping;
import com.lightframework.mvc.view.View;
import com.lightframework.mvc.view.ViewResolver;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DispatcherServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(DispatcherServlet.class);

    private ApplicationContext applicationContext;

    private List<HandlerMapping> handlerMappings = new ArrayList<>();

    private List<HandlerAdapter> handlerAdapters = new ArrayList<>();

    private List<ViewResolver> viewResolvers = new ArrayList<>();
    
    private CorsProcessor corsProcessor = new CorsProcessor();
    
    private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();
    
    @Override
    public void init() throws ServletException {
        try {
            initApplicationContext();
            initHandlerMappings();
            initHandlerAdapters();
            initViewResolvers();
            initCors();
            initContentNegotiation();

            logger.info("DispatcherServlet initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize DispatcherServlet", e);
            throw new ServletException("Failed to initialize DispatcherServlet", e);
        }
    }

    protected void initApplicationContext() throws Exception {
        String contextConfigLocation = getServletConfig().getInitParameter("contextConfigLocation");
        if (contextConfigLocation == null) {
            contextConfigLocation = "com.lightframework";
        }

        this.applicationContext = new com.lightframework.ioc.context.AnnotationConfigApplicationContext(
                contextConfigLocation.split(","));
    }

    protected void initHandlerMappings() throws Exception {
        RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping(
                this.applicationContext);
        handlerMapping.initHandlerMethods();
        this.handlerMappings.add(handlerMapping);

        logger.info("Initialized {} handler mappings", this.handlerMappings.size());
    }

    protected void initHandlerAdapters() throws Exception {
        this.handlerAdapters.add(new RequestMappingHandlerAdapter());

        logger.info("Initialized {} handler adapters", this.handlerAdapters.size());
    }

    protected void initViewResolvers() throws Exception {
        String[] viewResolverNames = this.applicationContext.getBeanNamesForType(ViewResolver.class);
        for (String name : viewResolverNames) {
            try {
                ViewResolver viewResolver = this.applicationContext.getBean(name, ViewResolver.class);
                this.viewResolvers.add(viewResolver);
            } catch (Exception e) {
                logger.warn("Could not load view resolver: {}", name, e);
            }
        }

        if (this.viewResolvers.isEmpty()) {
            this.viewResolvers.add(new com.lightframework.mvc.view.InternalResourceViewResolver());
        }

        logger.info("Initialized {} view resolvers", this.viewResolvers.size());
    }
    
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            doDispatch(request, response);
        } catch (Exception e) {
            logger.error("Error dispatching request", e);
            handleError(request, response, e);
        }
    }

    protected void doDispatch(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        HttpServletRequest processedRequest = request;
        HandlerExecutionChain mappedHandler = null;

        // TODO [L3][练习] 在请求进入时把当前 request 绑定到 Web 作用域（例如调用 RequestScope.setCurrentRequest(request)），；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        //   并在 finally 块中清理（RequestScope.clear()），使 @RequestScope / @SessionScope Bean 在 Controller 中能正常获取。
        //   验收标准：请求处理期间可取到绑定的 request；处理结束后 ThreadLocal 被清空、跨请求不串号、无内存泄漏。
        // TODO [L3][优化-工厂方法] request 生命周期目前由 DispatcherServlet 直接管理；可抽象出 WebScope 接口（RequestScope/SessionScope），；写对标志：按工厂方法模式完成实现，新增单测覆盖“按类型/参数创建不同产品”的主路径与一条异常路径，断言返回对象类型与属性正确。
        //   用工厂方法按作用域类型创建 Scope 实例，并在请求边界统一触发其 create/destroy 回调，解耦 Servlet 与具体作用域实现。

        try {
            mappedHandler = getHandler(processedRequest);

            if (!corsProcessor.processRequest(request, response,
                    mappedHandler != null ? mappedHandler.getHandler() : null)) {
                return;
            }
            if (mappedHandler == null) {
                noHandlerFound(processedRequest, response);
                return;
            }

            HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
            if (ha == null) {
                throw new ServletException("No adapter for handler [" + mappedHandler.getHandler() + "]");
            }

            if (!mappedHandler.applyPreHandle(processedRequest, response)) {
                return;
            }

            ModelAndView mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

            mappedHandler.applyPostHandle(processedRequest, response, mv);

            processDispatchResult(processedRequest, response, mappedHandler, mv);

        } catch (Exception ex) {
            triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
            throw ex;
        }
    }

    protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        // TODO [L1][练习] 把"遍历 handlerMappings 取第一个非空 HandlerExecutionChain"的查找逻辑，；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        //   抽取为一个清晰的 lookupHandler(request) 辅助方法（可用 Stream 或提前返回），提升可读性。
        //   验收标准：行为与现在一致——第一个能匹配请求的 HandlerMapping 胜出。
        for (HandlerMapping hm : this.handlerMappings) {
            HandlerExecutionChain handler = hm.getHandler(request);
            if (handler != null) {
                return handler;
            }
        }
        return null;
    }

    protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
        // TODO [L2][优化-适配器] HandlerAdapter 已用适配器模式（HandlerAdapter 接口 + supports/handle）。；写对标志：按适配器模式完成实现，新增单测覆盖“被适配接口调用被转发到目标接口”的主路径，断言结果一致且异常被正确转换。
        //   当前选择逻辑是 for 循环逐个 supports()；可额外引入"适配器注册表/工厂"：按 handler 类型预注册并排序，
        //   使新增适配器（如 SimpleControllerHandlerAdapter、HttpRequestHandlerAdapter）零改动接入 DispatcherServlet。
        for (HandlerAdapter ha : this.handlerAdapters) {
            if (ha.supports(handler)) {
                return ha;
            }
        }
        return null;
    }

    protected void noHandlerFound(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendError(HttpServletResponse.SC_NOT_FOUND,
                "No handler found for " + request.getRequestURI());
    }

    protected void processDispatchResult(HttpServletRequest request, HttpServletResponse response,
            HandlerExecutionChain mappedHandler, ModelAndView mv) throws Exception {

        // TODO [L2][练习] 当前 mv==null 直接 sendError(404)；补充对 ModelAndView.isReference() 的判断，；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        //   当视图名以 "redirect:" / "forward:" 开头时走重定向或内部转发逻辑，而不是直接 404。
        //   验收标准：Controller 返回 redirect:/login 时能正确触发重定向而非 404。
        if (mv == null) {
            if (response.isCommitted()) {
                triggerAfterCompletion(request, response, mappedHandler, null);
                return;
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            triggerAfterCompletion(request, response, mappedHandler, null);
            return;
        }

        render(mv, request, response);

        triggerAfterCompletion(request, response, mappedHandler, null);
    }

    protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        View view;
        String viewName = mv.getViewName();

        if (viewName != null) {
            view = resolveViewName(viewName, mv.getModel(), request);
            if (view == null) {
                throw new ServletException("Could not resolve view with name '" + viewName + "'");
            }
        } else {
            throw new ServletException("No view name provided");
        }

        view.render(mv.getModel(), request, response);
    }

    protected View resolveViewName(String viewName, Map<String, Object> model,
            HttpServletRequest request) throws Exception {

        // TODO [L1][练习] 把"遍历 viewResolvers 取第一个非空 View"的查找逻辑抽取为 lookupViewResolver(viewName) 辅助方法，；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        //   并明确"无 resolver 命中时返回 null"的契约。验收标准：与现有行为一致，第一个能解析的 resolver 胜出。
        for (ViewResolver viewResolver : this.viewResolvers) {
            View view = viewResolver.resolveViewName(viewName);
            if (view != null) {
                return view;
            }
        }
        return null;
    }

    protected void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response,
            HandlerExecutionChain mappedHandler, Exception ex) throws Exception {

        if (mappedHandler != null) {
            mappedHandler.triggerAfterCompletion(request, response, ex);
        }
    }

    protected void initCors() {
        logger.info("CORS processor initialized");
    }
    
    protected void initContentNegotiation() {
        logger.info("ContentNegotiationManager initialized");
    }
    
    protected void handleError(HttpServletRequest request, HttpServletResponse response,
            Exception ex) throws IOException {

        logger.error("Error handling request: {}", request.getRequestURI(), ex);

        if (!response.isCommitted()) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal server error: " + ex.getMessage());
        }
    }

    @Override
    public void destroy() {
        if (this.applicationContext != null) {
            this.applicationContext.close();
        }
        logger.info("DispatcherServlet destroyed");
    }

    public ApplicationContext getApplicationContext() {
        return this.applicationContext;
    }
}