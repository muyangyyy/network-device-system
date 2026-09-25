# 网络设备全生命周期台账管理系统

企业级网络设备台账管理、维修工单和数据分析系统，适用于企业、机房、园区网络运维场景。

## 功能特性

- 设备电子化建档与全生命周期管理
- 多级设备分组（按区域/类型/业务/班组）
- 三类维修工单（硬件维修、调试维修、光路异常维修）
- 工单状态流转与闭环管理
- 维修数据可视化分析（ECharts）
- 首页驾驶舱实时统计
- Excel 导入导出
- 三种角色权限隔离（管理员/运维人员/只读人员）
- JWT 认证与 BCrypt 密码加密
- 操作日志与审计追踪

## 技术栈

### 前端
- Vue 3 + TypeScript + Vite
- Element Plus + @element-plus/icons-vue
- Pinia（状态管理）
- Vue Router（路由）
- Axios（HTTP 请求）
- ECharts（图表）
- dayjs（日期处理）

### 后端
- Java 17 + Spring Boot 3.2
- Spring Security + JWT（jjwt 0.12）
- MyBatis-Plus 3.5（SQL 全部写在 Mapper 注解里，无 XML 映射文件）
- MySQL 8.0
- EasyExcel（设备台账导入导出）
- Knife4j / springdoc-openapi（接口文档 `/doc.html`）
- Hutool、Redis（依赖已声明，见下方说明）

### 部署
- Docker Compose
- Nginx（前端静态服务）

> **依赖说明**：`pom.xml` 中声明了 Hutool 与 `spring-boot-starter-data-redis`，
> 但代码里没有任何调用点——Hutool 全项目零引用，Redis 只有一个 `RedisTemplate` Bean 定义、
> 没有缓存读写。两者保留为后续扩展预留；`docker-compose.yml` 仍把 Redis 作为后端启动的
> 前置健康检查，因此走 Docker 部署时请保留该服务。
>
> **Excel 导入导出**：设备台账的导入 / 导出走后端 EasyExcel（`/devices/import`、`/devices/export`）；
> 前端 `xlsx` 库只负责「维修工单列表」等页面的客户端导出，以及统计报表的多工作表导出。

## 快速开始

### 环境要求
- JDK 17+
- Node.js 18+
- Maven 3.8+
- MySQL 8.0+
- Redis 7+

### 方式一：Docker 一键部署（推荐）

