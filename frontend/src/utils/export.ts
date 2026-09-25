import * as XLSX from 'xlsx'
import dayjs from 'dayjs'

export interface ExportColumn {
  label: string
  prop: string
}

/** 一个工作表：表头 + 数据行（二维数组，列序与表头一致） */
export interface ExportSheet {
  /** 工作表名。Excel 限制 31 字符，且不能包含 : \ / ? * [ ] */
  name: string
  header: string[]
  rows: (string | number)[][]
}

function normalizeCell(value: unknown): string | number {
  if (value === null || value === undefined) return ''
  if (value instanceof Date) return dayjs(value).format('YYYY-MM-DD HH:mm:ss')
  return typeof value === 'number' ? value : String(value)
}

function autoColWidths(header: string[], rows: (string | number)[][]) {
  return header.map((label, i) => ({
    wch: Math.max(label.length * 2, ...rows.map((row) => String(row[i] ?? '').length), 10)
  }))
}

/** 把多张工作表写入同一个 .xlsx 并触发下载 */
export function exportSheets(sheets: ExportSheet[], filename?: string) {
  const wb = XLSX.utils.book_new()
  sheets.forEach((sheet) => {
    const ws = XLSX.utils.aoa_to_sheet([sheet.header, ...sheet.rows])
    ws['!cols'] = autoColWidths(sheet.header, sheet.rows)
    XLSX.utils.book_append_sheet(wb, ws, sheet.name)
  })
  XLSX.writeFile(wb, `${filename || 'export'}_${dayjs().format('YYYYMMDDHHmmss')}.xlsx`)
}

/** 单表导出（保持既有调用方兼容） */
export function exportExcel(data: any[], columns: ExportColumn[], filename?: string) {
  const header = columns.map((col) => col.label)
  const rows = data.map((row) => columns.map((col) => normalizeCell(row[col.prop])))
  exportSheets([{ name: 'Sheet1', header, rows }], filename)
}
