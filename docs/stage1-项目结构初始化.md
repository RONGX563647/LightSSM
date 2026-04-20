# 第1阶段：项目结构初始化

## 变动内容

### 1. 创建项目基础结构
- 创建单一pom.xml，整合所有依赖
- 创建标准Maven目录结构：
  - src/main/java - 源代码目录
  - src/main/resources - 资源文件目录
  - src/test/java - 测试代码目录
  - docs - 文档目录

### 2. 配置依赖管理
- Jakarta Servlet API 6.0.0
- MySQL Connector 8.0.33
- Jackson JSON 2.17.2
- Dom4j XML解析 2.1.3
- OGNL表达式 3.0.8
- Druid连接池 1.2.9
- Logback日志 1.2.11
- JUnit 5测试框架

### 3. 创建包结构
- com.lightframework.ioc - IoC容器模块
- com.lightframework.mvc - SpringMVC模块
- com.lightframework.orm - ORM模块
- com.lightframework.aop - AOP模块

## 设计说明

### 项目架构
采用单一项目结构（非多模块），便于简历展示轻量级框架设计。

### 包命名规范
使用`com.lightframework`作为根包名，清晰区分各模块职责。

### 依赖版本管理
统一在pom.xml的properties中管理版本号，便于升级维护。

## 文件清单
- pom.xml - Maven配置文件
- .gitignore - Git忽略文件配置
- 目录结构创建（空目录）

## 下一步计划
第2阶段将实现IoC容器核心功能，包括：
- @Component/@Autowired注解定义
- BeanFactory核心接口
- DefaultListableBeanFactory实现（三级缓存）
- ApplicationContext应用上下文