package com.szh.contractReviewSystem.controller.notdb;

import com.szh.contractReviewSystem.agent.docx.model.DocxModifyRequest;
import com.szh.contractReviewSystem.agent.docx.model.DocxModifyResponse;
import com.szh.contractReviewSystem.annotation.RequiresPermissions;
import com.szh.contractReviewSystem.common.Result;
import com.szh.contractReviewSystem.service.docx.DocxDocumentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/docx-agent")
public class DocxSkillAgentController extends BaseController {

    private final DocxDocumentService docxDocumentService;

    public DocxSkillAgentController(DocxDocumentService docxDocumentService) {
        this.docxDocumentService = docxDocumentService;
    }

    @RequiresPermissions("review:modify")
    @PostMapping("/modify")
    public Result<DocxModifyResponse> modifyDocument(@Valid @RequestBody DocxModifyRequest request) {
        return success("DOCX文档修改成功", docxDocumentService.modifyDocument(request));
    }
}
