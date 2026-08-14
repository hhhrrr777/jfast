package io.github.hhhrrr777.jfast.core;

import java.util.Map;

/**
 * 渲染接缝(ADR-0007):纯字符串接口,引擎实现零 IO。
 * 遍历、剥后缀、文件名渲染、写盘归上层 FileTreeWalker。
 */
public interface TemplateEngine {

    /**
     * 渲染一份模板源串。
     *
     * @param templateSource 模板内容(整份,引擎不从任何地方读文件)
     * @param templateName   模板名,仅用于异常信息定位(带模板名 + 行号)
     * @param model          根模型,三命名空间 project.* / entity.* / conditions.*
     * @return 渲染结果
     * @throws TemplateRenderException 渲染失败(未定义变量、语法错误等),信息带模板名 + 行号
     */
    String render(String templateSource, String templateName, Map<String, Object> model);
}
