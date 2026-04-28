# 合同审查系统服务端能力提升路线

本文从实际企业 Java 项目练习角度，整理合同审查系统后续可补充的功能点。目标不是单纯堆功能，而是让项目逐步具备企业后端常见的安全性、可维护性、可追踪性、可扩展性和上线意识。

## 一、当前项目基础

当前项目已经具备一些较好的服务端基础：

- 用户表和基础用户接口
- JWT 登录令牌能力
- 文件上传、下载和文件记录
- MyBatis 数据访问
- DOCX 合同修改流程
- 基于 `.trae/skills/legal-contract-risk` 的合同风险提示流程
- AI 生成修改要求和 DOCX 补丁方案

这些能力已经能支撑一个合同审查 Demo。下一步应重点补齐企业项目中更常见的工程化能力。

## 二、认证与权限体系

### 建议补充

- 注册、登录、退出登录
- Access Token + Refresh Token
- 密码加密存储
- 用户角色和权限
- 接口级权限控制

### 实现思路

数据库层面可以补充：

```text
sys_user
sys_role
sys_permission
sys_user_role
sys_role_permission
```

密码不要明文存储。可以优先使用 BCrypt，也可以在练习阶段先使用加盐 Hash。

登录成功后返回：

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer"
}
```

接口权限可以通过注解控制：

```java
@RequiresPermissions("file:upload")
@PostMapping("/upload")
public Result<?> upload(...) {
    ...
}
```

拦截器负责解析 Token，AOP 或权限组件负责判断用户是否具备接口权限。

### 练习价值

这部分可以帮助理解“认证”和“授权”的区别：

- 认证：你是谁
- 授权：你能做什么

## 三、统一异常、错误码和返回规范

### 建议补充

- 统一业务错误码
- 参数错误统一返回
- Token 失效返回 401
- 权限不足返回 403
- AI 调用失败、文件不存在、文件格式错误等业务异常分类

### 实现思路

完善 `BusinessExceptionEnum`，例如：

```text
400 参数错误
401 未登录或 Token 失效
403 无权限
404 资源不存在
500 系统错误
10001 用户不存在
10002 密码错误
20001 文件不存在
30001 AI 服务调用失败
```

业务层不要直接返回错误字符串，应抛出统一异常：

```java
throw new CustomException(ErrorCode.FILE_NOT_FOUND);
```

由 `GlobalExceptionHandler` 统一转换为前端响应。

### 练习价值

企业项目中，接口不只是“能返回数据”，还要让前端和调用方明确知道失败原因和处理方式。

## 四、文件生命周期管理

### 建议补充

- 文件上传记录
- 文件状态
- 原文件和修改后文件关联
- 文件软删除
- 文件下载权限校验
- 文件大小和类型限制
- 临时文件定期清理

### 实现思路

可以扩展文件表：

```text
file_storage
- id
- user_id
- file_name
- file_path
- file_category
- file_status
- source_file_id
- file_size
- content_type
- deleted
- create_time
- update_time
```

上传时保存记录。修改 DOCX 后生成新文件记录，并关联 `source_file_id`。

下载文件前必须校验：

```text
当前登录用户 ID == 文件所属用户 ID
```

临时目录例如 `docx-agent-work`，可以通过定时任务按时间清理。

### 练习价值

文件系统是企业后端常见风险点。文件权限、存储路径、临时文件清理、下载校验都很贴近真实业务。

## 五、合同审查任务异步化

### 建议补充

当前 AI 审查和 DOCX 修改属于长耗时操作，不适合所有场景都使用同步 HTTP 请求等待结果。建议改造成任务模式：

- 提交审查任务
- 后端立即返回 `taskId`
- 后台异步执行
- 前端轮询任务状态
- 完成后查看报告和下载结果文件

### 表设计建议

```text
review_task
- id
- user_id
- source_file_id
- result_file_id
- task_type
- status
- progress
- risk_report
- generated_requirement
- error_message
- create_time
- update_time
```

`status` 可以设计为：

```text
pending
running
success
failed
```

### 接口建议

```text
POST /api/review/tasks
GET  /api/review/tasks/{id}
GET  /api/review/tasks
```

### 实现方式

练习阶段可以先用：

```java
@Async
```

并配置线程池。后续可以升级为 Redis 队列或 RabbitMQ。

### 练习价值

异步任务是企业系统非常常见的设计。它能训练你理解任务状态、失败重试、进度查询、结果持久化等服务端能力。

## 六、审查记录和操作日志

### 建议补充

- 登录日志
- 文件上传日志
- 文件下载日志
- 合同审查日志
- DOCX 修改日志
- 接口耗时记录

### 表设计建议

```text
operation_log
- id
- user_id
- operation_type
- operation_desc
- request_uri
- request_method
- ip
- elapsed_ms
- result
- error_message
- create_time
```

### 实现思路

使用 AOP 拦截 Controller 方法，记录：

- 当前用户 ID
- 请求路径
- 请求方法
- 执行耗时
- 成功或失败
- 异常信息

注意密码、Token、API Key 等敏感字段要脱敏。

### 练习价值

企业系统需要能排查问题、追踪行为和定位责任。操作日志是后端工程化的重要能力。

## 七、参数校验和接口文档

### 建议补充

- 所有请求 DTO 使用校验注解
- 文件大小、文件类型、路径参数统一校验
- Swagger 或 Knife4j 接口文档
- 请求示例和响应示例

### 实现思路

DTO 中使用：

```java
@NotBlank
@NotNull
@Size
@Pattern
```

例如：

```java
@NotBlank(message = "用户名不能为空")
@Size(max = 64, message = "用户名长度不能超过64")
private String username;
```

接口文档可以使用 Knife4j，方便前后端协作和调试。

### 练习价值

接口文档和参数校验可以让项目边界更清晰，减少脏数据进入业务层。

## 八、配置安全和环境隔离

### 建议补充

- 拆分开发、测试、生产配置
- 数据库密码、AI Key 不提交到代码仓库
- 使用环境变量注入敏感配置

### 配置建议

```text
application.yml
application-dev.yml
application-test.yml
application-prod.yml
```

敏感配置使用环境变量：

```yaml
spring:
  datasource:
    password: ${DB_PASSWORD:}

