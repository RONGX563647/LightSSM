package com.lightframework.ioc.test;

import com.lightframework.ioc.annotation.Autowired;
import com.lightframework.ioc.annotation.Component;
import com.lightframework.ioc.annotation.Primary;
import com.lightframework.ioc.annotation.Qualifier;
import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.context.AnnotationConfigApplicationContext;
import com.lightframework.ioc.core.DefaultListableBeanFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// ==================== @Qualifier 精确注入测试类 ====================

interface PaymentService {
    String pay();
}

@Component("alipayService")
@Qualifier("alipay")
class AlipayService implements PaymentService {
    @Override
    public String pay() {
        return "alipay";
    }
}

@Component("wechatPayService")
@Qualifier("wechatpay")
class WechatPayService implements PaymentService {
    @Override
    public String pay() {
        return "wechatpay";
    }
}

@Component
class QualifierFieldConsumer {
    @Autowired
    @Qualifier("alipay")
    private PaymentService paymentService;

    public PaymentService getPaymentService() {
        return paymentService;
    }
}

@Component
class QualifierWechatConsumer {
    @Autowired
    @Qualifier("wechatpay")
    private PaymentService paymentService;

    public PaymentService getPaymentService() {
        return paymentService;
    }
}

// ==================== @Qualifier 与 @Primary 优先级测试类 ====================

interface NotificationService {
    String sendNotification();
}

@Component("emailService")
@Qualifier("email")
@Primary
class EmailService implements NotificationService {
    @Override
    public String sendNotification() {
        return "email";
    }
}

@Component("smsService")
@Qualifier("sms")
class SmsService implements NotificationService {
    @Override
    public String sendNotification() {
        return "sms";
    }
}

@Component
class PrimaryAndQualifierConsumer {
    @Autowired
    @Qualifier("sms")
    private NotificationService notificationService;

    public NotificationService getNotificationService() {
        return notificationService;
    }
}

@Component
class PrimaryOnlyConsumer {
    @Autowired
    private NotificationService notificationService;

    public NotificationService getNotificationService() {
        return notificationService;
    }
}

// ==================== @Qualifier 构造器参数测试类 ====================

@Component
class ConstructorQualifierConsumer {
    private final PaymentService paymentService;

    @Autowired
    public ConstructorQualifierConsumer(@Qualifier("wechatpay") PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public PaymentService getPaymentService() {
        return paymentService;
    }
}

// ==================== @Qualifier BeanDefinition 测试 ====================

@Component
@Qualifier("testQualifier")
class TestQualifierBean {
    public String getId() {
        return "testQualifier";
    }
}

// ==================== 测试用例 ====================

public class QualifierTest {

    @Test
    public void testQualifierFieldInjection() throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
            AlipayService.class, WechatPayService.class,
            QualifierFieldConsumer.class, QualifierWechatConsumer.class);

        // 验证 @Qualifier("alipay") 注入的是 AlipayService
        QualifierFieldConsumer alipayConsumer = ctx.getBean(QualifierFieldConsumer.class);
        assertNotNull(alipayConsumer.getPaymentService());
        assertEquals("alipay", alipayConsumer.getPaymentService().pay());

        // 验证 @Qualifier("wechatpay") 注入的是 WechatPayService
        QualifierWechatConsumer wechatConsumer = ctx.getBean(QualifierWechatConsumer.class);
        assertNotNull(wechatConsumer.getPaymentService());
        assertEquals("wechatpay", wechatConsumer.getPaymentService().pay());

