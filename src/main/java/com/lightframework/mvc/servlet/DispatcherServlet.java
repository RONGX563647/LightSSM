package com.lightframework.mvc.servlet;

import com.lightframework.ioc.context.ApplicationContext;
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
    
    @Override
    public void init() throws ServletException {
        try {
            initApplicationContext();
            initHandlerMappings();
            initHandlerAdapters();
            initViewResolvers();

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

        try {
            mappedHandler = getHandler(processedRequest);
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
        for (HandlerMapping hm : this.handlerMappings) {
            HandlerExecutionChain handler = hm.getHandler(request);
            if (handler != null) {
                return handler;
            }
        }
        return null;
    }

    protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
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

        if (mv == null) {
            if (response.isCommitted()) {
                return;
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
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