package com.network.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class DelayDTO implements Serializable {

    @NotBlank(message = "延期原因不能为空")
    private String reason;

    private LocalDateTime planCompleteTime;
}
