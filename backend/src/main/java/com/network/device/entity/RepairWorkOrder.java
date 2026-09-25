package com.network.device.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("repair_work_order")
public class RepairWorkOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String workOrderNo;

    private Long deviceId;

    private String deviceCode;

    private String deviceName;

    private Long groupId;

    private LocalDateTime faultTime;

    private Long reporterId;

    private String repairType;

    private Long repairUserId;

    private String status;

    private LocalDateTime completedTime;

    private Long repairDuration;

    private String faultDescription;

    private String repairSolution;

    private String repairResult;

    private String deviceRepairStatus;

    private Long acceptanceUserId;

    private LocalDateTime acceptanceTime;

    private Integer overdue;

    private String priority;

    private LocalDateTime planCompleteTime;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
