package com.govai.enterprise.controller;

import com.govai.enterprise.common.LoginUser;
import com.govai.enterprise.common.LoginUserContext;
import com.govai.enterprise.common.PageResult;
import com.govai.enterprise.common.Result;
import com.govai.enterprise.dto.EvaluationSubmitReq;
import com.govai.enterprise.dto.EvaluationVO;
import com.govai.enterprise.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 好差评接口
 */
@Tag(name = "好差评")
@RestController
@RequestMapping("/api/v1/ent-eval")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @Operation(summary = "提交好差评")
    @PostMapping
    public Result<EvaluationVO> submit(@Valid @RequestBody EvaluationSubmitReq req) {
        return Result.success(evaluationService.submit(LoginUserContext.require(), req));
    }

    @Operation(summary = "查询某申报记录的评价(未评价返回 null)")
    @GetMapping("/record/{recordId}")
    public Result<EvaluationVO> getByRecord(@PathVariable Long recordId) {
        return Result.success(evaluationService.getByRecord(LoginUserContext.require(), recordId));
    }

    @Operation(summary = "我的评价列表")
    @GetMapping("/list")
    public Result<PageResult<EvaluationVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(evaluationService.pageMine(LoginUserContext.require(), page, size));
    }
}
