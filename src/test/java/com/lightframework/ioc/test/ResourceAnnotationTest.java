package com.lightframework.ioc.test;

import com.lightframework.di.annotation.Autowired;
import com.lightframework.di.annotation.Component;
import com.lightframework.di.annotation.Resource;
import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.context.AnnotationConfigApplicationContext;
import com.lightframework.ioc.core.DefaultListableBeanFactory;
import com.lightframework.ioc.exception.BeanCreationException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// ==================== 测试用 Bean ====================

interface UserService {
    String getServiceName();
}

@Component("userService")
class UserServiceImpl implements UserService {
    @Override
    public String getServiceName() {
        return "UserService";
    }
}

@Component("userServiceImpl2")
class UserServiceImpl2 implements UserService {
    @Override
    public String getServiceName() {
        return "UserServiceImpl2";
    }
}

// 专门用于测试找不到 Bean 的场景
interface NotFoundInterface {}

// ==================== @Resource 按名称注入测试 ====================

@Component("nameResourceBean")
class NameResourceBean {
    @Resource(name = "userServiceImpl2")
    private UserService userService;

    public UserService getUserService() {
        return userService;
    }
}

// ==================== @Resource 默认名称注入测试（使用字段名） ====================

@Component("defaultNameResourceBean")
class DefaultNameResourceBean {
    @Resource
    private UserService userService;

    public UserService getUserService() {
        return userService;
    }
}

// ==================== @Resource 按类型注入测试 ====================

@Component("typeResourceBean")
class TypeResourceBean {
    @Resource(type = UserServiceImpl.class)
    private UserService userService;

    public UserService getUserService() {
        return userService;
    }
}

// ==================== @Resource 方法注入测试 ====================

@Component("methodResourceBean")
class MethodResourceBean {
    private UserService userService;

    @Resource(name = "userServiceImpl2")
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public UserService getUserService() {
        return userService;
    }
}

// ==================== @Resource setter 方法默认名称测试 ====================

@Component("defaultMethodResourceBean")
class DefaultMethodResourceBean {
    private UserService userService;

    @Resource
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public UserService getUserService() {
        return userService;
    }
}

// ==================== @Resource 和 @Autowired 共存时 @Resource 优先测试 ====================

@Component("mixedInjectionBean")
class MixedInjectionBean {
    // 同时使用 @Resource 和 @Autowired，@Resource 应该优先
    @Resource(name = "userServiceImpl2")
    @Autowired
    private UserService userService;

    public UserService getUserService() {
        return userService;
    }
}

// ==================== @Resource setter 与 @Autowired 共存测试 ====================

@Component("mixedMethodInjectionBean")
class MixedMethodInjectionBean {
    private UserService userService;

    @Resource(name = "userServiceImpl2")
    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public UserService getUserService() {
        return userService;
    }
}

// ==================== @Resource 找不到 Bean 时测试 ====================

class NotFoundResourceBean {
    @Resource(name = "nonExistentBean")
    private NotFoundInterface someBean;

    public NotFoundInterface getSomeBean() {
        return someBean;
    }
}

// ==================== 测试用例 ====================

public class ResourceAnnotationTest {

    @Test
    public void testResourceByName() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        BeanDefinition bd1 = new BeanDefinition("userService", UserServiceImpl.class);
        factory.registerBeanDefinition("userService", bd1);

        BeanDefinition bd2 = new BeanDefinition("userServiceImpl2", UserServiceImpl2.class);
        factory.registerBeanDefinition("userServiceImpl2", bd2);

        BeanDefinition bd3 = new BeanDefinition("nameResourceBean", NameResourceBean.class);
        factory.registerBeanDefinition("nameResourceBean", bd3);

        NameResourceBean bean = factory.getBean("nameResourceBean", NameResourceBean.class);

