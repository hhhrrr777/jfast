# jfast-baseline

> 定位:jfast 的 **baseline 基准应用**——以 `jfast new --preset empty` 的生成物为起点、
> 手工叠加完整后台功能(S2 阶段),作为后续模板化(`templates/presets/full/`)的底稿。
> 自带构建,**不进生成器 reactor**(ADR-0005)。

jfast-baseline 工程

## 工程信息

- GroupId: io.github.hhhrrr777
- ArtifactId: jfast-baseline
- 基础包名: io.github.hhhrrr777.jfast.baseline
- JDK: 21
- 数据库: mysql

## 技术栈

- Spring Boot 3.5.16
- MyBatis-Plus 3.5.17
- JDK 21
- 数据库: mysql
- 前端: Vue 3.5 + Vite 8 + TypeScript + Element Plus(Node ^20.19 或 >=22.12)

## 数据库准备(MySQL)

默认连接 `src/main/resources/application.yml` 中的 `jdbc:mysql://127.0.0.1:3306/jfast_baseline`。
首次启动前建库:

```sql
CREATE DATABASE IF NOT EXISTS jfast_baseline
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

连接账号/密码/端口如需修改,编辑 `application.yml` 的 `spring.datasource` 段即可。

## 快速开始

```bash
# 后端(8080)
mvn spring-boot:run
# 启动后可访问 OpenAPI: http://127.0.0.1:8080/v3/api-docs
# Swagger UI:            http://127.0.0.1:8080/swagger-ui.html

# 前端(另开终端)
npm install
npm run build   # 或 npm run dev 本地开发
```
