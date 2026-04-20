package com.rongx.mybatis.binding;

import com.rongx.mybatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

/**
 * MapperProxy 单元测试
 */
public class MapperProxyTest {

    private SqlSession sqlSession;
    private MapperProxy<TestMapper> mapperProxy;
    private Map<Method, MapperMethod> methodCache;

    @Before
    public void setUp() {
        sqlSession = new SimpleSqlSession();
        methodCache = new ConcurrentHashMap<>();
        mapperProxy = new MapperProxy<>(sqlSession, TestMapper.class, methodCache);
    }

    @Test
    public void testInvokeObjectMethod() throws Throwable {
        // 测试调用 Object 类的方法
        Method toStringMethod = Object.class.getMethod("toString");
        Object result = mapperProxy.invoke(null, toStringMethod, null);

        // Object 方法不会被代理
        assertNotNull(result);
    }

    @Test
    public void testInvokeEqualsMethod() throws Throwable {
        // 测试调用 equals 方法
        Method equalsMethod = Object.class.getMethod("equals", Object.class);
        Object result = mapperProxy.invoke(mapperProxy, equalsMethod, new Object[]{mapperProxy});

        // equals 应该正常工作
        assertTrue((Boolean) result);
    }

    @Test
    public void testInvokeHashCodeMethod() throws Throwable {
        // 测试调用 hashCode 方法
        Method hashCodeMethod = Object.class.getMethod("hashCode");
        Object result = mapperProxy.invoke(mapperProxy, hashCodeMethod, null);

        assertNotNull(result);
        assertTrue(result instanceof Integer);
    }

    @Test
    public void testMethodCacheIsEmptyInitially() {
        // 测试初始缓存为空
        assertEquals(0, methodCache.size());
    }

    // ==================== 测试辅助类 ====================

    interface TestMapper {
        String selectById(Long id);
        int insert(String name);
        int update(Long id, String name);
        Object delete(Long id);
    }

    /**
     * 简单的 SqlSession 实现用于测试
     */
    static class SimpleSqlSession implements SqlSession {
        @Override
        public <T> T selectOne(String statement) {
            return null;
        }

        @Override
        public <T> T selectOne(String statement, Object parameter) {
            return null;
        }

        @Override
        public <E> List<E> selectList(String statement, Object parameter) {
            return java.util.Collections.emptyList();
        }

        @Override
        public int insert(String statement, Object parameter) {
            return 0;
        }

        @Override
        public int update(String statement, Object parameter) {
            return 0;
        }

        @Override
        public Object delete(String statement, Object parameter) {
            return 0;
        }

        @Override
        public void commit() {
        }

        @Override
        public com.rongx.mybatis.session.Configuration getConfiguration() {
            return new com.rongx.mybatis.session.Configuration();
        }

        @Override
        public <T> T getMapper(Class<T> type) {
            return null;
        }
    }
}