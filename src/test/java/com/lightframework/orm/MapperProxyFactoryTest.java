package com.rongx.mybatis.binding;

import com.rongx.mybatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * MapperProxyFactory 单元测试
 */
public class MapperProxyFactoryTest {

    private SqlSession sqlSession;
    private MapperProxyFactory<TestMapper> factory;

    @Before
    public void setUp() {
        sqlSession = new SimpleSqlSession();
        factory = new MapperProxyFactory<>(TestMapper.class);
    }

    @Test
    public void testNewInstance() {
        TestMapper mapper = factory.newInstance(sqlSession);
        assertNotNull(mapper);
        assertTrue(TestMapper.class.isInstance(mapper));
    }

    @Test
    public void testGetMapperInterface() {
        // 验证工厂创建的是正确的接口类型
        TestMapper mapper = factory.newInstance(sqlSession);
        assertEquals(TestMapper.class, mapper.getClass().getInterfaces()[0]);
    }

    @Test
    public void testMethodCache() {
        assertNotNull(factory.getMethodCache());
        assertTrue(factory.getMethodCache().isEmpty());
    }

    @Test
    public void testMultipleInstances() {
        // 多次创建应该返回不同的代理实例
        TestMapper mapper1 = factory.newInstance(sqlSession);
        TestMapper mapper2 = factory.newInstance(sqlSession);

        assertNotSame(mapper1, mapper2);
    }

    // ==================== 测试辅助类 ====================

    interface TestMapper {
        String selectById(Long id);
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