# PROTOTYPE —— Java CLI 生成器最小验证(扔掉的验证物)

对应票据:[原型:Java CLI 生成器最小验证](https://github.com/hhhrrr777/jfast/issues/5)。
**这不是正式代码**,只用于给「拍板:生成器自身技术栈」(#6)提供可触摸的讨论对象。

## 验证的三个问题

1. **交互手感** —— picocli(参数解析)+ JLine 4.x `jline-prompt`(Inquirer.js 风格向导)
2. **模板组织方式** —— 本原型实现「目录即模板」:`src/main/resources/templates/` 镜像目标工程目录结构,walk 一遍逐个落地
3. **打包分发** —— maven-shade 单 jar;native 本机无 GraalVM,未实测(调研结论:Rocker 路线可兑现,见下方结论)

## 跑起来

```bash
cd prototype/java-cli-minimal
mvn -q package
java -jar target/jfast-proto-0.0.1.jar          # 交互向导
```

非交互(脚本/CI 验证用,全部参数给齐就不进向导):

```bash
java -jar target/jfast-proto-0.0.1.jar \
  --group-id com.example --artifact-id demo --preset full --with-hello -o /tmp
```

已知刻意简化(原型不追求正确性):preset=empty + withHello=true 时 pom 带 web starter 但不生成 Controller;模板无条件覆盖已有文件。
