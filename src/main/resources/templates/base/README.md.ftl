# ${project.projectName}

> 由 jfast 生成(预设:${project.preset})。

## 工程信息

- GroupId: ${project.groupId}
- ArtifactId: ${project.artifactId}
- 基础包名: ${project.basePackage}
- JDK: ${project.jdkVersion}
- 数据库: ${project.database}
<#if conditions.systemAdmin>
- 数据库主机: ${project.dbHost}:${project.dbPort}
- 数据库名: ${project.dbName}
- 服务端口: ${project.serverPort}
</#if>
