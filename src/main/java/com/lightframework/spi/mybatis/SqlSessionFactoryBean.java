package com.lightframework.spi.mybatis;

import com.lightframework.ioc.core.FactoryBean;
import com.lightframework.ioc.core.InitializingBean;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class SqlSessionFactoryBean implements FactoryBean<SqlSessionFactory>, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(SqlSessionFactoryBean.class);

    private DataSource dataSource;
    private String configLocation;
    private String[] mapperLocations;
    private String mapperBasePackage;
    private org.apache.ibatis.session.Configuration configuration;
    private SqlSessionFactory sqlSessionFactory;

    @Override
    public void afterPropertiesSet() throws Exception {
        org.apache.ibatis.session.Configuration config;
        if (configLocation != null) {
            try (InputStream is = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(configLocation)) {
                if (is == null) {
                    throw new RuntimeException("MyBatis config not found: " + configLocation);
                }
                SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
                this.sqlSessionFactory = builder.build(is);
                config = this.sqlSessionFactory.getConfiguration();
            }
        } else {
            config = this.configuration;
            if (config == null) {
                config = new org.apache.ibatis.session.Configuration();
            }
            config.setMapUnderscoreToCamelCase(true);
            if (dataSource != null) {
                org.apache.ibatis.mapping.Environment env =
                    new org.apache.ibatis.mapping.Environment("lightssm",
                        new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(),
                        dataSource);
                config.setEnvironment(env);
            }
        }
        if (config != null) {
            if (mapperBasePackage != null && !mapperBasePackage.isEmpty()) {
                config.addMappers(mapperBasePackage);
                logger.info("Added mappers from package: {}", mapperBasePackage);
            }
            if (mapperLocations != null) {
                for (String location : mapperLocations) {
                    loadXmlMappers(config, location);
                }
            }
        }
        if (this.sqlSessionFactory == null) {
            SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
            this.sqlSessionFactory = builder.build(config);
        }
        logger.info("SqlSessionFactory initialized successfully");
    }

    private void loadXmlMappers(org.apache.ibatis.session.Configuration config, String location) throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (location.startsWith("classpath:")) {
            location = location.substring("classpath:".length());
        }
        location = location.replace('\\', '/');
        if (location.contains("*")) {
            String dirPath = location.substring(0, location.indexOf('*'));
            int lastSlash = dirPath.lastIndexOf('/');
            String prefixDir = lastSlash >= 0 ? dirPath.substring(0, lastSlash) : "";
            Enumeration<URL> resources = cl.getResources(prefixDir.isEmpty() ? "." : prefixDir);
            List<URL> xmlFiles = new ArrayList<>();
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    File dir = new File(url.toURI());
                    collectXmlFiles(dir, location, xmlFiles);
                } else if ("jar".equals(protocol)) {
                    logger.warn("Wildcard mapper locations in JAR not fully supported: {}", location);
                }
            }
            for (URL xmlUrl : xmlFiles) {
                try (InputStream is = xmlUrl.openStream()) {
                    String resource = xmlUrl.toString();
                    XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(is, config, resource, config.getSqlFragments());
                    xmlMapperBuilder.parse();
                    logger.debug("Loaded XML mapper: {}", resource);
                }
            }
        } else {
            InputStream is = cl.getResourceAsStream(location);
            if (is == null) {
                logger.warn("Mapper XML not found: {}", location);
                return;
            }
            try {
                XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(is, config, location, config.getSqlFragments());
                xmlMapperBuilder.parse();
                logger.info("Loaded XML mapper: {}", location);
            } finally {
                is.close();
            }
        }
    }

    private void collectXmlFiles(File dir, String pattern, List<URL> result) throws Exception {
        if (!dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                collectXmlFiles(file, pattern, result);
            } else if (file.getName().endsWith(".xml")) {
                String relativePath = file.toURI().toString();
                if (matchPattern(relativePath, pattern)) {
                    result.add(file.toURI().toURL());
                }
            }
        }
    }

    private static boolean matchPattern(String path, String pattern) {
        String regex = pattern
            .replace(".", "\\.")
            .replace("**", ".+")
            .replace("*", "[^/]+");
        return path.matches(".*" + regex);
    }

    @Override
    public SqlSessionFactory getObject() {
        return sqlSessionFactory;
    }

    @Override
    public Class<?> getObjectType() {
        return SqlSessionFactory.class;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setConfigLocation(String configLocation) {
        this.configLocation = configLocation;
    }

    public void setMapperLocations(String[] mapperLocations) {
        this.mapperLocations = mapperLocations;
    }

    public void setMapperBasePackage(String mapperBasePackage) {
        this.mapperBasePackage = mapperBasePackage;
    }

    public void setConfiguration(org.apache.ibatis.session.Configuration configuration) {
        this.configuration = configuration;
    }
}
