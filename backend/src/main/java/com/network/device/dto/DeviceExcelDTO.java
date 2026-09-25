package com.network.device.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 设备 Excel 导入 / 导出模型。
 * 导入时按表头名称匹配，未提供的列留空。
 */
@Data
public class DeviceExcelDTO implements Serializable {

    @ExcelProperty("设备编码")
    private String deviceCode;

    @ExcelProperty("设备名称")
    private String deviceName;

    @ExcelProperty("设备类型")
    private String deviceType;

    @ExcelProperty("品牌")
    private String brand;

    @ExcelProperty("型号")
    private String model;

    @ExcelProperty("序列号")
    private String serialNumber;

    @ExcelProperty("采购日期")
    private String purchaseDate;

    @ExcelProperty("保修到期日期")
    private String warrantyExpireDate;

    @ExcelProperty("安装位置")
    private String installationLocation;

    @ExcelProperty("IP地址")
    private String ipAddress;

    @ExcelProperty("MAC地址")
    private String macAddress;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("所属分组")
    private String groupName;

    @ExcelProperty("负责人")
    private String responsibleUserName;

    @ExcelProperty("供应商")
    private String supplier;

    @ExcelProperty("所属部门")
    private String department;

    @ExcelProperty("备注")
    private String remark;
}
