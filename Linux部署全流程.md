# 网络设备管理系统 — Linux 安装部署全流程

> 适用：任何全新 Linux 服务器（Ubuntu / Debian / CentOS / Rocky / 国产发行版均可）
> 部署形态：Docker Compose 四容器（MySQL 8 + Redis 7 + Spring Boot 3 后端 + Nginx 前端）
> 前置结论：宿主机**只需装 Docker**，其余全部由容器化构建解决，无需手动装 Java/Node/MySQL

---

## 一、环境要求

| 项 | 最低要求 | 推荐 |
|---|---|---|
| 系统 | Linux x86_64，内核 ≥ 3.10 | Ubuntu 22.04 / Debian 12 / Rocky 9 |
| CPU | 2 核 | 4 核 |
| 内存 | 2 GB | 4 GB+ |
| 磁盘 | 10 GB 可用 | 20 GB+ |
| 网络 | 可访问外网（拉取镜像与依赖） | — |

> 实测参考：4 GB 内存主机跑满四容器后仅占约 0.7 GB（后端 JVM 限制在 -Xmx512m），余量充足。

---

## 二、安装 Docker 与 Compose（已装可跳过）

### 1. 卸载旧版本（避免冲突）

```bash
sudo apt-get remove -y docker docker-engine docker.io containerd runc 2>/dev/null   # Debian/Ubuntu
# sudo dnf remove -y docker docker-engine podman runc                                # CentOS/Rocky
```

### 2. 安装 Docker Engine（国内服务器推荐用阿里云源）

**Debian / Ubuntu：**

```bash
sudo apt-get update && sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://mirrors.aliyun.com/docker-ce/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
https://mirrors.aliyun.com/docker-ce/linux/ubuntu $(lsb_release -cs) stable" | \
sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

**CentOS / Rocky / Alma：**

```bash
sudo dnf install -y dnf-utils
sudo dnf config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
```

### 3. 启动并设置开机自启

```bash
sudo systemctl enable --now docker
sudo systemctl status docker        # 应为 active (running)
docker compose version              # 应输出 v2.x（compose 插件已含于上述安装）
```

### 4. 配置镜像加速（国内强烈建议）

```bash
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<'EOF'
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://docker.1panel.live"
  ],
  "log-driver": "json-file",
  "log-opts": { "max-size": "50m", "max-file": "3" }
}
EOF
sudo systemctl restart docker
```

> `log-opts` 限制容器日志体积，防止长期运行把磁盘写满。若服务器海外网络良好，可去掉 mirrors。

### 5. （可选）免 sudo 使用 docker

```bash
sudo usermod -aG docker $USER && newgrp docker    # 重新登录后生效
```

---

## 三、获取项目代码

### 方式 A：git clone（推荐，便于后续更新）

```bash
git clone https://github.com/<你的用户名>/network-device-system.git
cd network-device-system
```

> 仓库为 Private 时需要带 token：`git clone https://<token>@github.com/<用户名>/network-device-system.git`
> 或先 `gh auth login` / 配置凭据后再 clone。

### 方式 B：离线包上传

在本机打包（已排除依赖与构建产物，约 300 KB）：

```bash
tar -czf network-device-system.tar.gz \
  --exclude='frontend/node_modules' --exclude='frontend/dist' \
  --exclude='backend/target' --exclude='.git' network-device-system

# 上传到服务器
scp network-device-system.tar.gz user@<服务器IP>:~/
# 服务器上解压
tar -xzf network-device-system.tar.gz && cd network-device-system
```

---

## 四、配置环境变量

```bash
cp .env.example .env
vi .env
```

必须修改的两项（**生产环境务必改掉默认值**）：

```ini
# 数据库 root 密码（同时是 backend 连库密码）
MYSQL_ROOT_PASSWORD=<生成一个强随机串，如: openssl rand -hex 16>
# JWT 签名密钥（≥32 字符随机串）
JWT_SECRET=<openssl rand -base64 48>
# 端口（默认即可，冲突时改）
FRONTEND_PORT=80
BACKEND_PORT=8080
```

```bash
chmod 600 .env    # 收紧权限
```

> `.env` 已被 .gitignore 排除，永远不会进版本库。

---

## 五、一键部署

```bash
./deploy.sh          # 首次运行自动构建镜像并启动
```

或手动执行（与脚本等效）：

```bash
sudo docker compose up -d --build
```

**首次构建耗时参考**：后端 Maven 拉依赖约 3-8 分钟（Dockerfile 内已配阿里云 Maven 源）、前端 npm ci 约 1-3 分钟（已配 npmmirror），之后依赖层有缓存，重建只需 1-3 分钟。

**启动时序**（compose 已编排好依赖，无需干预）：

```
mysql 启动 → healthy 检查通过 → redis 启动 → healthy → backend 启动（连库）→ frontend
```

查看进度：

```bash
sudo docker compose ps                  # 等 mysql/redis 显示 (healthy)
sudo docker compose logs -f backend     # 看到 "Started NetworkDeviceApplication" 即就绪
```

---

## 六、数据库自动初始化（首次启动）

MySQL 容器首次启动（数据卷为空时）自动执行 `backend/src/main/resources/db/init.sql`：

- 创建全部业务表（设备/分组/工单/用户/角色/权限/字典/日志等）
- 写入种子数据：权限树、三种角色（ADMIN/OPERATOR/READONLY）、默认账号

> ⚠️ 只在**数据卷为空**时执行。之后如需变更表结构/种子数据，须手工对存量库执行 SQL。

验证初始化成功：

```bash
sudo docker exec network-device-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" network_device \
  -e "SHOW TABLES; SELECT username FROM sys_user;"
# 应看到全部表 + admin/operator 两个账号
```

---

## 七、验证部署

```bash
# 1. 四容器状态（mysql/redis 应有 healthy）
sudo docker compose ps

# 2. 后端探活
curl -s http://localhost:8080/api/auth/login -X POST \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
# 期望: {"code":200,"message":"登录成功","data":{"token":"..."}}

# 3. 前端页面
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8088/    # 200
```

浏览器访问 `http://<服务器IP>:8088` → 登录（admin / admin123）→ 侧边栏菜单齐全、仪表盘图表渲染正常。

**默认账号**：

| 角色 | 用户名 | 密码 | 权限 |
|---|---|---|---|
| 管理员 | admin | admin123 | 全部功能 |
| 运维人员 | operator | operator123 | 设备+工单全流程（无删除/验收） |
| 只读 | —（需自建） | — | 仅查看 |

> ⚠️ **首次登录后立即修改默认密码**（右上角头像 → 个人中心）。

---

## 八、防火墙与安全组放行

| 场景 | 需放行端口 |
|---|---|
| 仅本机访问 | 无需任何操作 |
| 局域网/公网 Web 访问 | TCP `8088`（前端）、可选 TCP `8080`（直连 API/接口文档） |
| 云服务器 | 在云控制台**安全组**放行上述 TCP 端口 |

系统防火墙示例：

```bash
# ufw (Ubuntu/Debian)
sudo ufw allow 8088/tcp && sudo ufw allow 8080/tcp

# firewalld (CentOS/Rocky)
sudo firewall-cmd --permanent --add-port=8088/tcp --add-port=8080/tcp && sudo firewall-cmd --reload
```

> 安全默认：mysql(3306)/redis(6379) **不对外暴露端口**，backend 经 Docker 内部网络直连。
> 如需本机调试数据库，临时在 docker-compose.yml 给 mysql 加
> `ports: ["127.0.0.1:3306:3306"]`，远程管理走 SSH 隧道。

---

## 九、（可选）域名 + HTTPS

1. 域名 A 记录解析到服务器 IP。
2. 两种做法任选：
   - **改用 80/443**：`.env` 设 `FRONTEND_PORT=80`，再用 certbot/caddy 在宿主机做 443 反代；
   - **保留 8088，宿主机 Nginx 反代**：

```nginx
server {
    listen 443 ssl;
    server_name your.domain.com;
    ssl_certificate     /etc/ssl/fullchain.pem;
    ssl_certificate_key /etc/ssl/privkey.pem;
    location / {
        proxy_pass http://127.0.0.1:8088;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

3. 若后端将跨域直连 API（不走前端反代），在 docker-compose.yml 的 backend 环境变量加：

```yaml
CORS_ALLOWED_ORIGINS: https://your.domain.com,http://<局域网IP>:8088
```

> 前端经 Nginx 同源反代 `/api` 访问时**无需配置**此项。

---

## 十、日常运维

### 应用更新（改代码后）

```bash
cd network-device-system && git pull          # 或重新上传改动文件

# 只改了后端
sudo docker compose build backend && sudo docker compose up -d backend
# 只改了前端
sudo docker compose build frontend && sudo docker compose up -d frontend
# 都改了
sudo docker compose build backend frontend && sudo docker compose up -d backend frontend
```

> 前端改动要求 index.html 不被浏览器缓存：项目 nginx.conf 已内置
> `Cache-Control: no-cache`（`location = /index.html`），无需额外操作。

### 数据备份（建议 cron 每日执行）

```bash
# 数据库逻辑备份
sudo docker exec network-device-mysql sh -c \
  'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" network_device' > backup_$(date +%F).sql

# 一并备份 .env 与上传附件（docker 卷 upload_data）
tar -czf uploads_$(date +%F).tar.gz /var/lib/docker/volumes/network-device-system_upload_data
```

crontab 示例（每日 02:00）：

```bash
0 2 * * * cd /path/to/network-device-system && ./backup.sh >> backup.log 2>&1
```

### 数据恢复

```bash
cat backup_2026-09-26.sql | sudo docker exec -i network-device-mysql \
  sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" network_device'
```

### 常用命令速查

```bash
sudo docker compose ps                    # 容器状态
sudo docker compose logs -f backend       # 跟随后端日志
sudo docker compose restart backend       # 重启单服务
./deploy.sh stop                          # 停止（保留容器）
./deploy.sh down                          # 删容器（保留数据卷）
./deploy.sh reset                         # ⚠️ 连数据卷一起清空（数据丢失！）
```

---

## 十一、故障排查

| 现象 | 排查步骤 |
|---|---|
| 容器起不来 | `docker compose logs <服务名>` 看报错；`docker compose ps` 看 Exit Code |
| backend 反复重启 | 多为连不上 mysql：确认 mysql healthy；`.env` 密码与首次初始化一致（改密码需重建 mysql 数据卷） |
| 页面 502 | backend 未就绪（启动约 40s）或挂了，`docker compose logs backend` |
| 登录报 500 | mysql 未 healthy 或 init.sql 未执行（数据卷是否为空） |
| 前端有页面但接口 404 | 前端 nginx `/api` 反代目标为 `backend:8080`，确认 backend 容器 running |
| 构建时 npm/maven 超时 | Dockerfile 已配国内源；海外服务器可自行替换回官方源 |
| 端口被占用 | 改 `.env` 的 `FRONTEND_PORT` / `BACKEND_PORT` 后 `up -d` |
| 磁盘持续增长 | 容器日志限了 50m×3；再查 `docker system df`，构建缓存可 `docker builder prune` |

**日志位置**：容器日志 `docker compose logs`；应用数据卷 `mysql_data`（数据库）、`redis_data`、`upload_data`（附件）。

---

## 十二、架构总览

```
浏览器 ──> :8088 前端容器(nginx: 静态资源 + /api 反代) ──> :8080 后端容器(Sprint Boot 3)
                                                          ├── mysql 容器 (3306, 内部网络)
                                                          ├── redis 容器 (6379, 内部网络)
                                                          └── upload_data 卷 (附件)
```

- 四容器内部网络 `network-device-net`，仅 frontend/backend 对宿主机暴露端口
- 全部容器 `restart: always`，宿主机重启后自动拉起
- 数据持久化：mysql_data / redis_data / upload_data 三个 docker 卷，`down` 不丢数据，`reset` 才清空
