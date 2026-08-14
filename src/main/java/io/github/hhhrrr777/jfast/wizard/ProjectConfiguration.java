package io.github.hhhrrr777.jfast.wizard;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 问题树装配后的最终工程配置,可直接转为 Freemarker 渲染模型。
 *
 * @param preset      预设名
 * @param groupId     Maven groupId
 * @param artifactId  Maven artifactId
 * @param basePackage Java 基础包名
 * @param packagePath 包路径(basePackage 的点分段转斜杠)
 * @param projectName 工程显示名(从 artifactId 推导)
 * @param description 工程描述
 * @param jdkVersion  JDK 版本
 * @param database    数据库类型
 * @param dbHost      数据库主机
 * @param dbPort      数据库端口
 * @param dbName      数据库名
 * @param dbUser      数据库用户名
 * @param dbPassword  数据库密码
 * @param serverPort  后端服务端口
 * @param outputDir   输出目录
 * @param conditions  模板条件命名空间 conditions.*
 */
public record ProjectConfiguration(String preset,
                                   String groupId,
                                   String artifactId,
                                   String basePackage,
                                   String packagePath,
                                   String projectName,
                                   String description,
                                   String jdkVersion,
                                   String database,
                                   String dbHost,
                                   String dbPort,
                                   String dbName,
                                   String dbUser,
                                   String dbPassword,
                                   String serverPort,
                                   String outputDir,
                                   Map<String, Object> conditions) {

    /**
     * 转为 Freemarker 根模型:project.* 命名空间 + conditions.* 命名空间。
     */
    public Map<String, Object> toRenderModel() {
        Map<String, Object> model = new LinkedHashMap<>();
        Map<String, Object> project = new LinkedHashMap<>();
        project.put("preset", preset);
        project.put("groupId", groupId);
        project.put("artifactId", artifactId);
        project.put("basePackage", basePackage);
        project.put("packagePath", packagePath);
        project.put("projectName", projectName);
        project.put("description", description);
        project.put("jdkVersion", jdkVersion);
        project.put("database", database);
        project.put("dbHost", dbHost);
        project.put("dbPort", dbPort);
        project.put("dbName", dbName);
        project.put("dbUser", dbUser);
        project.put("dbPassword", dbPassword);
        project.put("serverPort", serverPort);
        model.put("project", project);
        model.put("conditions", conditions);
        return model;
    }
}
