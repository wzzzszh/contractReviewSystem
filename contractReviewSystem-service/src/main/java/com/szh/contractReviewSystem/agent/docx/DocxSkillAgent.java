package com.szh.contractReviewSystem.agent.docx;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface DocxSkillAgent {

    @UserMessage("{{task}}")
    String execute(@V("task") String task);
}
