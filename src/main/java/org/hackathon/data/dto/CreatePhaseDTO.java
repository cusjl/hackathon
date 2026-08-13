package org.hackathon.data.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreatePhaseDTO {
    @NotBlank(message = "名称不能为空")
    private String name;
    @NotNull(message = "提交开始时间不能为空")
    private LocalDateTime submitBeg;
    @NotNull(message = "提交结束时间不能为空")
    private LocalDateTime submitEnd;
    @NotNull(message = "评审开始时间不能为空")
    private LocalDateTime reviewBeg;
    @NotNull(message = "评审结束时间不能为空")
    private LocalDateTime reviewEnd;
    private Boolean blindReview = false;
    private Boolean midCheck = false;
    private Boolean manualPick = false;
    @DecimalMax(value = "1.0", message = "通过率不能大于1")
    @DecimalMin(value = "0.0", message = "通过率不能小于0")
    private BigDecimal passRate;
    private Boolean poll = false;
}