只需要装好 [Docker Desktop](https://www.docker.com/products/docker-desktop)，然后一条命令：

```bash
cd network-device-system
./deploy.sh          # Windows 用： deploy.bat
```

脚本会自动完成：生成 `.env` 配置 → 构建前后端镜像 → 按健康检查顺序启动 MySQL / Redis / 后端 / 前端 → 打印访问地址。

首次构建约 5-10 分钟。完成后访问 **http://localhost** 即可。

常用命令：

| 命令 | 作用 |
|------|------|
| `./deploy.sh` | 启动（或重建后启动） |
| `./deploy.sh logs` | 实时查看日志 |
| `./deploy.sh stop` | 停止服务 |
| `./deploy.sh down` | 停止并删除容器（数据保留） |
| `./deploy.sh reset` | 清空数据重新部署 |

**端口冲突**：直接改项目根目录的 `.env`（首次运行后生成），无需动 `docker-compose.yml`：

```ini
FRONTEND_PORT=8080
BACKEND_PORT=18080
MYSQL_PORT=13306
REDIS_PORT=16379
```

### 方式二：本地开发

环境要求：JDK 17+、Node.js 18+、Maven 3.8+、MySQL 8.0+、Redis 7+

```bash
# 1. 初始化数据库
mysql -u root -p < backend/src/main/resources/db/init.sql

# 2. 启动 Redis（可选：当前代码没有 Redis 读写，仅用于保持与 Docker 部署环境一致）
redis-server

# 3. 启动后端（新终端）
cd backend && mvn spring-boot:run

# 4. 启动前端（新终端）
cd frontend && npm install && npm run dev
```

访问地址：

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:3000 |
| 后端 API | http://localhost:8080/api |
| 接口文档 | http://localhost:8080/doc.html |

> 注意：本地开发时前端默认走 Vite 代理转发 `/api` 到 `localhost:8080`，无需额外配置跨域。

### 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 运维人员 | operator | operator123 |

首次登录后请修改默认密码。

## 线上部署（飞牛 NAS 实录）

本项目已部署到飞牛 NAS（117.159.223.35），部署目录 `/vol2/1000/Docker/network-device-system/`，2026-09 全链路验证通过。

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端 | http://117.159.223.35:8088 | nginx 容器，宿主机 8088→容器 80（80 被飞牛系统占用，故用 8088） |
| 后端 API | http://117.159.223.35:8080/api | 经前端 nginx `/api` 反代亦可访问 |
| 接口文档 | http://117.159.223.35:8080/doc.html | knife4j |

登录账号同上（admin / admin123）。

**安全默认**：mysql/redis 容器不对宿主机暴露端口（backend 经 Docker 内部网络直连），如需本机调试数据库，临时在 `docker-compose.yml` 给 mysql 加 `ports: ["127.0.0.1:3306:3306"]`，远程管理走 SSH 隧道。`.env`（含 `MYSQL_ROOT_PASSWORD`、`JWT_SECRET`，chmod 600）位于部署目录，`cat .env` 查看。

**维护命令**（SSH 登录后）：
```bash
cd /vol2/1000/Docker/network-device-system
sudo docker compose ps          # 容器状态
sudo docker compose logs -f backend   # 后端日志
./deploy.sh stop | down | reset # 停止 / 删容器留卷 / 清数据
```

**重建后端**（改代码后）：
```bash
sudo docker compose build backend && sudo docker compose up -d backend
```

**已知环境事项**：
- NAS 无公网拉取 Docker Hub 极慢，`/etc/docker/daemon.json` 已配 daocloud/rat.dev/1panel 三个镜像加速（备份 `.bak`）。
- 后端镜像构建用阿里云 Maven 镜像（见 `backend/Dockerfile`），前端 `npm ci` 走 npmmirror。
- NAS 系统时钟：2026-09-25 复查已通过 NTP 自动校准（`System clock synchronized: yes`，与本地时间秒级一致），此前「快约 17 小时」的偏移已消除，日志时间戳可直接使用。

## 项目结构

```
network-device-system/
├── backend/                          # 后端项目
│   ├── src/main/java/com/network/device/
│   │   ├── NetworkDeviceApplication.java
│   │   ├── annotation/               # 自定义注解
│   │   ├── aspect/                   # AOP 切面
│   │   ├── common/                   # 通用类（Result, 异常处理等）
│   │   ├── config/                   # 配置类
│   │   ├── controller/               # 控制器
│   │   ├── dto/                      # 数据传输对象
│   │   ├── entity/                   # 实体类
│   │   ├── enums/                    # 枚举类
│   │   ├── mapper/                   # MyBatis Mapper（注解 SQL）
│   │   ├── security/                 # 安全与JWT
│   │   ├── service/                  # 业务服务
│   │   └── vo/                       # 视图对象
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/init.sql              # 数据库初始化
│   └── pom.xml
├── frontend/                         # 前端项目
│   ├── src/
│   │   ├── api/                      # API 接口
│   │   ├── components/               # 公共组件
│   │   ├── layout/                   # 布局组件
│   │   ├── router/                   # 路由配置
│   │   ├── store/                    # Pinia 状态
│   │   ├── styles/                   # 样式
│   │   ├── utils/                    # 工具函数
│   │   └── views/                    # 页面视图
│   ├── package.json
│   └── vite.config.ts
├── deploy.sh                         # 一键部署脚本（macOS / Linux / Git Bash）
├── deploy.bat                        # 一键部署脚本（Windows）
├── .env.example                      # 部署配置模板（首次运行自动复制为 .env）
├── docker-compose.yml
├── 代码审查与部署优化说明.md          # 逐轮代码审查记录（问题定位 / 修复 / 验证）
└── README.md
```

## 已修复问题

下表是早期版本经代码审查确认的真实缺陷，**当前代码均已修复**，保留在此便于对照回归：

| 级别 | 模块 | 问题 | 现状 |
|------|------|------|------|
| 高 | 设备分组 | 前端调用 `/groups/*`，后端映射为 `/api/device-groups/*`，分组功能全部 404 | 前后端统一为 `/device-groups` |
| 高 | 操作日志 | 前端调用 `/api/system-logs`，后端缺少对应的 Controller，日志页无数据 | 已补 `SystemLogController` |
| 高 | 数据统计 | 6 个统计接口的返回结构与前端类型定义不一致，图表无法渲染 | 7 个接口的 VO 字段与 `api/statistics.ts` 逐字段对齐 |
| 中 | 设备导入 | 前端以 multipart 上传，后端接收 JSON 数组，导入必然失败 | 后端改为 `@RequestParam MultipartFile` |
| 中 | 数据导出 | 前端按 blob 接收，后端返回 JSON，导出失败 | 后端改为 EasyExcel 直接写响应流 |
| 中 | 字典项 | 前端 `/dicts/{dictId}/items/{itemId}`，后端 `/dicts/items/{id}`，路径不一致 | 统一为 `/dicts/{dictType}/items` + `/dicts/items/{id}` |
| 中 | 工单附件 | 前端调用 `/repairs/{id}/attachments` 与 `/api/upload`，后端无上传接口 | 已补 `FileController` |
| 低 | 统计 | `StatisticsMapper.xml` 中 4 条语句无对应接口方法，属未接入的死代码 | 所有 SQL 已改为 Mapper 注解，项目内不存在 XML 映射文件 |
| 低 | 安全 | `SecurityConfig` 放行 `/api/auth/captcha`，但该接口不存在 | 放行项已移除 |
| 高 | 工单完成 | `completeOrder` 中 `setDeviceRepairStatus` 在 `updateById` 之后执行，`device_repair_status` 列从未持久化 | 已移到 `updateById` 之前 |
| 中 | 工单导出 | `exportOrders` 只拷贝 4/11 个筛选字段，导出数据与页面筛选不一致 | 已补全全部 11 个字段 |
| 中 | 用户管理 | `resetPassword` 硬编码 `"123456"` | 改为生成 8 位随机密码并返回明文 |
| 高 | 分组 / 权限树 | 编辑分组或权限时可以把父级设成自己或自己的后代，树递归会 StackOverflowError | 改父级前做环校验，父级不存在时拒绝 |
| 中 | 工单取消 | 取消已分配的工单后设备状态不恢复，永久卡在「故障维修」 | 取消后无其他活跃工单时自动恢复「正常」并记日志 |
| 中 | 编号并发 | 工单号 / 设备编码「读 MAX +1 后插入」存在竞态，并发创建撞唯一键报 500 | 捕获唯一键冲突后自动重取序号重试（最多 3 次） |
| 高 | 部门树 | 编辑部门时可以把父级设成自己或自己的后代，子部门树从树上静默消失（第十三轮树形服务修复漏网） | updateDepartment 增加环校验；创建时校验父部门存在；与分组服务同规则 |
| 中 | 部门删除 | 删除有子部门/有用户的部门后引用悬空：子部门树消失、用户档案部门为空 | 删除前检查子部门与在用用户，存在即拒删 |
| 中 | 权限删除 | 删除有子权限的权限后子权限 parentId 悬空，子树从权限树消失 | 删除前检查子权限，存在即拒删 |
| 低 | 分组 / 部门编辑 | 编辑时清空「上级分组/部门」保存后父级不变（undefined 被 JSON 省略，后端跳过更新） | 提交时归一化 parentId ?? 0，0 表示顶级 |
| 低 | 附件上传 | 后端业务异常返回 HTTP 200 + code≠200，el-upload 误报「上传成功」 | 成功回调增加业务码检查，失败显示后端消息且不 emit success |
| 低 | 工单创建 | 设备搜索框文案称「编码或名称」但实现只按编码搜索 | 文案改为「输入设备编码搜索」 |
| 中 | 设备编辑 | 清空「分组/负责人」保存后关联不变（undefined 被 JSON 省略，后端跳过更新；设备调组/换负责人是高频操作） | 编辑提交归一化 0 哨兵，后端 LambdaUpdateWrapper 显式 SET NULL；创建分支防御 0→NULL |
| 中 | 用户编辑 | 清空「部门」保存后部门不变（同病，用户调岗到未分配状态无法操作） | 同款 0 哨兵 + UpdateWrapper SET NULL |
| 低 | 分组编辑 | 清空「负责人」保存后负责人不变（同病） | 同款 0 哨兵 + UpdateWrapper SET NULL |
| 信息 | 部门编辑 | 清空「负责人」同样清不掉，但部门更新是实体直绑（传 0 会原样写库，哨兵不可用），改造需 DTO 化整条链路 | 记录不改（低频操作），前端保持不传该字段、无写 0 风险 |
| 中 | 错误提示 | 全站 20 处 catch 用固定文案（如「删除失败」）吞掉后端可读拒绝原因 | 统一透传 `e.message`，无消息时才显示兜底文案 |
| 中 | 工单返修 | 返修时 set null 的完成时间/方案/结果被 updateById 的 NOT_NULL 策略跳过，返修中仍显示上一轮数据 | LambdaUpdateWrapper 显式 SET NULL |
| 中 | 个人信息 | getCurrentUser 未填充部门名、前端类型也缺字段，「部门」永远显示「-」 | 后端查部门表填充；前端 UserInfo 补 departmentName |
| 中 | 工单附件 | 工单详情从不组装 attachments，处理页已上传附件永远不回显 | getOrderVOById 复用 listAttachments 组装 |
| 低 | 静默加载失败 | 13 个页面共 39 处 catch 吞掉加载错误，页面空白无提示（repair 三页之外 36 处由首次构建验证后的全项目复扫发现） | 统一透传后端消息，无消息时兜底「加载…失败」 |
| 低 | 工单创建 | 步进表单用 `validate(callback)` 反模式产生 unhandled rejection | 统一为 try/await/catch 形态 |
| 信息 | 构建验证 | 首次 vue-tsc 构建暴露 5 处类型错误（locale 模块无声明、log.vue 响应类型）——静态审查盲区实证 | 补 shims.d.ts 模块声明；request.get 标注 any；构建已通过（1m07s），产物 dist/ |

## 统计筛选口径

统计接口（`/api/statistics/**`）共用一组查询参数：
`startTime` / `endTime` / `groupId` / `deviceType` / `repairType` / `repairUserId`。

| 条件 | 工单维度统计 | 设备维度统计 |
|------|--------------|--------------|
| `startTime` / `endTime` | 生效（按 `created_at`，左闭右开） | 生效（按 `created_at`，左闭右开） |
| `groupId` | 生效（精确匹配，不递归子分组） | 生效（精确匹配，不递归子分组） |
| `deviceType` | 生效（经 `device_id` 关联 `network_device`） | 生效 |
| `repairType` | 生效 | 不适用（设备表无该字段） |
| `repairUserId` | 生效 | 不适用（设备表无该字段） |

- **左闭右开**：只选到日期时，结束日期**当天**的数据会被包含（上界自动补到次日零点）。
- **工单维度**：维修类型分布、维修趋势、分组 / 设备维修排名、效率统计，以及概览里的工单类指标。
- **设备维度**：设备状态分布，以及概览里的设备类指标。
- 维修趋势图固定展示「近 30 天 / 近 12 周 / 近 12 个月」，所选时间范围会与该窗口取交集。

## 已知限制

| 级别 | 模块 | 说明 |
|------|------|------|
| 中 | 返修率 / 一次修复率 | 返修率按「当前状态为 `REPAIR_AGAIN` 的工单数」统计。工单被重新处理后该状态即消失，指标会随流程推进变小。若需按「曾经被退回重做」统计，需改为关联 `repair_status_log`，待确认业务定义。 |
| 中 | 完成率分母 | 分母包含 `DRAFT`（草稿）与 `CANCELLED`（已取消）的工单。若这两类不应计入，完成率会被系统性低估。 |
| 中 | 逻辑删除 + 唯一索引 | `username` / `role_key` / `dict_type` 的唯一索引不区分逻辑删除，删除后同名记录无法复用（接口会给出明确提示）。若要允许复用需修改 DDL 并执行 `./deploy.sh reset`（会清空数据）。 |
| 中 | 接口文档公开 | `/doc.html`（Knife4j）未做鉴权，生产环境建议在 Nginx 层限制访问。 |
| 中 | 请求体直接绑定实体 | 部分接口（如 `SysDictController`）直接用实体作 `@RequestBody`，存在批量赋值风险，建议逐步改为 DTO。 |
| 低 | 登录 / 登出操作日志 | 操作日志筛选器里的「登录 / 登出」选项当前不会有记录：登录行为记录在独立的「登录日志」页。 |

> 完整的逐轮审查记录（问题定位、修复方案、验证脚本与结果）见
> [`代码审查与部署优化说明.md`](./代码审查与部署优化说明.md)。

## 系统模块

| 模块 | 说明 |
|------|------|
| 首页驾驶舱 | 实时统计、趋势图、待办事项 |
| 设备台账 | 设备 CRUD、状态管理、导入导出 |
| 设备分组 | 多级分组树、分组管理 |
| 维修工单 | 工单创建、分派、处理、验收闭环 |
| 数据统计 | 维修类型、趋势、排行、效率分析 |
| 系统管理 | 用户、角色、权限、部门、字典、日志 |

## API 接口

所有接口前缀为 `/api`，统一返回格式：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 0
}
```

## License

MIT
