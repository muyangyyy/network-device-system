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

## 生产部署

支持任意可运行 Docker 的主机（Linux / NAS / 云服务器）：

```bash
cp .env.example .env          # 修改 MYSQL_ROOT_PASSWORD 与 JWT_SECRET
./deploy.sh                   # 一键构建并启动（Windows 用 deploy.bat）
```

- mysql / redis 容器默认**不对外暴露端口**（backend 经 Docker 内部网络直连），公网部署安全默认
- 数据库首次启动自动执行 `backend/src/main/resources/db/init.sql`（建表 + 种子数据）
- 详细的更新部署、数据库迁移、运维与回滚步骤见 [`部署步骤手册.md`](./部署步骤手册.md)

**重建单个服务**（改代码后）：
```bash
sudo docker compose build backend && sudo docker compose up -d backend
```

**已知环境事项**：
- Docker Hub 拉取慢时可在 `/etc/docker/daemon.json` 配置镜像加速。
- 后端镜像构建用阿里云 Maven 镜像（见 `backend/Dockerfile`），前端 `npm ci` 走 npmmirror。

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
├── 部署步骤手册.md                    # 生产部署完整步骤（更新部署/数据库迁移/运维回滚）
└── README.md
```

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


| 模块 | 说明 |
|------|------|
| 首页驾驶舱 | 实时统计、趋势图、待办事项 |
| 设备台账 | 设备 CRUD、状态管理、导入导出 |
| 设备分组 | 多级分组树、分组管理 |
| 维修记录 | 记录创建、分派、处理、验收闭环，支持删除 |
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
