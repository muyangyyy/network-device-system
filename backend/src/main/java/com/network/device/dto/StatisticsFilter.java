package com.network.device.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统计查询的过滤条件（已归一化）。
 *
 * <p>由 {@link StatisticsQueryDTO} 转换而来。之所以要单独一个类，是因为 Mapper 里用
 * {@code <if test='f.xxx != null'>} 来判断「该条件是否生效」，而前端传过来的空串
 * （Element Plus 的 clearable 控件清空后常常是 {@code ''} 而不是 {@code null}）
 * 必须在这里统一收敛成 {@code null}，否则会出现 {@code AND device_type = ''} 这种
 * 「筛了个空值、结果全空」的情况。
 *
 * <p>时间边界统一归一化成 {@code yyyy-MM-dd HH:mm:ss} 字符串，并采用 <b>左闭右开</b>：
 * 只给到日期（{@code 2026-09-01}）时上界会补到次日零点，即
 * {@code created_at < 2026-09-02 00:00:00}，这样「结束日期」当天整天都会被包含。
 * 若写成 {@code created_at <= 2026-09-01 00:00:00}，结束当天的数据会被整体漏掉。
 */
@Data
public class StatisticsFilter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 起始时间（含），格式 yyyy-MM-dd HH:mm:ss */
    private String startTime;

    /** 结束时间（不含），格式 yyyy-MM-dd HH:mm:ss */
    private String endTime;

    /** 设备分组 ID（精确匹配，不递归子分组） */
    private Long groupId;

    /** 设备类型，取值见数据字典 device_type */
    private String deviceType;

    /** 维修类型，取值见数据字典 repair_type */
    private String repairType;

    /** 维修人 ID */
    private Long repairUserId;

    public static StatisticsFilter from(StatisticsQueryDTO queryDTO) {
        StatisticsFilter filter = new StatisticsFilter();
        if (queryDTO == null) {
            return filter;
        }
        filter.setStartTime(normalizeBoundary(queryDTO.getStartTime(), false));
        filter.setEndTime(normalizeBoundary(queryDTO.getEndTime(), true));
        filter.setGroupId(queryDTO.getGroupId());
        filter.setDeviceType(trimToNull(queryDTO.getDeviceType()));
        filter.setRepairType(trimToNull(queryDTO.getRepairType()));
        filter.setRepairUserId(queryDTO.getRepairUserId());
        return filter;
    }

    /**
     * 把前端传来的时间边界归一化成可比较的字符串。
     *
     * @param exclusive {@code true} 表示这是开区间上界：只给到日期时需要 +1 天，
     *                  否则「结束日期」当天的记录会被整体排除
     */
    private static String normalizeBoundary(String raw, boolean exclusive) {
        String value = trimToNull(raw);
        if (value == null) {
            return null;
        }
        LocalDateTime parsed;
        if (value.length() <= 10) {
            // 纯日期：2026-09-01（Element Plus 的 daterange + value-format="YYYY-MM-DD" 走这条）
            parsed = LocalDate.parse(value).atStartOfDay();
            if (exclusive) {
                parsed = parsed.plusDays(1);
            }
        } else {
            // 兼容 "yyyy-MM-dd HH:mm:ss" 与 ISO 的 "yyyy-MM-ddTHH:mm:ss"
            parsed = LocalDateTime.parse(value.replace(' ', 'T'));
        }
        return parsed.format(DATE_TIME_FORMATTER);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
