package com.lightframework.ioc.test;

import com.lightframework.ioc.annotation.Autowired;
import com.lightframework.ioc.annotation.Component;
import com.lightframework.ioc.annotation.EventListener;
import com.lightframework.ioc.context.AnnotationConfigApplicationContext;
import com.lightframework.ioc.event.ApplicationEvent;
import com.lightframework.ioc.event.ApplicationEventPublisher;
import com.lightframework.ioc.event.ApplicationListener;
import com.lightframework.ioc.event.ContextClosedEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// ========== 自定义事件 ==========

class CustomEvent extends ApplicationEvent {
    private final String message;

    public CustomEvent(Object source, String message) {
        super(source);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

class OrderEvent extends ApplicationEvent {
    private final String orderId;

    public OrderEvent(Object source, String orderId) {
        super(source);
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}

// ========== 使用 ApplicationListener 接口的监听器 ==========

@Component("customEventListener")
class CustomEventListener implements ApplicationListener<CustomEvent> {
    static final List<CustomEvent> receivedEvents = new ArrayList<>();

    @Override
    public void onApplicationEvent(CustomEvent event) {
        receivedEvents.add(event);
    }

    public static void reset() {
        receivedEvents.clear();
    }
}

// 另一个监听同一事件的监听器
@Component("anotherCustomEventListener")
class AnotherCustomEventListener implements ApplicationListener<CustomEvent> {
    static final List<CustomEvent> receivedEvents = new ArrayList<>();

    @Override
    public void onApplicationEvent(CustomEvent event) {
        receivedEvents.add(event);
    }

    public static void reset() {
        receivedEvents.clear();
    }
}

// 监听所有事件的监听器
@Component("globalEventListener")
class GlobalEventListener implements ApplicationListener<ApplicationEvent> {
    static final List<ApplicationEvent> receivedEvents = new ArrayList<>();

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        receivedEvents.add(event);
    }

    public static void reset() {
        receivedEvents.clear();
    }
}

// ========== 使用 @EventListener 注解的监听器 ==========

@Component("annotatedEventListener")
class AnnotatedEventListener {
    static final List<OrderEvent> receivedOrderEvents = new ArrayList<>();
    static final List<ApplicationEvent> receivedAllEvents = new ArrayList<>();

    @EventListener
    public void onOrderEvent(OrderEvent event) {
        receivedOrderEvents.add(event);
    }

    @EventListener(ApplicationEvent.class)
    public void onAnyEvent(ApplicationEvent event) {
        receivedAllEvents.add(event);
    }

    public static void reset() {
        receivedOrderEvents.clear();
        receivedAllEvents.clear();
    }
}

// ========== 发布事件的 Bean ==========

@Component("eventPublisherBean")
class EventPublisherBean implements com.lightframework.ioc.core.BeanFactoryAware {
    private com.lightframework.ioc.core.DefaultListableBeanFactory beanFactory;

    @Override
    public void setBeanFactory(com.lightframework.ioc.core.DefaultListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public void publishCustomEvent(String message) {
        beanFactory.publishEvent(new CustomEvent(this, message));
    }
}

public class EventMechanismTest {

    @Test
    public void testApplicationListenerReceivesEvent() throws Exception {
        CustomEventListener.reset();
        AnotherCustomEventListener.reset();

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                CustomEventListener.class,
                AnotherCustomEventListener.class
        );

        CustomEvent event = new CustomEvent(this, "Hello Event");
        context.publishEvent(event);

        assertEquals(1, CustomEventListener.receivedEvents.size());
        assertEquals("Hello Event", CustomEventListener.receivedEvents.get(0).getMessage());

        assertEquals(1, AnotherCustomEventListener.receivedEvents.size());
        assertEquals("Hello Event", AnotherCustomEventListener.receivedEvents.get(0).getMessage());

        context.close();
    }

    @Test
    public void testAnnotatedEventListenerReceivesEvent() throws Exception {
        AnnotatedEventListener.reset();

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                AnnotatedEventListener.class
        );

        OrderEvent event = new OrderEvent(this, "ORDER-001");
        context.publishEvent(event);

        assertEquals(1, AnnotatedEventListener.receivedOrderEvents.size());
        assertEquals("ORDER-001", AnnotatedEventListener.receivedOrderEvents.get(0).getOrderId());

        assertEquals(1, AnnotatedEventListener.receivedAllEvents.size());

        context.close();
    }

    @Test
    public void testPublishCustomEvent() throws Exception {
        CustomEventListener.reset();

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                CustomEventListener.class
        );

        CustomEvent event = new CustomEvent(this, "Test Custom Event");
        context.publishEvent(event);

        assertEquals(1, CustomEventListener.receivedEvents.size());
        assertEquals("Test Custom Event", CustomEventListener.receivedEvents.get(0).getMessage());
        assertNotNull(CustomEventListener.receivedEvents.get(0).getSource());
        assertTrue(CustomEventListener.receivedEvents.get(0).getTimestamp() > 0);

        context.close();
    }

    @Test
    public void testMultipleListenersForSameEvent() throws Exception {
        CustomEventListener.reset();
        AnotherCustomEventListener.reset();
        GlobalEventListener.reset();

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                CustomEventListener.class,
                AnotherCustomEventListener.class,
                GlobalEventListener.class
        );

        CustomEvent event = new CustomEvent(this, "Multi-Listener Test");
        context.publishEvent(event);

        // All three listeners should receive the event
        assertEquals(1, CustomEventListener.receivedEvents.size());
        assertEquals(1, AnotherCustomEventListener.receivedEvents.size());
        assertEquals(1, GlobalEventListener.receivedEvents.size());

        context.close();
    }

    @Test
    public void testEventPublishingOrder() throws Exception {
        GlobalEventListener.reset();

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                GlobalEventListener.class
        );

        // Publish events in order
        context.publishEvent(new CustomEvent(this, "First"));
        context.publishEvent(new CustomEvent(this, "Second"));
        context.publishEvent(new CustomEvent(this, "Third"));

        assertEquals(3, GlobalEventListener.receivedEvents.size());
        assertEquals("First", ((CustomEvent) GlobalEventListener.receivedEvents.get(0)).getMessage());
        assertEquals("Second", ((CustomEvent) GlobalEventListener.receivedEvents.get(1)).getMessage());
        assertEquals("Third", ((CustomEvent) GlobalEventListener.receivedEvents.get(2)).getMessage());

        context.close();
    }

    @Test
    public void testContextClosedEvent() throws Exception {
        GlobalEventListener.reset();

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                GlobalEventListener.class
        );

        context.close();

        // ContextClosedEvent should have been published
        boolean foundClosedEvent = false;
        for (ApplicationEvent event : GlobalEventListener.receivedEvents) {
            if (event instanceof ContextClosedEvent) {
                foundClosedEvent = true;
                assertEquals(context, event.getSource());
                break;
            }
        }
        assertTrue(foundClosedEvent, "ContextClosedEvent should be published on context close");
    }

    @Test
    public void testBeanPublishesEventViaBeanFactory() throws Exception {
        CustomEventListener.reset();

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                CustomEventListener.class,
                EventPublisherBean.class
        );

        EventPublisherBean publisherBean = context.getBean("eventPublisherBean", EventPublisherBean.class);
        publisherBean.publishCustomEvent("Published via BeanFactory");

        assertEquals(1, CustomEventListener.receivedEvents.size());
        assertEquals("Published via BeanFactory", CustomEventListener.receivedEvents.get(0).getMessage());

        context.close();
    }
}
