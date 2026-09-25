package com.network.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.network.device.common.BusinessException;
import com.network.device.common.ResultCode;
import com.network.device.entity.SysDepartment;
import com.network.device.entity.SysUser;
import com.network.device.mapper.SysDepartmentMapper;
import com.network.device.mapper.SysUserMapper;
import com.network.device.service.SysDepartmentService;
import com.network.device.vo.DeviceGroupVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SysDepartmentServiceImpl implements SysDepartmentService {

    private final SysDepartmentMapper sysDepartmentMapper;
    private final SysUserMapper sysUserMapper;

    public SysDepartmentServiceImpl(SysDepartmentMapper sysDepartmentMapper, SysUserMapper sysUserMapper) {
        this.sysDepartmentMapper = sysDepartmentMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public List<DeviceGroupVO> getDepartmentTree() {
        List<SysDepartment> allDepts = sysDepartmentMapper.selectList(
                new LambdaQueryWrapper<SysDepartment>()
                        .eq(SysDepartment::getStatus, 1)
                        .orderByAsc(SysDepartment::getSort));

        return buildDeptTree(allDepts, 0L);
    }

    private List<DeviceGroupVO> buildDeptTree(List<SysDepartment> departments, Long parentId) {
        List<DeviceGroupVO> tree = new ArrayList<>();
        for (SysDepartment dept : departments) {
            // parent_id 允许为 NULL（手工导入的历史数据），直接 .equals 会 NPE，这里按顶级部门(0)处理
            Long deptParentId = dept.getParentId() == null ? 0L : dept.getParentId();
            if (deptParentId.equals(parentId)) {
                DeviceGroupVO vo = new DeviceGroupVO();
                vo.setId(dept.getId());
                vo.setParentId(dept.getParentId());
                vo.setName(dept.getDeptName());
                vo.setSort(dept.getSort());
                vo.setManagerId(dept.getLeaderId());
                vo.setStatus(dept.getStatus());
                vo.setDescription(dept.getRemark());

                List<DeviceGroupVO> children = buildDeptTree(departments, dept.getId());
                vo.setChildren(children);

                tree.add(vo);
            }
        }
        return tree;
    }

    @Override
    public SysDepartment getDepartmentById(Long id) {
        SysDepartment dept = sysDepartmentMapper.selectByIdOnly(id);
        if (dept == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST);
        }
        return dept;
    }

    @Override
    @Transactional
    public SysDepartment createDepartment(SysDepartment department) {
        // 父部门存在性校验：不校验会创建出「孤儿部门」，树里永远不可见（与分组服务同规则）
        if (department.getParentId() == null) {
            department.setParentId(0L);
        }
        if (department.getParentId() != 0L) {
            requireDeptExists(department.getParentId());
        }
        sysDepartmentMapper.insert(department);
        return department;
    }

    @Override
    @Transactional
    public SysDepartment updateDepartment(Long id, SysDepartment department) {
        getDepartmentById(id);
        if (department.getParentId() != null) {
            // 环校验：把部门挂到自己或自己的后代下面，会让该子树从 getDepartmentTree
            // 的结果里整体消失（环上节点不可达自顶级，递归永远展开不到它们）。
            validateNoCycle(id, department.getParentId());
        }
        department.setId(id);
        sysDepartmentMapper.updateById(department);
        return department;
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        getDepartmentById(id);

        // 子部门检查：直接删除会让子部门的 parentId 悬空，整棵子部门树从树上消失
        Long subDeptCount = sysDepartmentMapper.selectCount(
                new LambdaQueryWrapper<SysDepartment>().eq(SysDepartment::getParentId, id));
        if (subDeptCount != null && subDeptCount > 0) {
            throw new BusinessException(ResultCode.STATUS_ERROR.getCode(), "该部门下有子部门，无法删除");
        }

        // 在用检查：删除后用户的 departmentId 悬空，用户档案里的部门显示为空
        Long userCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getDepartmentId, id));
        if (userCount != null && userCount > 0) {
            throw new BusinessException(ResultCode.STATUS_ERROR.getCode(), "该部门下仍有用户，无法删除");
        }

        sysDepartmentMapper.deleteById(id);
    }

    /**
     * 校验把 id 的父部门改为 parentId 不会成环，且父部门真实存在。
     *
     * <p>成环判定：沿 parentId 的祖先链向上走，若走到 id 自己，
     * 说明 id 是 parentId 的祖先，挂上去后链路会绕回 id。
     * visited 集合兜底历史数据本身已存在环的情况，避免校验时死循环。
     */
    private void validateNoCycle(Long id, Long parentId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (id.equals(parentId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "上级部门不能是自己");
        }
        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null && cursor != 0L && visited.add(cursor)) {
            if (cursor.equals(id)) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "上级部门不能是自己的子部门");
            }
            SysDepartment parent = sysDepartmentMapper.selectById(cursor);
            if (parent == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "上级部门不存在");
            }
            cursor = parent.getParentId();
        }
    }

    private void requireDeptExists(Long parentId) {
        if (sysDepartmentMapper.selectById(parentId) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "上级部门不存在");
        }
    }
}
