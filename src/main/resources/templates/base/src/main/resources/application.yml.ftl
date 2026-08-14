server:
  port: ${project.serverPort}

spring:
  application:
    name: ${project.name}
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    driver-class-name: <#if project.database == "mysql">com.mysql.cj.jdbc.Driver<#elseif project.database == "postgresql">org.postgresql.Driver<#elseif project.database == "dm">dm.jdbc.driver.DmDriver<#elseif project.database == "kingbase">com.kingbase8.Driver<#elseif project.database == "opengauss">org.opengauss.Driver</#if>
    url: jdbc:<#if project.database == "mysql">mysql<#elseif project.database == "postgresql">postgresql<#elseif project.database == "dm">dm<#elseif project.database == "kingbase">kingbase8<#elseif project.database == "opengauss">opengauss</#if>://${project.dbHost}:${project.dbPort}/${project.dbName}
    username: ${project.dbUser}
    password: ${project.dbPassword}
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: assign_id
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  mapper-locations: classpath*:mapper/**/*.xml

springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs
