package com.network.device.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AssignDTO implements Serializable {

    @NotNull(message = "维修人ID不能为空")
    private Long repairUserId;

    private LocalDateTime planCompleteTime;

    private String priority;

    private String remark;
}
