package io.github.hhhrrr777.jfast.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class FreemarkerTemplateEngineTest {

    private final TemplateEngine engine = new FreemarkerTemplateEngine();

    @Test
    void rendersSimpleVariable() {
        String result = engine.render(
                "package ${project.packageName};",
                "Main.java.ftl",
                Map.of("project", Map.of("packageName", "com.example.demo")));

        assertThat(result).isEqualTo("package com.example.demo;");
    }

    @Test
    void undefinedVariableFailsWithTemplateNameAndLineNumber() {
        String source = "第一行\npackage ${missing};";

        assertThatThrownBy(() -> engine.render(source, "Main.java.ftl", Map.of()))
                .isInstanceOf(TemplateRenderException.class)
                .hasMessageContaining("missing")
                .hasMessageContaining("Main.java.ftl")
                .hasMessageContaining("line 2");
    }

    @Test
    void rendersUtf8ChineseContent() {
        String result = engine.render("工程:${project.name}", "readme.ftl", Map.of("project", Map.of("name", "演示工程")));

        assertThat(result).isEqualTo("工程:演示工程");
    }

    @Test
    void doesNotHtmlEscapeOutput() {
        String result = engine.render("${project.tag}", "page.vue.ftl", Map.of("project", Map.of("tag", "<el-button>保存</el-button>")));

        assertThat(result).isEqualTo("<el-button>保存</el-button>");
    }
}
