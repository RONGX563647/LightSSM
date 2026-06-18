package com.lightframework.ioc.test;

import com.lightframework.ioc.annotation.Component;
import com.lightframework.ioc.annotation.DependsOn;
import com.lightframework.ioc.context.AnnotationConfigApplicationContext;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

// 测试用 Bean：记录初始化顺序
@Component
class InitOrderRecorder {
    public static final List<String> initOrder = Collections.synchronizedList(new ArrayList<>());
    
    public static void record(String beanName) {
        initOrder.add(beanName);
    }
    
    public static void clear() {
        initOrder.clear();
    }
}

@Component
class DatabaseInitializer {
    @jakarta.annotation.PostConstruct
    public void init() {
        InitOrderRecorder.record("databaseInitializer");
    }
}

@Component
class CacheInitializer {
    @jakarta.annotation.PostConstruct
    public void init() {
        InitOrderRecorder.record("cacheInitializer");
    }
}

@DependsOn({"databaseInitializer", "cacheInitializer"})
@Component
class AppService {
    @jakarta.annotation.PostConstruct
    public void init() {
        InitOrderRecorder.record("appService");
    }
}

// 链式依赖：C dependsOn B, B dependsOn A
@Component
class ChainA {
    @jakarta.annotation.PostConstruct
    public void init() {
        InitOrderRecorder.record("chainA");
    }
}

@DependsOn("chainA")
@Component
class ChainB {
    @jakarta.annotation.PostConstruct
    public void init() {
        InitOrderRecorder.record("chainB");
    }
}

@DependsOn("chainB")
@Component
class ChainC {
    @jakarta.annotation.PostConstruct
    public void init() {
        InitOrderRecorder.record("chainC");
    }
}

public class DependsOnTest {

    @Test
    public void testDependsOnBasic() throws Exception {
        InitOrderRecorder.clear();
        
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
            DatabaseInitializer.class, CacheInitializer.class, AppService.class);
        
        // 验证初始化顺序：databaseInitializer 和 cacheInitializer 应该在 appService 之前
        List<String> order = InitOrderRecorder.initOrder;
        assertTrue(order.contains("databaseInitializer"));
        assertTrue(order.contains("cacheInitializer"));
        assertTrue(order.contains("appService"));
        
        int dbIndex = order.indexOf("databaseInitializer");
        int cacheIndex = order.indexOf("cacheInitializer");
        int appIndex = order.indexOf("appService");
        
        assertTrue(dbIndex < appIndex, "databaseInitializer should be initialized before appService");
        assertTrue(cacheIndex < appIndex, "cacheInitializer should be initialized before appService");
        
        ctx.close();
    }

    @Test
    public void testDependsOnChain() throws Exception {
        InitOrderRecorder.clear();
        
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
            ChainA.class, ChainB.class, ChainC.class);
        
        List<String> order = InitOrderRecorder.initOrder;
        
        // 链式依赖：A -> B -> C
        int aIndex = order.indexOf("chainA");
        int bIndex = order.indexOf("chainB");
        int cIndex = order.indexOf("chainC");
        
        assertTrue(aIndex < bIndex, "chainA should be initialized before chainB");
        assertTrue(bIndex < cIndex, "chainB should be initialized before chainC");
        
        ctx.close();
    }

    @Test
    public void testDependsOnWithManualRegistration() throws Exception {
        InitOrderRecorder.clear();
        
        com.lightframework.ioc.beans.BeanDefinition dbBd = new com.lightframework.ioc.beans.BeanDefinition("myDb", DatabaseInitializer.class);
        com.lightframework.ioc.beans.BeanDefinition appBd = new com.lightframework.ioc.beans.BeanDefinition("myApp", AppService.class);
        appBd.setDependsOn(new String[]{"myDb"});
        
        com.lightframework.ioc.core.DefaultListableBeanFactory factory = new com.lightframework.ioc.core.DefaultListableBeanFactory();
        factory.registerBeanDefinition("myDb", dbBd);
        factory.registerBeanDefinition("myApp", appBd);
        factory.preInstantiateSingletons();
        
        List<String> order = InitOrderRecorder.initOrder;
        int dbIndex = order.indexOf("databaseInitializer");
        int appIndex = order.indexOf("appService");
        
        assertTrue(dbIndex < appIndex, "myDb should be initialized before myApp");
    }
}
