package com.network.device.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.network.device.entity.SysDict;
import com.network.device.entity.SysDictItem;

import java.util.List;

public interface SysDictService {

    Page<SysDict> listDicts(Integer page, Integer pageSize);

    SysDict getDictById(Long id);

    SysDict createDict(SysDict dict);

    SysDict updateDict(Long id, SysDict dict);

    void deleteDict(Long id);

    List<SysDictItem> listDictItemsByType(String dictType);

    SysDictItem getDictItemById(Long id);

    SysDictItem createDictItem(String dictType, SysDictItem dictItem);

    SysDictItem updateDictItem(Long id, SysDictItem dictItem);

    void deleteDictItem(Long id);
}
