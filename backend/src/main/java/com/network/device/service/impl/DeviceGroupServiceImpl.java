package com.network.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.network.device.common.BusinessException;
import com.network.device.common.ResultCode;
import com.network.device.dto.DeviceGroupDTO;
import com.network.device.entity.DeviceGroup;
import com.network.device.entity.NetworkDevice;
import com.network.device.mapper.DeviceGroupMapper;
import com.network.device.mapper.NetworkDeviceMapper;
import com.network.device.service.DeviceGroupService;
import com.network.device.vo.DeviceGroupVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class DeviceGroupServiceImpl implements DeviceGroupService {

    private final DeviceGroupMapper deviceGroupMapper;
    private final NetworkDeviceMapper networkDeviceMapper;

    public DeviceGroupServiceImpl(DeviceGroupMapper deviceGroupMapper, NetworkDeviceMapper networkDeviceMapper) {
        this.deviceGroupMapper = deviceGroupMapper;
        this.networkDeviceMapper = networkDeviceMapper;
    }

    @Override
    public List<DeviceGroupVO> getGroupTree() {
        List<DeviceGroup> allGroups = deviceGroupMapper.selectList(
                new LambdaQueryWrapper<DeviceGroup>()
                        .eq(DeviceGroup::getStatus, 1)
                        .orderByAsc(DeviceGroup::getSortOrder));

        return buildGroupTree(allGroups, 0L);
    }

    private List<DeviceGroupVO> buildGroupTree(List<DeviceGroup> groups, Long parentId) {
        List<DeviceGroupVO> tree = new ArrayList<>();
        for (DeviceGroup group : groups) {
            // parent_id 允许为 NULL（历史数据），直接 .equals 会 NPE，这里按顶级分组(0)处理
            Long groupParentId = group.getParentId() == null ? 0L : group.getParentId();
            if (groupParentId.equals(parentId)) {
                DeviceGroupVO vo = new DeviceGroupVO();
                vo.setId(group.getId());
                vo.setParentId(group.getParentId());
                vo.setName(group.getGroupName());
                vo.setCode(group.getGroupCode());
                vo.setGroupType(group.getGroupType());
                vo.setManagerId(group.getManagerUserId());
                vo.setDescription(group.getDescription());
                vo.setSort(group.getSortOrder());
                vo.setStatus(group.getStatus());

                List<DeviceGroupVO> children = buildGroupTree(groups, group.getId());
                vo.setChildren(children);

                tree.add(vo);
            }
        }
        return tree;
    }

    @Override
    public DeviceGroup getGroupById(Long id) {
        DeviceGroup group = deviceGroupMapper.selectById(id);
        if (group == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST);
        }
        return group;
    }

    @Override
    @Transactional
    public DeviceGroup createGroup(DeviceGroupDTO dto) {
        if (StringUtils.hasText(dto.getCode())) {
            DeviceGroup existing = deviceGroupMapper.selectByGroupCode(dto.getCode());
            if (existing != null) {
                throw new BusinessException(ResultCode.DATA_EXIST.getCode(), "分组编码已存在");
            }
        }

        // 父分组存在性校验：不校验会创建出「孤儿分组」，树里永远不可见
        if (dto.getParentId() != null && dto.getParentId() != 0L) {
            requireGroupExists(dto.getParentId());
        }

        DeviceGroup group = new DeviceGroup();
        group.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        group.setGroupName(dto.getName());
        group.setGroupCode(dto.getCode());
        group.setGroupType(dto.getGroupType());
        // 防御 0：0=「清空」哨兵（仅编辑场景），创建时 0 不是合法 id，按未选择(NULL)处理
        group.setManagerUserId(dto.getManagerId() != null && dto.getManagerId() != 0L ? dto.getManagerId() : null);
        group.setDescription(dto.getDescription());
        group.setSortOrder(dto.getSort() != null ? dto.getSort() : 0);
        group.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        deviceGroupMapper.insert(group);

        return group;
    }

    @Override
    @Transactional
    public DeviceGroup updateGroup(Long id, DeviceGroupDTO dto) {
        DeviceGroup group = getGroupById(id);

        if (StringUtils.hasText(dto.getName())) {
            group.setGroupName(dto.getName());
        }
        if (dto.getParentId() != null) {
            // 环校验：把分组挂到自己或自己的后代下面，会让 getGroupTree 的递归
            // 无限循环直到 StackOverflowError（整个请求线程被打爆）。
            validateNoCycle(id, dto.getParentId());
            group.setParentId(dto.getParentId());
        }
        if (StringUtils.hasText(dto.getCode())) {
            group.setGroupCode(dto.getCode());
        }
        if (StringUtils.hasText(dto.getGroupType())) {
            group.setGroupType(dto.getGroupType());
        }
        // 0 是前端「清空」哨兵：el-select 清空后值是 undefined，JSON 序列化会省略字段，
        // 后端无法区分「没传」与「清空」；用户 id 自增从 1 开始，0 不是合法 id。
        // 置 NULL 必须走 UpdateWrapper——updateById 的 NOT_NULL 策略会跳过 null 字段。
        boolean clearManager = dto.getManagerId() != null && dto.getManagerId() == 0L;
        if (dto.getManagerId() != null && !clearManager) {
            group.setManagerUserId(dto.getManagerId());
        }
        if (StringUtils.hasText(dto.getDescription())) {
            group.setDescription(dto.getDescription());
        }
        if (dto.getSort() != null) {
            group.setSortOrder(dto.getSort());
        }
        if (dto.getStatus() != null) {
            group.setStatus(dto.getStatus());
        }

        deviceGroupMapper.updateById(group);
        if (clearManager) {
            deviceGroupMapper.update(null, new LambdaUpdateWrapper<DeviceGroup>()
                    .eq(DeviceGroup::getId, id)
                    .set(DeviceGroup::getManagerUserId, null));
        }
        return group;
    }

    /**
     * 校验把 id 的父分组改为 parentId 不会成环，且父分组真实存在。
     *
     * <p>成环判定：沿 parentId 的祖先链（parentId → parent.parentId → ...）向上走，
     * 若走到 id 自己，说明 id 是 parentId 的祖先，挂上去后链路会绕回 id。
     * visited 集合兜底历史数据本身已存在环的情况，避免校验时死循环。
     */
    private void validateNoCycle(Long id, Long parentId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (id.equals(parentId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "父分组不能是自己");
        }
        java.util.Set<Long> visited = new java.util.HashSet<>();
        Long cursor = parentId;
        while (cursor != null && cursor != 0L && visited.add(cursor)) {
            if (cursor.equals(id)) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "父分组不能是自己的子分组");
            }
            DeviceGroup parent = deviceGroupMapper.selectById(cursor);
            if (parent == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "父分组不存在");
            }
            cursor = parent.getParentId();
        }
    }

    private void requireGroupExists(Long groupId) {
        if (deviceGroupMapper.selectById(groupId) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "父分组不存在");
        }
    }

    @Override
    @Transactional
    public void deleteGroup(Long id) {
        getGroupById(id);

        Long subGroupCount = deviceGroupMapper.countByParentId(id);
        if (subGroupCount > 0) {
            throw new BusinessException(ResultCode.STATUS_ERROR.getCode(), "该分组下有子分组，无法删除");
        }

        Long deviceCount = deviceGroupMapper.countDevicesByGroupId(id);
        if (deviceCount > 0) {
            throw new BusinessException(ResultCode.STATUS_ERROR.getCode(), "该分组下有设备，无法删除");
        }

        Long orderCount = deviceGroupMapper.countOrdersByGroupId(id);
        if (orderCount > 0) {
            throw new BusinessException(ResultCode.STATUS_ERROR.getCode(), "该分组下有工单，无法删除");
        }

        deviceGroupMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void updateManager(Long id, Long managerUserId) {
        DeviceGroup group = getGroupById(id);
        group.setManagerUserId(managerUserId);
        deviceGroupMapper.updateById(group);
    }

    @Override
    @Transactional
    public void migrateDevices(List<Long> deviceIds, Long targetGroupId) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return;
        }
        if (targetGroupId != null) {
            getGroupById(targetGroupId);
        }

        for (Long deviceId : deviceIds) {
            NetworkDevice device = networkDeviceMapper.selectById(deviceId);
            if (device != null) {
                device.setGroupId(targetGroupId);
                networkDeviceMapper.updateById(device);
            }
        }
    }
}
