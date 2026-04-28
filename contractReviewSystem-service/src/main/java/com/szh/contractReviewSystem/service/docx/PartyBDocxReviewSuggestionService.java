package com.szh.contractReviewSystem.service.docx;

import com.szh.contractReviewSystem.agent.docx.DocxSkillAgentProperties;
import com.szh.contractReviewSystem.agent.docx.model.DocxModifyPerspective;
import com.szh.contractReviewSystem.config.ArkConfig;
import org.springframework.stereotype.Service;

@Service
public class PartyBDocxReviewSuggestionService extends AbstractDocxReviewSuggestionService {

    public PartyBDocxReviewSuggestionService(DocxSkillAgentProperties properties,
                                             ArkConfig arkConfig,
                                             LegalContractRiskReviewService legalContractRiskReviewService) {
        super(properties, arkConfig, legalContractRiskReviewService);
    }

    @Override
    public DocxModifyPerspective getPerspective() {
        return DocxModifyPerspective.PARTY_B;
    }

    @Override
    protected String buildPerspectiveInstruction() {
        return """
                优先处理“对乙方不利”部分的风险，尤其是扩大乙方义务边界、削弱收款保护、
                造成验收或变更管理不可执行、施加无限违约/赔偿/保密/知识产权/争议解决风险的条款。
                对“对甲方不利”部分，只在其可能导致条款无效、履约障碍、谈判失败或整体争议风险时，
                转化为更平衡、更可执行的修改要求。
                """;
    }
}
