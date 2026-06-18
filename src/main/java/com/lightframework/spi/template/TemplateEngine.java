package com.lightframework.spi.template;

import com.lightframework.spi.Ordered;
import java.util.Map;

public interface TemplateEngine extends Ordered {

    String render(String templateName, Map<String, Object> model);

    String renderContent(String content, Map<String, Object> model);

    @Override
    default int getOrder() { return Ordered.LOWEST_PRECEDENCE; }
}
