package com.network.device.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class RejectDTO implements Serializable {

    /** 退回原因。前端发送的是 rejectReason，用 @JsonAlias 兼容 */
    @NotBlank(message = "退回原因不能为空")
    @JsonAlias({"rejectReason"})
    private String reason;
}
