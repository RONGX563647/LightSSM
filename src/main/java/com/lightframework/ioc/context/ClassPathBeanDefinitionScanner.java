package com.lightframework.ioc.context;

import com.lightframework.ioc.annotation.Component;
import com.lightframework.ioc.annotation.DependsOn;
import com.lightframework.ioc.annotation.Lazy;
import com.lightframework.ioc.annotation.Primary;
import com.lightframework.ioc.annotation.Profile;
import com.lightframework.ioc.annotation.Qualifier;
import com.lightframework.ioc.annotation.Scope;
import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.core.DefaultListableBeanFactory;
import com.lightframework.ioc.core.Environment;
import com.lightframework.ioc.core.StandardEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class ClassPathBeanDefinitionScanner {

    private static final Logger logger = LoggerFactory.getLogger(ClassPathBeanDefinitionScanner.class);

    // Phase 3: META-INF/lightssm.components cache — pre-built list of @Component classes
    private static java.util.Set<String> cachedComponents;
    static {
        try {
            Enumeration<URL> urls = Thread.currentThread().getContextClassLoader()
                    .getResources("META-INF/lightssm.components");
            if (urls.hasMoreElements()) {
                java.util.Set<String> set = new java.util.LinkedHashSet<>();
                while (urls.hasMoreElements()) {
                    try (InputStream is = urls.nextElement().openStream()) {
                        java.util.Scanner sc = new java.util.Scanner(is, "UTF-8");
                        while (sc.hasNextLine()) {
                            String line = sc.nextLine().trim();
                            if (!line.isEmpty() && !line.startsWith("#")) {
                                set.add(line);
                            }
                        }
                    }
                }
                cachedComponents = set;
                logger.info("Loaded {} cached component classes from META-INF/lightssm.components", set.size());
            }
        } catch (Exception e) {
            logger.debug("No META-INF/lightssm.components cache found, using full scan");
        }
    }

    private final DefaultListableBeanFactory beanFactory;
    private Environment environment = new StandardEnvironment();
    private boolean parallelScan = false;

    public ClassPathBeanDefinitionScanner(DefaultListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public void setParallelScan(boolean parallelScan) {
        this.parallelScan = parallelScan;
    }

    public boolean isParallelScan() {
        return parallelScan;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * 扫描多个基础包，注册 @Component Bean 定义
     */
    public int scan(String... basePackages) throws Exception {
        if (parallelScan && basePackages.length > 1) {
            return scanParallel(basePackages);
        }
        int count = 0;
        for (String basePackage : basePackages) {
            count += doScan(basePackage);
        }
        return count;
    }

    /**
     * 并行扫描多个包，使用 ConcurrentHashMap 收集后批量注册，避免并发问题
     */
    protected int scanParallel(String... basePackages) throws Exception {
        Map<String, BeanDefinition> definitions = new ConcurrentHashMap<>();

        List<Integer> results = List.of(basePackages).parallelStream()
            .map(pkg -> {
                try {
                    return doScan(pkg, definitions);
                } catch (Exception e) {
                    logger.warn("Failed to scan package: " + pkg, e);
                    return 0;
                }
            })
            .collect(Collectors.toList());

        for (Map.Entry<String, BeanDefinition> entry : definitions.entrySet()) {
            beanFactory.registerBeanDefinition(entry.getKey(), entry.getValue());
        }

        return results.stream().mapToInt(Integer::intValue).sum();
    }

    // 单线程扫描入口
    protected int doScan(String basePackage) throws Exception {
        return doScan(basePackage, null);
    }

    // 统一扫描入口：collectTo 非空时收集到 Map，空时直接注册
    protected int doScan(String basePackage, Map<String, BeanDefinition> collectTo) throws Exception {
        String packagePath = basePackage.replace('.', '/');
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader()
            .getResources(packagePath);

        int count = 0;
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();
            if ("file".equals(protocol)) {
                count += scanDirectory(new File(resource.toURI()), basePackage, collectTo);
            } else if ("jar".equals(protocol)) {
                count += scanJar(resource, packagePath, basePackage, collectTo);
            }
        }
        return count;
    }

    // 扫描目录（统一实现）
    protected int scanDirectory(File directory, String basePackage, Map<String, BeanDefinition> collectTo) throws Exception {
        if (!directory.exists()) return 0;
        File[] files = directory.listFiles();
        if (files == null) return 0;

        int count = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                count += scanDirectory(file, basePackage + "." + file.getName(), collectTo);
            } else if (file.getName().endsWith(".class")) {
                String className = basePackage + '.' + file.getName().substring(0,
                    file.getName().length() - 6);
                byte[] classBytes = Files.readAllBytes(file.toPath());
                if (processClass(className, classBytes, collectTo)) {
                    count++;
                }
            }
        }
        return count;
    }

    // 扫描 JAR 文件（统一实现）
    protected int scanJar(URL jarUrl, String packagePath, String basePackage, Map<String, BeanDefinition> collectTo) throws Exception {
        int count = 0;
        if (!"jar".equals(jarUrl.getProtocol())) return 0;

        JarURLConnection conn = (JarURLConnection) jarUrl.openConnection();
        try (JarInputStream jis = new JarInputStream(conn.getInputStream())) {
            JarEntry entry;
            String packagePrefix = packagePath.replace('/', '.') + ".";
            while ((entry = jis.getNextJarEntry()) != null) {
                String entryName = entry.getName();
                if (entryName.endsWith(".class")) {
                    String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                    if (className.startsWith(packagePrefix) || className.startsWith(basePackage)) {
                        byte[] classBytes = jis.readAllBytes();
                        if (processClass(className, classBytes, collectTo)) {
                            count++;
                        }
                    } else {
                        jis.readAllBytes();
                    }
                }
            }
        }
        return count;
    }

    /**
     * 统一处理类：检查 @Component 或其派生注解、Profile，注册/收集 BeanDefinition
     */
    protected boolean processClass(String className, byte[] classBytes, Map<String, BeanDefinition> collectTo) throws Exception {
        if (cachedComponents != null && !cachedComponents.contains(className)) {
            return false;
        }
        if (cachedComponents == null && classBytes != null && !hasComponentAnnotation(classBytes)) {
            return false;
        }
        try {
            Class<?> clazz = Class.forName(className);
            Component component = findComponentAnnotation(clazz);

            if (component != null && !clazz.isInterface() && !clazz.isAnnotation()) {
                Profile profile = clazz.getAnnotation(Profile.class);
                if (profile != null && !environment.acceptsProfiles(profile.value())) {
                    logger.debug("Skipping bean {} - profile not active", className);
                    return false;
                }
                registerOrCollectBeanDefinition(clazz, component, collectTo);
                logger.debug("Found component class: {}", className);
                return true;
            }
        } catch (ClassNotFoundException e) {
            logger.warn("Could not load class: {}", className);
        }
        return false;
    }

    /**
     * 检查字节码是否包含 @Component 或其派生注解
     */
    private boolean hasComponentAnnotation(byte[] classBytes) {
        // 先检查 @Component
        if (scanAnnotationWithAsm(classBytes, "Lcom/lightframework/ioc/annotation/Component;")) {
            return true;
        }
        // 检查派生注解（@Service, @Repository, @Controller 等）
        return scanAnnotationWithAsm(classBytes, "Lcom/lightframework/ioc/annotation/Service;")
            || scanAnnotationWithAsm(classBytes, "Lcom/lightframework/ioc/annotation/Repository;")
            || scanAnnotationWithAsm(classBytes, "Lcom/lightframework/ioc/annotation/Controller;");
    }

    /**
     * 查找 @Component 注解或其派生注解（如 @Service, @Repository）
     */
    private Component findComponentAnnotation(Class<?> clazz) {
        // 直接标注 @Component
        Component component = clazz.getAnnotation(Component.class);
        if (component != null) {
            return component;
        }
        // 检查派生注解（@Service, @Repository 等标注了 @Component 的注解）
        for (Annotation ann : clazz.getAnnotations()) {
            if (ann.annotationType().isAnnotationPresent(Component.class)) {
                return createComponentProxy(ann);
            }
        }
        return null;
    }

    /**
     * 为派生注解创建 @Component 代理
     */
    private Component createComponentProxy(Annotation derivedAnnotation) {
        return new Component() {
            @Override
            public String value() {
                try {
                    return (String) derivedAnnotation.annotationType().getMethod("value").invoke(derivedAnnotation);
                } catch (Exception e) {
                    return "";
                }
            }
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return Component.class;
            }
        };
    }

    /**
     * 注册 BeanDefinition 到 Factory 或收集到 Map（并行扫描时使用）
     */
    protected void registerOrCollectBeanDefinition(Class<?> beanClass, Component component, Map<String, BeanDefinition> collectTo) {
        String beanName = component.value();
        if (beanName.isEmpty()) {
            beanName = generateBeanName(beanClass);
        }

        BeanDefinition bd = new BeanDefinition(beanName, beanClass);

        Scope scope = beanClass.getAnnotation(Scope.class);
        if (scope != null) {
            bd.setScope(scope.value());
        }

        if (beanClass.isAnnotationPresent(Primary.class)) {
            bd.setPrimary(true);
        }

        Qualifier qualifier = beanClass.getAnnotation(Qualifier.class);
        if (qualifier != null && !qualifier.value().isEmpty()) {
            bd.setQualifier(qualifier.value());
        }

        Lazy lazy = beanClass.getAnnotation(Lazy.class);
        if (lazy != null) {
            bd.setLazyInit(lazy.value());
        }

        DependsOn dependsOn = beanClass.getAnnotation(DependsOn.class);
        if (dependsOn != null && dependsOn.value().length > 0) {
            bd.setDependsOn(dependsOn.value());
        }

        if (collectTo != null) {
            collectTo.put(beanName, bd);
        } else {
            this.beanFactory.registerBeanDefinition(beanName, bd);
        }
    }

    /**
     * ASM 预扫描 — 直接读字节码判断 @Component，避免 Class.forName
     */
    private boolean scanAnnotationWithAsm(byte[] classBytes, String annotationDescriptor) {
        try {
            ClassReader reader = new ClassReader(classBytes);
            boolean[] found = {false};
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    if (descriptor.equals(annotationDescriptor)) {
                        found[0] = true;
                    }
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return found[0];
        } catch (Exception e) {
            logger.debug("ASM scan failed for class, falling back to Class.forName: {}", e.getMessage());
            return true;
        }
    }

    protected String generateBeanName(Class<?> beanClass) {
        String shortName = beanClass.getSimpleName();
        // 处理特殊情况：如 XMLParser -> xmlParser, URLHandler -> urlHandler
        if (shortName.length() > 1 && Character.isUpperCase(shortName.charAt(1)) 
                && Character.isUpperCase(shortName.charAt(0))) {
            return shortName;
        }
        return shortName.substring(0, 1).toLowerCase() + shortName.substring(1);
    }

    // Legacy aliases for backward compatibility
    protected int findAndRegisterComponentsInDirectory(File directory, String basePackage) throws Exception {
        return scanDirectory(directory, basePackage, null);
    }

    protected int findAndRegisterComponentsInJar(URL jarUrl, String packagePath, String basePackage) throws Exception {
        return scanJar(jarUrl, packagePath, basePackage, null);
    }
}
