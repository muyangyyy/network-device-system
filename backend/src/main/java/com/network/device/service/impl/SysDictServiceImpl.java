package com.network.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.network.device.common.BusinessException;
import com.network.device.common.ResultCode;
import com.network.device.entity.SysDict;
import com.network.device.entity.SysDictItem;
import com.network.device.mapper.SysDictItemMapper;
import com.network.device.mapper.SysDictMapper;
import com.network.device.service.SysDictService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysDictServiceImpl implements SysDictService {

    private final SysDictMapper sysDictMapper;
    private final SysDictItemMapper sysDictItemMapper;

    public SysDictServiceImpl(SysDictMapper sysDictMapper, SysDictItemMapper sysDictItemMapper) {
        this.sysDictMapper = sysDictMapper;
        this.sysDictItemMapper = sysDictItemMapper;
    }

    @Override
    public Page<SysDict> listDicts(Integer page, Integer pageSize) {
        Page<SysDict> pageParam = new Page<>(page, pageSize);
        return sysDictMapper.selectPage(pageParam, new LambdaQueryWrapper<SysDict>()
                .orderByDesc(SysDict::getCreatedAt));
    }

    @Override
    public SysDict getDictById(Long id) {
        SysDict dict = sysDictMapper.selectById(id);
        if (dict == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST);
        }
        return dict;
    }

    @Override
    @Transactional
    public SysDict createDict(SysDict dict) {
        // 唯一性预检必须覆盖「已逻辑删除」的行：uk_dict_type 是普通唯一索引，不区分逻辑删除，
        // 被删除的字典在物理上仍占用该 dict_type。
        // 原先用 selectByDictType（带 deleted = 0）判断，删除字典后再建同类型会撞唯一键。
        Long exists = sysDictMapper.countByDictTypeIncludeDeleted(dict.getDictType());
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.DATA_EXIST.getCode(),
                    "字典类型已存在（可能属于已删除的字典，请更换类型标识）");
        }
        sysDictMapper.insert(dict);
        return dict;
    }

    @Override
    @Transactional
    public SysDict updateDict(Long id, SysDict dict) {
        getDictById(id);
        dict.setId(id);
        sysDictMapper.updateById(dict);
        return dict;
    }

    @Override
    @Transactional
    public void deleteDict(Long id) {
        getDictById(id);
        sysDictMapper.deleteById(id);
    }

    @Override
    public List<SysDictItem> listDictItemsByType(String dictType) {
        return sysDictItemMapper.selectByDictType(dictType);
    }

    @Override
    public SysDictItem getDictItemById(Long id) {
        SysDictItem item = sysDictItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST);
        }
        return item;
    }

    @Override
    @Transactional
    public SysDictItem createDictItem(String dictType, SysDictItem dictItem) {
        SysDict dict = sysDictMapper.selectByDictType(dictType);
        if (dict == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST.getCode(), "字典类型不存在");
        }
        dictItem.setDictId(dict.getId());
        dictItem.setDictType(dictType);
        sysDictItemMapper.insert(dictItem);
        return dictItem;
    }

    @Override
    @Transactional
    public SysDictItem updateDictItem(Long id, SysDictItem dictItem) {
        getDictItemById(id);
        dictItem.setId(id);
        sysDictItemMapper.updateById(dictItem);
        return dictItem;
    }

    @Override
    @Transactional
    public void deleteDictItem(Long id) {
        getDictItemById(id);
        sysDictItemMapper.deleteById(id);
    }
}
