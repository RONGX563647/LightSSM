package com.lightframework.orm.type;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

class TypeHandlerTest {

    @Test
    void testSimpleTypeRegistry() {
        assertTrue(SimpleTypeRegistry.isSimpleType(String.class));
        assertTrue(SimpleTypeRegistry.isSimpleType(Integer.class));
        assertTrue(SimpleTypeRegistry.isSimpleType(Long.class));
        assertTrue(SimpleTypeRegistry.isSimpleType(Boolean.class));
        assertTrue(SimpleTypeRegistry.isSimpleType(Double.class));
        assertTrue(SimpleTypeRegistry.isSimpleType(Float.class));
        assertTrue(SimpleTypeRegistry.isSimpleType(Short.class));
        assertTrue(SimpleTypeRegistry.isSimpleType(Byte.class));
        assertTrue(SimpleTypeRegistry.isSimpleType(Character.class));
        assertTrue(SimpleTypeRegistry.isSimpleType(Date.class));
        assertTrue(SimpleTypeRegistry.isSimpleType(BigDecimal.class));
        assertTrue(SimpleTypeRegistry.isSimpleType(BigInteger.class));
        assertFalse(SimpleTypeRegistry.isSimpleType(Object.class));
        assertFalse(SimpleTypeRegistry.isSimpleType(TypeHandlerTest.class));
    }

    @Test
    void testTypeHandlerRegistryBasicTypes() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        assertNotNull(registry.getTypeHandler(Integer.class, null));
        assertNotNull(registry.getTypeHandler(String.class, null));
        assertNotNull(registry.getTypeHandler(Long.class, null));
        assertNotNull(registry.getTypeHandler(Boolean.class, null));
        assertNotNull(registry.getTypeHandler(Double.class, null));
    }

    @Test
    void testTypeHandlerRegistryReturnsDefaultForUnknown() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        TypeHandler<?> handler = registry.getTypeHandler(TypeHandlerTest.class, null);
        assertNotNull(handler);
        assertTrue(handler instanceof ObjectTypeHandler);
    }

    @Test
    void testTypeHandlerRegistryPrimitive() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        assertNotNull(registry.getTypeHandler(int.class, null));
        assertNotNull(registry.getTypeHandler(boolean.class, null));
        assertNotNull(registry.getTypeHandler(long.class, null));
    }

    @Test
    void testTypeAliasRegistry() {
        TypeAliasRegistry registry = new TypeAliasRegistry();
        assertEquals(String.class, registry.resolveAlias("string"));
        assertEquals(Integer.class, registry.resolveAlias("int"));
        assertEquals(Integer.class, registry.resolveAlias("integer"));
        assertEquals(Long.class, registry.resolveAlias("long"));
        assertEquals(Double.class, registry.resolveAlias("double"));
        assertEquals(Boolean.class, registry.resolveAlias("boolean"));
    }

    @Test
    void testTypeAliasRegistryCustomClass() throws Exception {
        TypeAliasRegistry registry = new TypeAliasRegistry();
        Class<?> clazz = registry.resolveAlias("java.lang.StringBuilder");
        assertEquals(StringBuilder.class, clazz);
    }

    @Test
    void testJdbcTypeValues() {
        assertEquals(java.sql.Types.VARCHAR, JdbcType.VARCHAR.TYPE_CODE);
        assertEquals(java.sql.Types.INTEGER, JdbcType.INTEGER.TYPE_CODE);
        assertEquals(java.sql.Types.BIGINT, JdbcType.BIGINT.TYPE_CODE);
        assertEquals(java.sql.Types.DATE, JdbcType.DATE.TYPE_CODE);
        assertEquals(java.sql.Types.TIMESTAMP, JdbcType.TIMESTAMP.TYPE_CODE);
    }
}