        ctx.close();
    }

    @Test
    public void testQualifierOverridesPrimary() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        BeanDefinition bd1 = new BeanDefinition("emailService", EmailService.class);
        bd1.setPrimary(true);
        bd1.setQualifier("email");
        factory.registerBeanDefinition("emailService", bd1);

        BeanDefinition bd2 = new BeanDefinition("smsService", SmsService.class);
        bd2.setQualifier("sms");
        factory.registerBeanDefinition("smsService", bd2);

        BeanDefinition bd3 = new BeanDefinition("primaryAndQualifierConsumer", PrimaryAndQualifierConsumer.class);
        factory.registerBeanDefinition("primaryAndQualifierConsumer", bd3);

        PrimaryAndQualifierConsumer consumer = factory.getBean("primaryAndQualifierConsumer", PrimaryAndQualifierConsumer.class);

        // @Qualifier 应该覆盖 @Primary
        assertNotNull(consumer.getNotificationService());
        assertEquals("sms", consumer.getNotificationService().sendNotification());

        // 验证没有 @Qualifier 时使用 @Primary
        BeanDefinition bd4 = new BeanDefinition("primaryOnlyConsumer", PrimaryOnlyConsumer.class);
        factory.registerBeanDefinition("primaryOnlyConsumer", bd4);

        PrimaryOnlyConsumer primaryConsumer = factory.getBean("primaryOnlyConsumer", PrimaryOnlyConsumer.class);
        assertEquals("email", primaryConsumer.getNotificationService().sendNotification());
    }

    @Test
    public void testQualifierConstructorParameter() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        BeanDefinition bd1 = new BeanDefinition("alipayService", AlipayService.class);
        bd1.setQualifier("alipay");
        factory.registerBeanDefinition("alipayService", bd1);

        BeanDefinition bd2 = new BeanDefinition("wechatPayService", WechatPayService.class);
        bd2.setQualifier("wechatpay");
        factory.registerBeanDefinition("wechatPayService", bd2);

        BeanDefinition bd3 = new BeanDefinition("constructorConsumer", ConstructorQualifierConsumer.class);
        factory.registerBeanDefinition("constructorConsumer", bd3);

        ConstructorQualifierConsumer consumer = factory.getBean("constructorConsumer", ConstructorQualifierConsumer.class);

        // 验证构造器参数级别的 @Qualifier 生效
        assertNotNull(consumer.getPaymentService());
        assertEquals("wechatpay", consumer.getPaymentService().pay());
    }

    @Test
    public void testBeanDefinitionQualifier() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        BeanDefinition bd = new BeanDefinition("testBean", TestQualifierBean.class);
        bd.setQualifier("myQualifier");
        factory.registerBeanDefinition("testBean", bd);

        // 验证 qualifier 正确设置
        BeanDefinition retrieved = factory.getBeanDefinition("testBean");
        assertEquals("myQualifier", retrieved.getQualifier());
    }

    @Test
    public void testQualifierWithAnnotationScanning() throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
            AlipayService.class, WechatPayService.class);

        // 验证扫描时 @Qualifier 注解被正确读取
        BeanDefinition alipayBd = ctx.getBeanFactory().getBeanDefinition("alipayService");
        assertEquals("alipay", alipayBd.getQualifier());

        BeanDefinition wechatBd = ctx.getBeanFactory().getBeanDefinition("wechatPayService");
        assertEquals("wechatpay", wechatBd.getQualifier());

        ctx.close();
    }

    @Test
    public void testGetBeanByQualifier() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        BeanDefinition bd1 = new BeanDefinition("serviceA", AlipayService.class);
        bd1.setQualifier("serviceA");
        factory.registerBeanDefinition("serviceA", bd1);

        BeanDefinition bd2 = new BeanDefinition("serviceB", WechatPayService.class);
        bd2.setQualifier("serviceB");
        factory.registerBeanDefinition("serviceB", bd2);

        // 通过 qualifier 获取 Bean
        PaymentService serviceA = factory.getBeanByQualifier(PaymentService.class, "serviceA");
        assertEquals("alipay", serviceA.pay());

        PaymentService serviceB = factory.getBeanByQualifier(PaymentService.class, "serviceB");
        assertEquals("wechatpay", serviceB.pay());
    }

    @Test
    public void testMultipleBeansWithoutQualifierThrowsException() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        BeanDefinition bd1 = new BeanDefinition("service1", AlipayService.class);
        factory.registerBeanDefinition("service1", bd1);

        BeanDefinition bd2 = new BeanDefinition("service2", WechatPayService.class);
        factory.registerBeanDefinition("service2", bd2);

        // 多个同类型 Bean 且没有 @Primary/@Qualifier 应该抛异常
        try {
            factory.getBean(PaymentService.class);
            fail("Expected exception when multiple beans of same type without qualifier or primary");
        } catch (Exception e) {
            // Expected
            assertTrue(true);
        }
    }
}
