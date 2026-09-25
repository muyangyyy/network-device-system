import request from '@/utils/request'

export interface Dict {
  id: number
  dictName: string
  dictType: string
  status: number
  remark?: string
  createdAt?: string
}

export interface DictItem {
  id: number
  dictId?: number
  dictType?: string
  itemLabel: string
  itemValue: string
  sort: number
  status: number
  remark?: string
}

/** 后端 GET /dicts 只接收 page / pageSize(size)，没有 dictName / dictType 过滤参数 */
export function getDicts(params?: { page?: number; size?: number }) {
  return request.get<any, { records: Dict[]; total: number }>('/dicts', { params })
}

export function createDict(data: Partial<Dict>) {
  return request.post('/dicts', data)
}

export function updateDict(id: number, data: Partial<Dict>) {
  return request.put(`/dicts/${id}`, data)
}

export function deleteDict(id: number) {
  return request.delete(`/dicts/${id}`)
}

/** 按字典类型(dictType)查询字典项 */
export function getDictItems(dictType: string) {
  return request.get<any, DictItem[]>(`/dicts/${dictType}/items`)
}

/** 按字典类型(dictType)新增字典项 */
export function createDictItem(dictType: string, data: Partial<DictItem>) {
  return request.post(`/dicts/${dictType}/items`, data)
}

export function updateDictItem(itemId: number, data: Partial<DictItem>) {
  return request.put(`/dicts/items/${itemId}`, data)
}

export function deleteDictItem(itemId: number) {
  return request.delete(`/dicts/items/${itemId}`)
}
