// 全局模块声明补充（第十六轮构建验证新增）
// element-plus 未随包提供 dist/locale 的类型声明，vue-tsc 报 TS7016，此处补声明
declare module 'element-plus/dist/locale/zh-cn.mjs' {
  const zhCn: any
  export default zhCn
}
