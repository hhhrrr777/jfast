package io.github.hhhrrr777.jfast.core;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 渲染接缝的 Freemarker 实现(ADR-0007),配置钉死:
 * UTF-8、未定义变量报错(严格模式)、异常带模板名 + 行号、关闭自动 HTML 转义。
 * 实现零 IO——模板源串由调用方传入,本类不触碰文件系统。
 */
public final class FreemarkerTemplateEngine implements TemplateEngine {

    private final Configuration configuration;

    public FreemarkerTemplateEngine() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_34);
        cfg.setDefaultEncoding(StandardCharsets.UTF_8.name());
        // 严格模式:未定义变量直接报错(classic_compatible 默认即 false,显式钉死)
        cfg.setClassicCompatible(false);
        // 异常直接抛出,Freemarker 异常信息自带模板名 + 行号;关闭异常日志避免污染 CLI 输出
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        // 不设置 output_format:保持 undefined,即不做任何自动转义(关闭自动 HTML 转义)
        this.configuration = cfg;
    }

    @Override
    public String render(String templateSource, String templateName, Map<String, Object> model) {
        try {
            Template template = new Template(templateName, new StringReader(templateSource), configuration);
            StringWriter out = new StringWriter();
            template.process(model, out);
            return out.toString();
        } catch (TemplateException | IOException e) {
            throw new TemplateRenderException(e.getMessage(), e);
        }
    }
}