ark:
  apiKey: ${ARK_API_KEY:}
```

### 练习价值

真实企业项目中，密钥不能写死在代码或配置文件里。配置隔离是上线意识的重要部分。

## 九、AI 调用稳定性设计

### 建议补充

- AI 请求超时
- AI 调用失败重试
- 失败降级
- AI 调用日志
- Prompt 版本管理
- 调用耗时和成本统计

### 表设计建议

```text
ai_call_log
- id
- user_id
- task_id
- model
- prompt_version
- status
- elapsed_ms
- error_message
- create_time
```

Prompt 建议放到资源文件中：

```text
resources/prompts/
- risk-review-v1.md
- docx-modify-v1.md
```

后续可以考虑数据库管理 Prompt 版本。

### 练习价值

AI 功能不是只要能调用成功就够了。企业项目还要考虑失败、超时、成本、版本回溯和结果可解释性。

## 十、数据库设计规范化

### 建议补充

- 所有表统一基础字段
- 唯一索引
- 状态字段枚举化
- 分页查询
- 条件查询
- 软删除
- 乐观锁

### 通用字段建议

```text
id
create_time
update_time
deleted
version
```

文件列表、任务列表、日志列表都建议做分页：

```text
GET /api/files?pageNum=1&pageSize=10
GET /api/review/tasks?pageNum=1&pageSize=10
```

### 练习价值

数据库设计决定系统后续维护成本。分页、索引、状态字段、软删除都是企业项目高频能力。

## 十一、测试体系

### 建议补充

- Service 单元测试
- Controller 集成测试
- 登录接口测试
- Token 拦截测试
- 文件上传下载测试
- 合同审查任务测试

### 技术选择

- JUnit 5
- SpringBootTest
- MockMvc
- H2 测试库或独立测试库

### 关键测试场景

```text
登录成功
登录失败
无 Token 被拦截
Token 过期被拦截
上传文件成功
下载他人文件失败
提交审查任务成功
审查任务失败后记录错误信息
```

### 练习价值

测试不是为了形式上的覆盖率，而是保护关键业务流程不被后续改动破坏。

## 十二、推荐实施优先级

### 第一阶段：企业基础盘

优先补齐安全和接口基础：

1. 密码加密
2. JWT 拦截完善
3. 文件权限校验
4. 统一异常错误码
5. DTO 参数校验

### 第二阶段：业务工程化

围绕合同审查核心业务做工程化：

1. 审查任务异步化
2. 审查记录表
3. 操作日志
4. 文件生命周期管理
5. AI 调用日志

### 第三阶段：上线意识

补齐部署和可维护能力：

1. 环境配置拆分
2. Swagger / Knife4j
3. 日志脱敏
4. 临时文件定时清理
5. 基础测试体系

## 十三、最推荐优先做的四个点

如果只选四个最值得深入的方向，建议优先做：

1. 合同审查任务异步化
2. 文件权限校验
3. AI 调用日志
4. 审查记录表

这四个点加上后，项目会从功能 Demo 更明显地变成一个企业后端系统。

