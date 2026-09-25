package com.network.device.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 验收工单请求体。
 *
 * <p>前端 accept.vue 的「验收备注」是可选字段，但后端 accept 接口原先没有 {@code @RequestBody}，
 * 用户填写的备注会被静默丢弃（接口 200，日志里永远是写死的「验收通过」）。
 * 这里补上承载字段；备注为空时保持原行为。
 */
@Data
public class AcceptDTO implements Serializable {

    /** 验收备注（可选） */
    private String remark;
}