        // 验证 @Resource(name="userServiceImpl2") 注入的是 UserServiceImpl2
        assertNotNull(bean.getUserService());
        assertTrue(bean.getUserService() instanceof UserServiceImpl2);
    }

    @Test
    public void testResourceDefaultName() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        BeanDefinition bd1 = new BeanDefinition("userService", UserServiceImpl.class);
        factory.registerBeanDefinition("userService", bd1);

        BeanDefinition bd2 = new BeanDefinition("defaultNameResourceBean", DefaultNameResourceBean.class);
        factory.registerBeanDefinition("defaultNameResourceBean", bd2);

        DefaultNameResourceBean bean = factory.getBean("defaultNameResourceBean", DefaultNameResourceBean.class);

        // 验证 @Resource 使用字段名 "userService" 进行注入
        assertNotNull(bean.getUserService());
        assertEquals("UserService", bean.getUserService().getServiceName());
    }

    @Test
    public void testResourceByType() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        BeanDefinition bd1 = new BeanDefinition("userServiceImpl", UserServiceImpl.class);
        factory.registerBeanDefinition("userServiceImpl", bd1);

        BeanDefinition bd2 = new BeanDefinition("typeResourceBean", TypeResourceBean.class);
        factory.registerBeanDefinition("typeResourceBean", bd2);

        TypeResourceBean bean = factory.getBean("typeResourceBean", TypeResourceBean.class);

        // 验证 @Resource(type=UserServiceImpl.class) 按类型注入
        assertNotNull(bean.getUserService());
        assertTrue(bean.getUserService() instanceof UserServiceImpl);
    }

    @Test
    public void testResourceMethodInjection() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        BeanDefinition bd1 = new BeanDefinition("userService", UserServiceImpl.class);
        factory.registerBeanDefinition("userService", bd1);

        BeanDefinition bd2 = new BeanDefinition("userServiceImpl2", UserServiceImpl2.class);
        factory.registerBeanDefinition("userServiceImpl2", bd2);

        BeanDefinition bd3 = new BeanDefinition("methodResourceBean", MethodResourceBean.class);
        factory.registerBeanDefinition("methodResourceBean", bd3);

        MethodResourceBean bean = factory.getBean("methodResourceBean", MethodResourceBean.class);

        // 验证 @Resource(name="userServiceImpl2") 通过 setter 方法注入
        assertNotNull(bean.getUserService());
        assertTrue(bean.getUserService() instanceof UserServiceImpl2);
    }

    @Test
    public void testResourceMethodDefaultName() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        BeanDefinition bd1 = new BeanDefinition("userService", UserServiceImpl.class);
        factory.registerBeanDefinition("userService", bd1);

        BeanDefinition bd2 = new BeanDefinition("defaultMethodResourceBean", DefaultMethodResourceBean.class);
        factory.registerBeanDefinition("defaultMethodResourceBean", bd2);

        DefaultMethodResourceBean bean = factory.getBean("defaultMethodResourceBean", DefaultMethodResourceBean.class);

        // 验证 @Resource setter 方法使用属性名 "userService" 进行注入（去掉 set 前缀，首字母小写）
        assertNotNull(bean.getUserService());
        assertEquals("UserService", bean.getUserService().getServiceName());
    }

    @Test
    public void testResourcePriorityOverAutowired() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        BeanDefinition bd1 = new BeanDefinition("userService", UserServiceImpl.class);
        factory.registerBeanDefinition("userService", bd1);

        BeanDefinition bd2 = new BeanDefinition("userServiceImpl2", UserServiceImpl2.class);
        factory.registerBeanDefinition("userServiceImpl2", bd2);

        BeanDefinition bd3 = new BeanDefinition("mixedInjectionBean", MixedInjectionBean.class);
        factory.registerBeanDefinition("mixedInjectionBean", bd3);

        MixedInjectionBean bean = factory.getBean("mixedInjectionBean", MixedInjectionBean.class);

        // 验证 @Resource 优先级高于 @Autowired
        assertNotNull(bean.getUserService());
        assertTrue(bean.getUserService() instanceof UserServiceImpl2);
    }

    @Test
    public void testResourceMethodPriorityOverAutowired() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        BeanDefinition bd1 = new BeanDefinition("userService", UserServiceImpl.class);
        factory.registerBeanDefinition("userService", bd1);

        BeanDefinition bd2 = new BeanDefinition("userServiceImpl2", UserServiceImpl2.class);
        factory.registerBeanDefinition("userServiceImpl2", bd2);

        BeanDefinition bd3 = new BeanDefinition("mixedMethodInjectionBean", MixedMethodInjectionBean.class);
        factory.registerBeanDefinition("mixedMethodInjectionBean", bd3);

        MixedMethodInjectionBean bean = factory.getBean("mixedMethodInjectionBean", MixedMethodInjectionBean.class);

        // 验证 @Resource setter 优先级高于 @Autowired setter
        assertNotNull(bean.getUserService());
        assertTrue(bean.getUserService() instanceof UserServiceImpl2);
    }

    @Test
    public void testResourceNotFound() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        BeanDefinition bd = new BeanDefinition("notFoundResourceBean", NotFoundResourceBean.class);
        factory.registerBeanDefinition("notFoundResourceBean", bd);

        // 验证找不到 Bean 时抛异常
        try {
            factory.getBean("notFoundResourceBean", NotFoundResourceBean.class);
            fail("Expected exception when bean not found");
        } catch (Exception e) {
            // 预期抛出 NoSuchBeanDefinitionException 或 BeanCreationException
            assertTrue(e.getMessage().contains("NotFoundInterface")
                    || e.getMessage().contains("notFoundResourceBean"));
        }
    }

    @Test
    public void testResourceWithAnnotationScanning() throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
            UserServiceImpl.class, UserServiceImpl2.class, NameResourceBean.class);

        NameResourceBean bean = ctx.getBean("nameResourceBean", NameResourceBean.class);
        assertNotNull(bean.getUserService());
        assertTrue(bean.getUserService() instanceof UserServiceImpl2);

        ctx.close();
    }
}
