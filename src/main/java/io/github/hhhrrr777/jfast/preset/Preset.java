package io.github.hhhrrr777.jfast.preset;

import java.util.List;
import java.util.Map;

/**
 * 工程预设元数据(ADR-0004)。
 *
 * @param name        预设标识(目录名)
 * @param displayName 向导显示名
 * @param description 描述
 * @param questions   问题白名单(question id)
 * @param conditions  模板条件变量命名空间 conditions.*
 */
public record Preset(String name,
                     String displayName,
                     String description,
                     List<String> questions,
                     Map<String, Object> conditions) {
}
