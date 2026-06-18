package com.lightframework.orm.builder;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParameterExpressionTest {

    @Test
    void testSimpleProperty() {
        ParameterExpression exp = new ParameterExpression("id");
        assertEquals("id", exp.get("property"));
        assertNull(exp.get("jdbcType"));
    }

    @Test
    void testPropertyWithJdbcType() {
        ParameterExpression exp = new ParameterExpression("id:INTEGER");
        assertEquals("id", exp.get("property"));
        assertEquals("INTEGER", exp.get("jdbcType"));
    }

    @Test
    void testPropertyWithJavaType() {
        ParameterExpression exp = new ParameterExpression("name,javaType=int");
        assertEquals("name", exp.get("property"));
        assertEquals("int", exp.get("javaType"));
    }

    @Test
    void testFullExpression() {
        ParameterExpression exp = new ParameterExpression("age,javaType=int,jdbcType=NUMERIC");
        assertEquals("age", exp.get("property"));
        assertEquals("int", exp.get("javaType"));
        assertEquals("NUMERIC", exp.get("jdbcType"));
    }

    @Test
    void testExpressionWithWhitespace() {
        ParameterExpression exp = new ParameterExpression("  id  :  INTEGER  ");
        assertEquals("id", exp.get("property"));
        assertEquals("INTEGER", exp.get("jdbcType"));
    }

    @Test
    void testExpressionWithTypeHandler() {
        ParameterExpression exp = new ParameterExpression("data,typeHandler=MyHandler");
        assertEquals("data", exp.get("property"));
        assertEquals("MyHandler", exp.get("typeHandler"));
    }

    @Test
    void testExpressionWithNumericScale() {
        ParameterExpression exp = new ParameterExpression("price,jdbcType=DECIMAL,numericScale=2");
        assertEquals("price", exp.get("property"));
        assertEquals("DECIMAL", exp.get("jdbcType"));
        assertEquals("2", exp.get("numericScale"));
    }

    @Test
    void testComplexProperty() {
        ParameterExpression exp = new ParameterExpression("user.name");
        assertEquals("user.name", exp.get("property"));
    }
}
