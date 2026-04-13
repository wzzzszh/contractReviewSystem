# DOCX Agent

这个项目现在已经预留了一套基于 Java 17 + LangChain4j Skills 的 `docx` agent 能力。

## 作用

- 读取本地 `docx` skill
- 让模型根据 `SKILL.md` 决定操作步骤
- 通过 shell 工具执行 skill 里的脚本
- 完成 `.doc/.docx` 的读取、拆包、修改、打包

## 关键类

- `com.szh.contractReviewSystem.agent.docx.DocxSkillAgentService`
- `com.szh.contractReviewSystem.agent.docx.DocxSkillAgentProperties`
- `com.szh.contractReviewSystem.agent.docx.DocxSkillAgent`

## 最小调用示例

```java
@Autowired
private DocxSkillAgentService docxSkillAgentService;

String result = docxSkillAgentService.modifyDocument(
        Path.of("C:/contracts/input.docx"),
        Path.of("C:/contracts/output-reviewed.docx"),
        "根据甲方立场，补强违约责任和通知送达条款，保留原格式"
);
```

## 配置

在 `application.yml` 里配置：

```yml
ark:
  apiKey: ${ARK_API_KEY:}
  model: ${ARK_MODEL:}

docx-agent:
  enabled: true
  baseUrl: https://ark.cn-beijing.volces.com/api/v3
  skillPath: ${user.home}/.codex/skills/docx
  shellWorkingDirectory: ${docx-agent.skillPath}
  pythonCommand: python
```

## 注意

- 这个 agent 依赖 Java 17。
- 如果 `python` 命令不可用，请把 `docx-agent.pythonCommand` 改成可用解释器，例如实际的 `python.exe` 绝对路径。
- `ShellSkills` 属于实验能力，更适合先在本地环境跑通，再决定是否用于生产环境。
