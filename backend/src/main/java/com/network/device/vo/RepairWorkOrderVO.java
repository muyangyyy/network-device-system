package com.network.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 维修工单视图对象。
 * 字段名与前端 api/repair.ts 的 RepairOrder 对齐：
 * acceptorId/acceptorName/acceptTime/createTime/updateTime/logs 等。
 */
@Data
public class RepairWorkOrderVO implements Serializable {

    private Long id;

    private String workOrderNo;

    private Long deviceId;

    private String deviceCode;

    private String deviceName;

    private Long groupId;

    private String groupName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime faultTime;

    private Long reporterId;

    private String reporterName;

    private String repairType;

    private String repairTypeName;

    private Long repairUserId;

    private String repairUserName;

    private String status;

    private String statusName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedTime;

    private Long repairDuration;

    private String faultDescription;

    private String repairSolution;

    private String repairResult;

    private String deviceRepairStatus;

    /** 验收人ID（对应实体 acceptanceUserId） */
    private Long acceptorId;

    /** 验收人姓名（对应实体 acceptanceUserName） */
    private String acceptorName;

    /** 验收时间（对应实体 acceptanceTime） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime acceptTime;

    /**
     * 退回原因。
     * 注意：数据库 repair_work_order 表没有对应列，当前恒为 null（前端已做 `|| '-'` 兜底）。
     * 如需真实展示，需给该表增加 reject_reason 列。
     */
    private String rejectReason;

    /**
     * 维修开始时间 / 维修结束时间。
     * 注意：数据库 repair_work_order 表没有对应列，当前恒为 null（前端已做 `|| '-'` 兜底）。
     * 如需真实展示，需给该表增加 repair_start_time / repair_end_time 列。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime repairStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime repairEndTime;

    private Integer overdue;

    private String priority;

    private String priorityName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planCompleteTime;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private RepairHardwareDetailVO hardwareDetail;

    private RepairDebugDetailVO debugDetail;

    private RepairOpticalDetailVO opticalDetail;

    /** 工单附件 */
    private List<AttachmentVO> attachments;

    /** 状态流转日志（前端 detail.vue 直接读 order.logs） */
    private List<OrderLogVO> logs;
}
