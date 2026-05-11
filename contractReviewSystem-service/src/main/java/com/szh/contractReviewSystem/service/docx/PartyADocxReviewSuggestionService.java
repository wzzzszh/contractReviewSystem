package com.szh.contractReviewSystem.service.docx;

import com.szh.contractReviewSystem.agent.docx.DocxSkillAgentProperties;
import com.szh.contractReviewSystem.agent.docx.model.DocxModifyPerspective;
import com.szh.contractReviewSystem.llm.LLMService;
import org.springframework.stereotype.Service;

@Service
public class PartyADocxReviewSuggestionService extends AbstractDocxReviewSuggestionService {

    public PartyADocxReviewSuggestionService(DocxSkillAgentProperties properties,
                                             LLMService llmService,
                                             LegalContractRiskReviewService legalContractRiskReviewService) {
        super(properties, llmService, legalContractRiskReviewService);
    }

    @Override
    public DocxModifyPerspective getPerspective() {
        return DocxModifyPerspective.PARTY_A;
    }

    @Override
    protected String buildPerspectiveInstruction() {
        return """
                优先处理“对甲方不利”部分的风险，尤其是限制甲方权利、削弱甲方付款/验收控制、
                加重甲方交付、责任、保密、知识产权或争议解决负担的条款。
                对“对乙方不利”部分，只在其可能导致条款无效、履约障碍、谈判失败或整体争议风险时，
                转化为更平衡、更可执行的修改要求。
                """;
    }
}
