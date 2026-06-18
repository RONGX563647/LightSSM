package com.lightframework.orm.scripting.xmltags;

import com.lightframework.orm.builder.SqlSourceBuilder;
import com.lightframework.orm.mapping.BoundSql;
import com.lightframework.orm.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DynamicSqlSourceTest {

    private Configuration configuration;
    private DynamicContext context;

    @BeforeEach
    void setUp() {
        configuration = new Configuration();
        context = new DynamicContext(configuration, new HashMap<>());
    }

    @Test
    void testStaticTextSqlNode() {
        StaticTextSqlNode node = new StaticTextSqlNode("SELECT * FROM user");
        node.apply(context);
        assertEquals("SELECT * FROM user", context.getSql());
    }

    @Test
    void testTextSqlNodeStatic() {
        TextSqlNode node = new TextSqlNode("SELECT * FROM user WHERE id = ?");
        assertFalse(node.isDynamic());
        node.apply(context);
        assertTrue(context.getSql().contains("SELECT * FROM user"));
    }

    @Test
    void testTextSqlNodeDynamic() {
        TextSqlNode node = new TextSqlNode("SELECT * FROM ${table}");
        assertTrue(node.isDynamic());
    }

    @Test
    void testIfSqlNodeTrue() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "test");
        DynamicContext ctx = new DynamicContext(configuration, params);

        IfSqlNode ifNode = new IfSqlNode(new StaticTextSqlNode("AND name = #{name}"), "name != null");
        ifNode.apply(ctx);
        assertTrue(ctx.getSql().contains("AND name ="));
    }

    @Test
    void testIfSqlNodeFalse() {
        Map<String, Object> params = new HashMap<>();
        DynamicContext ctx = new DynamicContext(configuration, params);

        IfSqlNode ifNode = new IfSqlNode(new StaticTextSqlNode("AND name = #{name}"), "name != null");
        ifNode.apply(ctx);
        assertFalse(ctx.getSql().contains("AND name ="));
    }

    @Test
    void testMixedSqlNode() {
        MixedSqlNode mixed = new MixedSqlNode(Arrays.asList(
            new StaticTextSqlNode("SELECT * FROM user WHERE 1=1"),
            new StaticTextSqlNode("AND name = #{name}")
        ));
        mixed.apply(context);
        assertTrue(context.getSql().contains("SELECT * FROM user"));
        assertTrue(context.getSql().contains("AND name = #{name}"));
    }

    @Test
    void testTrimSqlNodePrefix() {
        DynamicContext ctx = new DynamicContext(configuration, new HashMap<>());
        TrimSqlNode trim = new TrimSqlNode(configuration,
            new StaticTextSqlNode("name = #{name}"), "WHERE", (String) null, (String) null, (String) null);
        trim.apply(ctx);
        assertTrue(ctx.getSql().contains("WHERE"));
        assertTrue(ctx.getSql().contains("name = #{name}"));
    }

    @Test
    void testTrimSqlNodePrefixOverrides() {
        DynamicContext ctx = new DynamicContext(configuration, new HashMap<>());
        TrimSqlNode trim = new TrimSqlNode(configuration,
            new StaticTextSqlNode("AND name = #{name}"), "WHERE", "AND", (String) null, (String) null);
        trim.apply(ctx);
        String sql = ctx.getSql();
        assertTrue(sql.contains("WHERE"));
        assertFalse(sql.contains("AND WHERE"));
    }

    @Test
    void testDynamicSqlSourceGetBoundSql() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "test");
        params.put("age", 25);

        MixedSqlNode root = new MixedSqlNode(Arrays.asList(
            new StaticTextSqlNode("SELECT * FROM user WHERE 1=1"),
            new IfSqlNode(new StaticTextSqlNode("AND name = #{name}"), "name != null"),
            new IfSqlNode(new StaticTextSqlNode("AND age = #{age}"), "age != null")
        ));

        DynamicSqlSource dynamicSqlSource = new DynamicSqlSource(configuration, root);
        BoundSql boundSql = dynamicSqlSource.getBoundSql(params);
        String sql = boundSql.getSql();
        assertTrue(sql.contains("?"));
        assertTrue(boundSql.getParameterMappings().size() >= 2);
    }

    @Test
    void testDynamicSqlSourceWithNullParam() {
        MixedSqlNode root = new MixedSqlNode(Arrays.asList(
            new StaticTextSqlNode("SELECT * FROM user WHERE 1=1"),
            new IfSqlNode(new StaticTextSqlNode("AND name = #{name}"), "name != null")
        ));

        DynamicSqlSource dynamicSqlSource = new DynamicSqlSource(configuration, root);
        BoundSql boundSql = dynamicSqlSource.getBoundSql(null);
        assertNotNull(boundSql.getSql());
    }

    @Test
    void testTrimSqlNodeSuffixOverrides() {
        DynamicContext ctx = new DynamicContext(configuration, new HashMap<>());
        TrimSqlNode trim = new TrimSqlNode(configuration,
            new StaticTextSqlNode("name = #{name} AND"), null, null, "", "AND");
        trim.apply(ctx);
        String sql = ctx.getSql();
        assertFalse(sql.endsWith("AND"));
    }
}
