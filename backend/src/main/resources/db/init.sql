-- 关键：docker-entrypoint 导入本文件时，mysql 容器内 LANG 未设置会导致客户端
-- 字符集回落 latin1，UTF-8 中文被二次编码入库（表现为页面全站中文乱码）。
-- SET NAMES 强制本会话按 utf8mb4 收发，与下方建库字符集一致。
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS network_device DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE network_device;

-- ----------------------------
-- sys_user
-- ----------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码',
    real_name VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    avatar VARCHAR(256) DEFAULT NULL COMMENT '头像',
    department_id BIGINT DEFAULT NULL COMMENT '部门ID',
    status TINYINT DEFAULT 1 COMMENT '状态(0-禁用 1-启用)',
    last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_department_id (department_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- ----------------------------
-- sys_role
-- ----------------------------
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    role_key VARCHAR(64) NOT NULL COMMENT '角色标识(ADMIN/OPERATOR/READONLY)',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态(0-禁用 1-启用)',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_key (role_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- ----------------------------
-- sys_permission
-- ----------------------------
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    perm_name VARCHAR(64) NOT NULL COMMENT '权限名称',
    perm_key VARCHAR(128) NOT NULL COMMENT '权限标识',
    parent_id BIGINT DEFAULT 0 COMMENT '父级ID',
    type TINYINT DEFAULT 0 COMMENT '类型(0-目录 1-菜单 2-按钮)',
    path VARCHAR(256) DEFAULT NULL COMMENT '路由路径',
    component VARCHAR(256) DEFAULT NULL COMMENT '组件路径',
    icon VARCHAR(64) DEFAULT NULL COMMENT '图标',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态(0-禁用 1-启用)',
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- ----------------------------
-- sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ----------------------------
-- sys_role_permission
-- ----------------------------
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    perm_id BIGINT NOT NULL COMMENT '权限ID',
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_perm (role_id, perm_id),
    KEY idx_perm_id (perm_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ----------------------------
-- sys_department
-- ----------------------------
DROP TABLE IF EXISTS sys_department;
CREATE TABLE sys_department (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dept_name VARCHAR(64) NOT NULL COMMENT '部门名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父级ID',
    sort INT DEFAULT 0 COMMENT '排序',
    leader_id BIGINT DEFAULT NULL COMMENT '负责人ID',
    status TINYINT DEFAULT 1 COMMENT '状态(0-禁用 1-启用)',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- ----------------------------
-- sys_dict
-- ----------------------------
DROP TABLE IF EXISTS sys_dict;
CREATE TABLE sys_dict (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dict_name VARCHAR(128) NOT NULL COMMENT '字典名称',
    dict_type VARCHAR(128) NOT NULL COMMENT '字典类型',
    status TINYINT DEFAULT 1 COMMENT '状态(0-禁用 1-启用)',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典表';

-- ----------------------------
-- sys_dict_item
-- ----------------------------
DROP TABLE IF EXISTS sys_dict_item;
CREATE TABLE sys_dict_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dict_id BIGINT NOT NULL COMMENT '字典ID',
    dict_type VARCHAR(128) NOT NULL COMMENT '字典类型',
    item_label VARCHAR(128) NOT NULL COMMENT '字典项标签',
    item_value VARCHAR(128) NOT NULL COMMENT '字典项值',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态(0-禁用 1-启用)',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_dict_type (dict_type),
    KEY idx_dict_id (dict_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典项表';

-- ----------------------------
-- device_group
-- ----------------------------
DROP TABLE IF EXISTS device_group;
CREATE TABLE device_group (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parent_id BIGINT DEFAULT 0 COMMENT '父级ID',
    group_name VARCHAR(128) NOT NULL COMMENT '分组名称',
    group_code VARCHAR(64) DEFAULT NULL COMMENT '分组编码',
    group_type VARCHAR(32) DEFAULT NULL COMMENT '分组类型',
    manager_user_id BIGINT DEFAULT NULL COMMENT '管理员用户ID',
    description VARCHAR(512) DEFAULT NULL COMMENT '描述',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态(0-禁用 1-启用)',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id),
    KEY idx_group_code (group_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备分组表';

-- ----------------------------
-- network_device
-- ----------------------------
DROP TABLE IF EXISTS network_device;
CREATE TABLE network_device (
    id BIGINT NOT NULL AUTO_INCREMENT,
    device_code VARCHAR(64) NOT NULL COMMENT '设备编号',
    device_name VARCHAR(128) NOT NULL COMMENT '设备名称',
    device_type VARCHAR(64) DEFAULT NULL COMMENT '设备类型',
    brand VARCHAR(64) DEFAULT NULL COMMENT '品牌',
    model VARCHAR(64) DEFAULT NULL COMMENT '型号',
    serial_number VARCHAR(128) DEFAULT NULL COMMENT '序列号',
    purchase_date DATE DEFAULT NULL COMMENT '采购日期',
    warranty_expire_date DATE DEFAULT NULL COMMENT '保修到期日期',
    installation_location VARCHAR(256) DEFAULT NULL COMMENT '安装位置',
    ip_address VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
    mac_address VARCHAR(64) DEFAULT NULL COMMENT 'MAC地址',
    status VARCHAR(32) DEFAULT 'NORMAL' COMMENT '状态(NORMAL/FAULT_REPAIR/IDLE/SCRAPPED)',
    group_id BIGINT DEFAULT NULL COMMENT '分组ID',
    responsible_user_id BIGINT DEFAULT NULL COMMENT '负责人ID',
    supplier VARCHAR(128) DEFAULT NULL COMMENT '供应商',
    department VARCHAR(128) DEFAULT NULL COMMENT '所属部门',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_code (device_code),
    KEY idx_device_type (device_type),
    KEY idx_status (status),
    KEY idx_group_id (group_id),
    KEY idx_ip_address (ip_address),
    KEY idx_serial_number (serial_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网络设备表';

-- ----------------------------
-- device_status_log
-- ----------------------------
DROP TABLE IF EXISTS device_status_log;
CREATE TABLE device_status_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    device_id BIGINT NOT NULL COMMENT '设备ID',
    device_code VARCHAR(64) DEFAULT NULL COMMENT '设备编号',
    original_status VARCHAR(32) DEFAULT NULL COMMENT '原状态',
    new_status VARCHAR(32) DEFAULT NULL COMMENT '新状态',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    operate_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_device_id (device_id),
    KEY idx_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备状态变更日志表';

-- ----------------------------
-- repair_work_order
-- ----------------------------
DROP TABLE IF EXISTS repair_work_order;
CREATE TABLE repair_work_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    work_order_no VARCHAR(64) NOT NULL COMMENT '工单编号',
    device_id BIGINT DEFAULT NULL COMMENT '设备ID',
    device_code VARCHAR(64) DEFAULT NULL COMMENT '设备编号',
    device_name VARCHAR(128) DEFAULT NULL COMMENT '设备名称',
    group_id BIGINT DEFAULT NULL COMMENT '设备分组ID',
    fault_time DATETIME DEFAULT NULL COMMENT '故障时间',
    reporter_id BIGINT DEFAULT NULL COMMENT '报修人ID',
    repair_type VARCHAR(32) DEFAULT NULL COMMENT '维修类型(HARDWARE/DEBUG/OPTICAL)',
    repair_user_id BIGINT DEFAULT NULL COMMENT '维修人ID',
    status VARCHAR(32) DEFAULT 'DRAFT' COMMENT '状态',
    completed_time DATETIME DEFAULT NULL COMMENT '完成时间',
    repair_duration BIGINT DEFAULT NULL COMMENT '维修耗时(分钟)',
    fault_description TEXT COMMENT '故障描述',
    repair_solution TEXT COMMENT '维修方案',
    repair_result TEXT COMMENT '维修结果',
    device_repair_status VARCHAR(32) DEFAULT NULL COMMENT '设备维修后状态',
    acceptance_user_id BIGINT DEFAULT NULL COMMENT '验收人ID',
    acceptance_time DATETIME DEFAULT NULL COMMENT '验收时间',
    overdue TINYINT DEFAULT 0 COMMENT '是否超时(0-否 1-是)',
    priority VARCHAR(16) DEFAULT 'MEDIUM' COMMENT '优先级(LOW/MEDIUM/HIGH/URGENT)',
    plan_complete_time DATETIME DEFAULT NULL COMMENT '计划完成时间',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_order_no (work_order_no),
    KEY idx_device_id (device_id),
    KEY idx_device_code (device_code),
    KEY idx_repair_user_id (repair_user_id),
    KEY idx_reporter_id (reporter_id),
    KEY idx_status (status),
    KEY idx_repair_type (repair_type),
    KEY idx_fault_time (fault_time),
    KEY idx_group_id (group_id),
    KEY idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='维修工单表';

-- ----------------------------
-- repair_hardware_detail
-- ----------------------------
DROP TABLE IF EXISTS repair_hardware_detail;
CREATE TABLE repair_hardware_detail (
    id BIGINT NOT NULL AUTO_INCREMENT,
    work_order_id BIGINT NOT NULL COMMENT '工单ID',
    damaged_component VARCHAR(128) DEFAULT NULL COMMENT '损坏部件',
    replacement_part_name VARCHAR(128) DEFAULT NULL COMMENT '更换配件名称',
    replacement_part_model VARCHAR(128) DEFAULT NULL COMMENT '更换配件型号',
    replacement_part_quantity INT DEFAULT 0 COMMENT '更换配件数量',
    replacement_part_cost DECIMAL(10,2) DEFAULT 0 COMMENT '更换配件费用',
    old_part_disposal_method VARCHAR(64) DEFAULT NULL COMMENT '旧件处理方式',
    hardware_failure_code VARCHAR(64) DEFAULT NULL COMMENT '硬件故障代码',
    whether_under_warranty TINYINT DEFAULT 0 COMMENT '是否在保(0-否 1-是)',
    supplier VARCHAR(128) DEFAULT NULL COMMENT '供应商',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='维修硬件明细表';

-- ----------------------------
-- repair_debug_detail
-- ----------------------------
DROP TABLE IF EXISTS repair_debug_detail;
CREATE TABLE repair_debug_detail (
    id BIGINT NOT NULL AUTO_INCREMENT,
    work_order_id BIGINT NOT NULL COMMENT '工单ID',
    configuration_change_content TEXT COMMENT '配置变更内容',
    old_firmware_version VARCHAR(64) DEFAULT NULL COMMENT '旧固件版本',
    new_firmware_version VARCHAR(64) DEFAULT NULL COMMENT '新固件版本',
    old_ip_address VARCHAR(64) DEFAULT NULL COMMENT '旧IP地址',
    new_ip_address VARCHAR(64) DEFAULT NULL COMMENT '新IP地址',
    old_gateway VARCHAR(64) DEFAULT NULL COMMENT '旧网关',
    new_gateway VARCHAR(64) DEFAULT NULL COMMENT '新网关',
    old_vlan VARCHAR(64) DEFAULT NULL COMMENT '旧VLAN',
    new_vlan VARCHAR(64) DEFAULT NULL COMMENT '新VLAN',
    route_change_content TEXT COMMENT '路由变更内容',
    firewall_policy_change TEXT COMMENT '防火墙策略变更',
    permission_change_content TEXT COMMENT '权限变更内容',
    network_parameter_change_record TEXT COMMENT '网络参数变更记录',
    rollback_plan TEXT COMMENT '回退方案',
    test_result VARCHAR(256) DEFAULT NULL COMMENT '测试结果',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='维修调试明细表';

-- ----------------------------
-- repair_optical_detail
-- ----------------------------
DROP TABLE IF EXISTS repair_optical_detail;
CREATE TABLE repair_optical_detail (
    id BIGINT NOT NULL AUTO_INCREMENT,
    work_order_id BIGINT NOT NULL COMMENT '工单ID',
    fault_optical_point VARCHAR(128) DEFAULT NULL COMMENT '故障光点',
    optical_route_name VARCHAR(128) DEFAULT NULL COMMENT '光路名称',
    cable_section VARCHAR(128) DEFAULT NULL COMMENT '线缆段',
    cable_length DECIMAL(10,2) DEFAULT NULL COMMENT '线缆长度(米)',
    optical_power_before DECIMAL(10,4) DEFAULT NULL COMMENT '维修前光功率(dBm)',
    optical_power_after DECIMAL(10,4) DEFAULT NULL COMMENT '维修后光功率(dBm)',
    attenuation_before DECIMAL(10,4) DEFAULT NULL COMMENT '维修前衰减(dB)',
    attenuation_after DECIMAL(10,4) DEFAULT NULL COMMENT '维修后衰减(dB)',
    wavelength VARCHAR(32) DEFAULT NULL COMMENT '波长',
    splitter_status VARCHAR(64) DEFAULT NULL COMMENT '分光器状态',
    jumper_status VARCHAR(64) DEFAULT NULL COMMENT '跳纤状态',
    cable_damage_description TEXT COMMENT '线缆损坏描述',
    link_test_result VARCHAR(256) DEFAULT NULL COMMENT '链路测试结果',
    test_tool VARCHAR(128) DEFAULT NULL COMMENT '测试工具',
    test_person VARCHAR(64) DEFAULT NULL COMMENT '测试人员',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='光路维修明细表';

-- ----------------------------
-- repair_status_log
-- ----------------------------
DROP TABLE IF EXISTS repair_status_log;
CREATE TABLE repair_status_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    work_order_id BIGINT NOT NULL COMMENT '工单ID',
    work_order_no VARCHAR(64) DEFAULT NULL COMMENT '工单编号',
    original_status VARCHAR(32) DEFAULT NULL COMMENT '原状态',
    new_status VARCHAR(32) DEFAULT NULL COMMENT '新状态',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作人姓名',
    operate_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_work_order_id (work_order_id),
    KEY idx_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单状态变更日志表';

-- ----------------------------
-- repair_attachment
-- ----------------------------
DROP TABLE IF EXISTS repair_attachment;
CREATE TABLE repair_attachment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    work_order_id BIGINT NOT NULL COMMENT '工单ID',
    file_name VARCHAR(256) NOT NULL COMMENT '文件名',
    file_path VARCHAR(512) NOT NULL COMMENT '文件路径',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    file_type VARCHAR(32) DEFAULT NULL COMMENT '文件类型',
    upload_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    uploader_id BIGINT DEFAULT NULL COMMENT '上传人ID',
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单附件表';

-- ----------------------------
-- operation_log
-- ----------------------------
DROP TABLE IF EXISTS operation_log;
CREATE TABLE operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    module VARCHAR(64) DEFAULT NULL COMMENT '操作模块',
    operation VARCHAR(128) DEFAULT NULL COMMENT '操作描述',
    method VARCHAR(256) DEFAULT NULL COMMENT '请求方法',
    request_url VARCHAR(512) DEFAULT NULL COMMENT '请求URL',
    request_method VARCHAR(16) DEFAULT NULL COMMENT 'HTTP方法',
    request_params TEXT COMMENT '请求参数',
    response_result TEXT COMMENT '响应结果',
    status TINYINT DEFAULT 1 COMMENT '状态(0-失败 1-成功)',
    error_msg TEXT COMMENT '错误信息',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作人姓名',
    operator_ip VARCHAR(64) DEFAULT NULL COMMENT '操作人IP',
    operate_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_operator_id (operator_id),
    KEY idx_operate_time (operate_time),
    KEY idx_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ----------------------------
-- login_log
-- ----------------------------
DROP TABLE IF EXISTS login_log;
CREATE TABLE login_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) DEFAULT NULL COMMENT '用户名',
    login_ip VARCHAR(64) DEFAULT NULL COMMENT '登录IP',
    login_location VARCHAR(256) DEFAULT NULL COMMENT '登录地点',
    browser VARCHAR(128) DEFAULT NULL COMMENT '浏览器',
    os VARCHAR(128) DEFAULT NULL COMMENT '操作系统',
    status TINYINT DEFAULT 1 COMMENT '状态(0-失败 1-成功)',
    msg VARCHAR(512) DEFAULT NULL COMMENT '消息',
    login_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_username (username),
    KEY idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- ----------------------------
-- notification_message
-- ----------------------------
DROP TABLE IF EXISTS notification_message;
CREATE TABLE notification_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(256) NOT NULL COMMENT '标题',
    content TEXT COMMENT '内容',
    sender_id BIGINT DEFAULT NULL COMMENT '发送人ID',
    receiver_id BIGINT DEFAULT NULL COMMENT '接收人ID',
    type VARCHAR(32) DEFAULT NULL COMMENT '类型',
    related_id BIGINT DEFAULT NULL COMMENT '关联ID',
    related_type VARCHAR(32) DEFAULT NULL COMMENT '关联类型',
    read_status TINYINT DEFAULT 0 COMMENT '已读状态(0-未读 1-已读)',
    read_time DATETIME DEFAULT NULL COMMENT '阅读时间',
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(64) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_receiver_id (receiver_id),
    KEY idx_read_status (read_status),
    KEY idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知消息表';

-- ----------------------------
-- 初始数据
-- ----------------------------

-- 默认部门
INSERT INTO sys_department (id, dept_name, parent_id, sort, status, created_by) VALUES
(1, '总公司', 0, 0, 1, 'admin'),
(2, '运维部', 1, 1, 1, 'admin'),
(3, '网络部', 1, 2, 1, 'admin'),
(4, '技术支持部', 1, 3, 1, 'admin');

-- 默认角色
INSERT INTO sys_role (id, role_name, role_key, sort, status, created_by) VALUES
(1, '超级管理员', 'ADMIN', 0, 1, 'admin'),
(2, '运维操作员', 'OPERATOR', 1, 1, 'admin'),
(3, '只读用户', 'READONLY', 2, 1, 'admin');

-- 默认用户（密码已用 BCrypt 加密，明文见注释）
-- admin    / admin123
-- operator / operator123
INSERT INTO sys_user (id, username, password, real_name, phone, email, department_id, status, created_by) VALUES
(1, 'admin', '$2a$10$efM/B6E9xd6zOMW7GU7SzOAxVvLStAAR5y6SyDdD10u4LF4YENxvS', '系统管理员', '13800138000', 'admin@network.com', 1, 1, 'admin'),
(2, 'operator', '$2a$10$3LrV/IMfCpU5wTl62oUy2eC14S.h5AoJDU11Fa1U5K5hm0DnMWBHi', '运维操作员', '13800138001', 'operator@network.com', 2, 1, 'admin');

-- 用户角色关联
INSERT INTO sys_user_role (user_id, role_id, created_by) VALUES
(1, 1, 'admin'),
(2, 2, 'admin');

-- 默认权限
-- 注意：perm_key 必须与前端 frontend/src/router/index.ts 的 meta.permission
-- 以及各页面 userStore.hasPermission('xxx') 的取值完全一致，否则侧边栏菜单会被隐藏、
-- 按钮不会渲染（layout/components/Sidebar.vue 会按 meta.permission 过滤菜单）。
INSERT INTO sys_permission (id, perm_name, perm_key, parent_id, type, path, component, icon, sort, status, created_by) VALUES
-- 一级目录
(1, '系统管理', 'system', 0, 0, '/system', NULL, 'Setting', 0, 1, 'admin'),
(2, '设备管理', 'device', 0, 0, '/device', NULL, 'Monitor', 1, 1, 'admin'),
(3, '运维管理', 'repair', 0, 0, '/repair', NULL, 'Tools', 2, 1, 'admin'),
(4, '统计分析', 'statistics', 0, 0, '/statistics', NULL, 'DataAnalysis', 3, 1, 'admin'),
-- 系统管理子菜单
(10, '用户管理', 'system:user', 1, 1, 'user', 'system/user/index', 'User', 0, 1, 'admin'),
(11, '角色管理', 'system:role', 1, 1, 'role', 'system/role/index', 'UserFilled', 1, 1, 'admin'),
(12, '权限管理', 'system:permission', 1, 1, 'permission', 'system/permission/index', 'Lock', 2, 1, 'admin'),
(13, '部门管理', 'system:department', 1, 1, 'department', 'system/department/index', 'OfficeBuilding', 3, 1, 'admin'),
(14, '字典管理', 'system:dict', 1, 1, 'dict', 'system/dict/index', 'Notebook', 4, 1, 'admin'),
(15, '操作日志', 'system:log', 1, 1, 'operation-log', 'system/operation-log/index', 'Document', 5, 1, 'admin'),
(16, '登录日志', 'system:loginLog', 1, 1, 'login-log', 'system/login-log/index', 'Tickets', 6, 1, 'admin'),
-- 设备管理子菜单
(20, '设备列表', 'device:list', 2, 1, 'list', 'device/list/index', 'List', 0, 1, 'admin'),
(21, '设备分组', 'group:list', 2, 1, 'group', 'device/group/index', 'Folder', 1, 1, 'admin'),
(22, '设备导入导出', 'device:importExport', 2, 1, 'import-export', NULL, 'Upload', 2, 1, 'admin'),
-- 设备管理按钮级权限
(23, '设备新增', 'device:add', 2, 2, NULL, NULL, NULL, 3, 1, 'admin'),
(24, '设备编辑', 'device:edit', 2, 2, NULL, NULL, NULL, 4, 1, 'admin'),
(25, '设备删除', 'device:delete', 2, 2, NULL, NULL, NULL, 5, 1, 'admin'),
(26, '设备详情', 'device:view', 2, 2, NULL, NULL, NULL, 6, 1, 'admin'),
(27, '设备导入', 'device:import', 2, 2, NULL, NULL, NULL, 7, 1, 'admin'),
(28, '设备导出', 'device:export', 2, 2, NULL, NULL, NULL, 8, 1, 'admin'),
-- 运维管理子菜单
(30, '工单列表', 'repair:list', 3, 1, 'list', 'repair/list/index', 'List', 0, 1, 'admin'),
(31, '工单创建', 'repair:add', 3, 2, NULL, NULL, NULL, 1, 1, 'admin'),
(32, '工单分配', 'repair:assign', 3, 2, NULL, NULL, NULL, 2, 1, 'admin'),
(33, '工单验收', 'repair:accept', 3, 2, NULL, NULL, NULL, 3, 1, 'admin'),
(34, '工单详情', 'repair:view', 3, 2, NULL, NULL, NULL, 4, 1, 'admin'),
(35, '工单处理', 'repair:process', 3, 2, NULL, NULL, NULL, 5, 1, 'admin'),
(36, '工单导出', 'repair:export', 3, 2, NULL, NULL, NULL, 6, 1, 'admin'),
(37, '工单删除', 'repair:delete', 3, 2, NULL, NULL, NULL, 7, 1, 'admin'),
-- 统计分析子菜单
(40, '数据看板', 'statistics:view', 4, 1, 'dashboard', 'statistics/dashboard/index', 'PieChart', 0, 1, 'admin'),
(41, '统计报表', 'statistics:report', 4, 1, 'report', 'statistics/report/index', 'BarChart', 1, 1, 'admin');

-- 角色权限关联
-- ADMIN：全部权限
INSERT INTO sys_role_permission (role_id, perm_id, created_by) VALUES
(1, 1, 'admin'), (1, 2, 'admin'), (1, 3, 'admin'), (1, 4, 'admin'),
(1, 10, 'admin'), (1, 11, 'admin'), (1, 12, 'admin'), (1, 13, 'admin'), (1, 14, 'admin'), (1, 15, 'admin'), (1, 16, 'admin'),
(1, 20, 'admin'), (1, 21, 'admin'), (1, 22, 'admin'),
(1, 23, 'admin'), (1, 24, 'admin'), (1, 25, 'admin'), (1, 26, 'admin'), (1, 27, 'admin'), (1, 28, 'admin'),
(1, 30, 'admin'), (1, 31, 'admin'), (1, 32, 'admin'), (1, 33, 'admin'), (1, 34, 'admin'), (1, 35, 'admin'), (1, 36, 'admin'), (1, 37, 'admin'),
(1, 40, 'admin'), (1, 41, 'admin');

-- OPERATOR：设备查看/编辑/导入导出 + 分组 + 工单全流程（不含删除与验收）+ 统计
INSERT INTO sys_role_permission (role_id, perm_id, created_by) VALUES
(2, 2, 'admin'), (2, 3, 'admin'), (2, 4, 'admin'),
(2, 20, 'admin'), (2, 21, 'admin'), (2, 22, 'admin'),
(2, 24, 'admin'), (2, 26, 'admin'), (2, 27, 'admin'), (2, 28, 'admin'),
(2, 30, 'admin'), (2, 31, 'admin'), (2, 34, 'admin'), (2, 35, 'admin'), (2, 36, 'admin'),
(2, 40, 'admin');

-- READONLY：仅查看
-- 只读角色：必须能看到设备列表与工单列表（否则侧边栏仅剩「统计分析」，
-- 且 device:view/repair:view 形同虚设——能看详情却进不去列表页）
INSERT INTO sys_role_permission (role_id, perm_id, created_by) VALUES
(3, 4, 'admin'),
(3, 20, 'admin'),
(3, 21, 'admin'),
(3, 26, 'admin'),
(3, 30, 'admin'),
(3, 34, 'admin'),
(3, 40, 'admin');

-- 默认字典
INSERT INTO sys_dict (id, dict_name, dict_type, status, created_by) VALUES
(1, '设备类型', 'device_type', 1, 'admin'),
(2, '设备状态', 'device_status', 1, 'admin'),
(3, '维修类型', 'repair_type', 1, 'admin'),
(4, '工单状态', 'repair_status', 1, 'admin'),
(5, '优先级', 'order_priority', 1, 'admin'),
(6, '设备品牌', 'device_brand', 1, 'admin');

-- 字典项 - 设备类型
INSERT INTO sys_dict_item (dict_id, dict_type, item_label, item_value, sort, status, created_by) VALUES
(1, 'device_type', '路由器', 'ROUTER', 0, 1, 'admin'),
(1, 'device_type', '交换机', 'SWITCH', 1, 1, 'admin'),
(1, 'device_type', '防火墙', 'FIREWALL', 2, 1, 'admin'),
(1, 'device_type', '服务器', 'SERVER', 3, 1, 'admin'),
(1, 'device_type', '光猫', 'ONT', 4, 1, 'admin'),
(1, 'device_type', 'AP', 'AP', 5, 1, 'admin'),
(1, 'device_type', '其他', 'OTHER', 6, 1, 'admin');

-- 字典项 - 设备状态
INSERT INTO sys_dict_item (dict_id, dict_type, item_label, item_value, sort, status, created_by) VALUES
(2, 'device_status', '正常运行', 'NORMAL', 0, 1, 'admin'),
(2, 'device_status', '故障维修', 'FAULT_REPAIR', 1, 1, 'admin'),
(2, 'device_status', '闲置', 'IDLE', 2, 1, 'admin'),
(2, 'device_status', '报废', 'SCRAPPED', 3, 1, 'admin');

-- 字典项 - 维修类型
INSERT INTO sys_dict_item (dict_id, dict_type, item_label, item_value, sort, status, created_by) VALUES
(3, 'repair_type', '硬件维修', 'HARDWARE', 0, 1, 'admin'),
(3, 'repair_type', '调试维修', 'DEBUG', 1, 1, 'admin'),
(3, 'repair_type', '光路异常维修', 'OPTICAL', 2, 1, 'admin');

-- 字典项 - 工单状态
INSERT INTO sys_dict_item (dict_id, dict_type, item_label, item_value, sort, status, created_by) VALUES
(4, 'repair_status', '草稿', 'DRAFT', 0, 1, 'admin'),
(4, 'repair_status', '已提交', 'SUBMITTED', 1, 1, 'admin'),
(4, 'repair_status', '已分配', 'ASSIGNED', 2, 1, 'admin'),
(4, 'repair_status', '处理中', 'PROCESSING', 3, 1, 'admin'),
(4, 'repair_status', '已完成', 'COMPLETED', 4, 1, 'admin'),
(4, 'repair_status', '已验收', 'ACCEPTED', 5, 1, 'admin'),
(4, 'repair_status', '已退回', 'REJECTED', 6, 1, 'admin'),
(4, 'repair_status', '已延期', 'DELAYED', 7, 1, 'admin'),
(4, 'repair_status', '返修', 'REPAIR_AGAIN', 8, 1, 'admin'),
(4, 'repair_status', '已关闭', 'CLOSED', 9, 1, 'admin'),
(4, 'repair_status', '已取消', 'CANCELLED', 10, 1, 'admin');

-- 字典项 - 优先级
INSERT INTO sys_dict_item (dict_id, dict_type, item_label, item_value, sort, status, created_by) VALUES
(5, 'order_priority', '低', 'LOW', 0, 1, 'admin'),
(5, 'order_priority', '中', 'MEDIUM', 1, 1, 'admin'),
(5, 'order_priority', '高', 'HIGH', 2, 1, 'admin'),
(5, 'order_priority', '紧急', 'URGENT', 3, 1, 'admin');

-- 字典项 - 设备品牌
INSERT INTO sys_dict_item (dict_id, dict_type, item_label, item_value, sort, status, created_by) VALUES
(6, 'device_brand', '华为', 'HUAWEI', 0, 1, 'admin'),
(6, 'device_brand', '中兴', 'ZTE', 1, 1, 'admin'),
(6, 'device_brand', '思科', 'CISCO', 2, 1, 'admin'),
(6, 'device_brand', '锐捷', 'RUIJIE', 3, 1, 'admin'),
(6, 'device_brand', '华三', 'H3C', 4, 1, 'admin'),
(6, 'device_brand', '烽火', 'FIBERHOME', 5, 1, 'admin'),
(6, 'device_brand', '其他', 'OTHER', 6, 1, 'admin');
