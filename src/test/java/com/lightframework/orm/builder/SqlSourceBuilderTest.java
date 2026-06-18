package com.lightframework.orm.builder;

import com.lightframework.orm.mapping.BoundSql;
import com.lightframework.orm.mapping.ParameterMapping;
import com.lightframework.orm.mapping.SqlSource;
import com.lightframework.orm.session.Configuration;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SqlSourceBuilderTest {

    private final Configuration configuration = new Configuration();

    static class User {
        private Integer id;
        private String name;
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @Test
    void testSimpleParameter() {
        SqlSourceBuilder builder = new SqlSourceBuilder(configuration);
        SqlSource sqlSource = builder.parse("SELECT * FROM user WHERE id = #{id}", User.class, new HashMap<>());
        BoundSql boundSql = sqlSource.getBoundSql(new User());
        assertEquals("SELECT * FROM user WHERE id = ?", boundSql.getSql());
        List<ParameterMapping> mappings = boundSql.getParameterMappings();
        assertEquals(1, mappings.size());
        assertEquals("id", mappings.get(0).getProperty());
    }

    @Test
    void testMultipleParameters() {
        SqlSourceBuilder builder = new SqlSourceBuilder(configuration);
        SqlSource sqlSource = builder.parse(
            "INSERT INTO user(name, age) VALUES(#{name}, #{age})", User.class, new HashMap<>());
        BoundSql boundSql = sqlSource.getBoundSql(new User());
        assertEquals("INSERT INTO user(name, age) VALUES(?, ?)", boundSql.getSql());
        assertEquals(2, boundSql.getParameterMappings().size());
    }

    @Test
    void testNoParameters() {
        SqlSourceBuilder builder = new SqlSourceBuilder(configuration);
        SqlSource sqlSource = builder.parse("SELECT 1", User.class, new HashMap<>());
        BoundSql boundSql = sqlSource.getBoundSql(new User());
        assertEquals("SELECT 1", boundSql.getSql());
        assertEquals(0, boundSql.getParameterMappings().size());
    }

    @Test
    void testParameterWithJdbcType() {
        SqlSourceBuilder builder = new SqlSourceBuilder(configuration);
        SqlSource sqlSource = builder.parse(
            "SELECT * FROM user WHERE id = #{id:INTEGER}", User.class, new HashMap<>());
        BoundSql boundSql = sqlSource.getBoundSql(new User());
        assertEquals("SELECT * FROM user WHERE id = ?", boundSql.getSql());
        assertEquals(1, boundSql.getParameterMappings().size());
        assertEquals("id", boundSql.getParameterMappings().get(0).getProperty());
    }
}
