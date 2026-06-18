package com.lightframework.orm.parsing;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GenericTokenParserTest {

    @Test
    void testSimpleToken() {
        GenericTokenParser parser = new GenericTokenParser("#{", "}", content -> "?");
        String result = parser.parse("SELECT * FROM user WHERE id = #{id}");
        assertEquals("SELECT * FROM user WHERE id = ?", result);
    }

    @Test
    void testMultipleTokens() {
        GenericTokenParser parser = new GenericTokenParser("#{", "}", content -> "?");
        String result = parser.parse("INSERT INTO user(name, age) VALUES(#{name}, #{age})");
        assertEquals("INSERT INTO user(name, age) VALUES(?, ?)", result);
    }

    @Test
    void testDollarToken() {
        Map<String, String> vars = new HashMap<>();
        vars.put("table", "users");
        vars.put("col", "id");
        GenericTokenParser parser = new GenericTokenParser("${", "}", content -> vars.getOrDefault(content, content));
        String result = parser.parse("SELECT * FROM ${table} WHERE ${col} = 1");
        assertEquals("SELECT * FROM users WHERE id = 1", result);
    }

    @Test
    void testMixedTokens() {
        GenericTokenParser hashParser = new GenericTokenParser("#{", "}", content -> "?");
        String afterHash = hashParser.parse("SELECT * FROM user WHERE #{id} = #{value}");
        assertEquals("SELECT * FROM user WHERE ? = ?", afterHash);
    }

    @Test
    void testEscapedToken() {
        GenericTokenParser parser = new GenericTokenParser("#{", "}", content -> "?");
        String result = parser.parse("SELECT \\#{id} FROM user");
        assertEquals("SELECT #{id} FROM user", result);
    }

    @Test
    void testNoToken() {
        GenericTokenParser parser = new GenericTokenParser("#{", "}", content -> "?");
        String result = parser.parse("SELECT 1");
        assertEquals("SELECT 1", result);
    }

    @Test
    void testNullText() {
        GenericTokenParser parser = new GenericTokenParser("#{", "}", content -> "?");
        String result = parser.parse(null);
        assertEquals("", result);
    }

    @Test
    void testEmptyText() {
        GenericTokenParser parser = new GenericTokenParser("#{", "}", content -> "?");
        String result = parser.parse("");
        assertEquals("", result);
    }

    @Test
    void testUnclosedToken() {
        GenericTokenParser parser = new GenericTokenParser("#{", "}", content -> "?");
        String result = parser.parse("SELECT * FROM user WHERE id = #{id");
        assertEquals("SELECT * FROM user WHERE id = #{id", result);
    }

    @Test
    void testTokenHandlerTransformation() {
        GenericTokenParser parser = new GenericTokenParser("${", "}", content -> content.toUpperCase());
        String result = parser.parse("${hello} ${world}");
        assertEquals("HELLO WORLD", result);
    }

    @Test
    void testAdjacentTokens() {
        GenericTokenParser parser = new GenericTokenParser("#{", "}", content -> "?");
        String result = parser.parse("#{a}#{b}");
        assertEquals("??", result);
    }
}
