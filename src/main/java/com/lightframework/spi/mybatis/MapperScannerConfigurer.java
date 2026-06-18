package com.lightframework.spi.mybatis;

import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.core.BeanDefinitionRegistry;
import com.lightframework.ioc.core.BeanDefinitionRegistryPostProcessor;
import com.lightframework.ioc.core.DefaultListableBeanFactory;
import com.lightframework.spi.mybatis.annotation.Mapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class MapperScannerConfigurer implements BeanDefinitionRegistryPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MapperScannerConfigurer.class);

    private String basePackage;
    private Class<? extends java.lang.annotation.Annotation> annotationClass = Mapper.class;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws Exception {
        if (basePackage == null || basePackage.isEmpty()) {
            logger.warn("MapperScannerConfigurer.basePackage is not set, skipping mapper scan");
            return;
        }

        List<Class<?>> mapperInterfaces = scanMapperInterfaces(basePackage);
        logger.info("Found {} mapper interfaces in package '{}'", mapperInterfaces.size(), basePackage);

        for (Class<?> mapperInterface : mapperInterfaces) {
            String beanName = mapperInterface.getName();
            BeanDefinition bd = new BeanDefinition(beanName, MyBatisMapperFactoryBean.class);
            bd.setScope("singleton");
            bd.setPropertyValue("mapperInterface", mapperInterface);
            registry.registerBeanDefinition(beanName, bd);

            if (logger.isDebugEnabled()) {
                logger.debug("Registered mapper bean: {} -> {}", beanName, mapperInterface.getName());
            }
        }
    }

    @Override
    public void postProcessBeanFactory(DefaultListableBeanFactory beanFactory) throws Exception {
    }

    protected List<Class<?>> scanMapperInterfaces(String basePackage) throws Exception {
        List<Class<?>> mappers = new ArrayList<>();
        String packagePath = basePackage.replace('.', '/');
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        Enumeration<URL> resources = cl.getResources(packagePath);
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();
            if ("file".equals(protocol)) {
                File dir = new File(resource.toURI());
                scanDirectory(dir, basePackage, mappers);
            } else if ("jar".equals(protocol)) {
                scanJar(resource, packagePath, basePackage, mappers);
            }
        }
        return mappers;
    }

    protected void scanDirectory(File dir, String basePackage, List<Class<?>> mappers) throws Exception {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, basePackage + "." + file.getName(), mappers);
            } else if (file.getName().endsWith(".class")) {
                String className = basePackage + "." + file.getName().substring(0, file.getName().length() - 6);
                registerIfMapper(className, mappers);
            }
        }
    }

    protected void scanJar(URL jarUrl, String packagePath, String basePackage, List<Class<?>> mappers) throws Exception {
        JarURLConnection conn = (JarURLConnection) jarUrl.openConnection();
        String packagePrefix = packagePath.replace('/', '.') + ".";
        try (JarInputStream jis = new JarInputStream(conn.getInputStream())) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                String entryName = entry.getName();
                if (entryName.endsWith(".class")) {
                    String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                    if (className.startsWith(packagePrefix) || className.startsWith(basePackage)) {
                        registerIfMapper(className, mappers);
                    }
                }
            }
        }
    }

    protected void registerIfMapper(String className, List<Class<?>> mappers) {
        Class<?> clazz = tryLoadMapperClass(className);
        if (clazz != null) {
            mappers.add(clazz);
        }
    }

    /**
     * Uses ASM to check if a class is a mapper interface without fully loading it.
     * Falls back to Class.forName only if ASM analysis is inconclusive or the class
     * is confirmed to be a mapper candidate by bytecode pre-scan.
     */
    private Class<?> tryLoadMapperClass(String className) {
        String internalName = className.replace('.', '/') + ".class";
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream is = cl.getResourceAsStream(internalName)) {
            if (is != null) {
                byte[] classBytes = is.readAllBytes();
                ClassReader reader = new ClassReader(classBytes);
                int access = reader.getAccess();
                boolean isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
                if (!isInterface) {
                    return null;
                }
                // Check for @Mapper annotation by scanning the annotation descriptors
                String mapperDesc = "L" + annotationClass.getName().replace('.', '/') + ";";
                String[] annotations = reader.getInterfaces(); // not annotations!
                // Actually, we need to check class annotations via getClassVisitor
                // Use a lightweight ASM visitor
                boolean[] found = {false};
                reader.accept(new org.objectweb.asm.ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public org.objectweb.asm.AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        if (mapperDesc.equals(descriptor)) {
                            found[0] = true;
                        }
                        return null;
                    }
                }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

                if (!found[0]) {
                    return null;
                }
            }
        } catch (Exception e) {
            logger.debug("ASM pre-scan failed for {}, falling back to Class.forName: {}", className, e.getMessage());
        }

        // Load the class only if it passed the ASM pre-check
        try {
            Class<?> clazz = Class.forName(className, false, cl);
            if (clazz.isInterface() && clazz.isAnnotationPresent(annotationClass)) {
                return clazz;
            }
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            logger.debug("Could not load mapper class: {}", className);
        }
        return null;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public void setAnnotationClass(Class<? extends java.lang.annotation.Annotation> annotationClass) {
        this.annotationClass = annotationClass;
    }

    public String getBasePackage() {
        return basePackage;
    }
}
