#!/usr/bin/env bash
# 网络设备台账管理系统 —— 一键部署脚本
# 用法:  ./deploy.sh         启动（首次会自动构建镜像）
#        ./deploy.sh logs    查看日志
#        ./deploy.sh stop    停止
#        ./deploy.sh down    停止并删除容器（数据卷保留）
#        ./deploy.sh reset   停止并删除容器 + 数据卷（会清空数据！）

set -euo pipefail

cd "$(dirname "$0")"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info() { printf "${GREEN}[部署]${NC} %s\n" "$1"; }
warn() { printf "${YELLOW}[提示]${NC} %s\n" "$1"; }
fail() { printf "${RED}[错误]${NC} %s\n" "$1" >&2; exit 1; }

# ---------- 环境检查 ----------
if ! command -v docker >/dev/null 2>&1; then
  fail "未检测到 docker，请先安装 Docker Desktop：https://www.docker.com/products/docker-desktop"
fi
if ! docker info >/dev/null 2>&1; then
  fail "Docker 未运行，请先启动 Docker Desktop 再执行本脚本。"
fi

COMPOSE="docker compose"
$COMPOSE version >/dev/null 2>&1 || COMPOSE="docker-compose"

# ---------- 首次运行生成 .env ----------
if [ ! -f .env ]; then
  info "未找到 .env，正在从 .env.example 生成..."
  cp .env.example .env
  warn "已生成 .env，生产环境请修改其中的 MYSQL_ROOT_PASSWORD 与 JWT_SECRET。"
fi

# 读取端口（用于最后输出访问地址）
FRONTEND_PORT=$(grep -E '^FRONTEND_PORT=' .env | cut -d= -f2 | tr -d '[:space:]')
FRONTEND_PORT=${FRONTEND_PORT:-80}
# 注意：不能写成 $(grep ... | cut ... || echo 8080)。
# 管道的退出码取自最后一个命令（tr），即使 grep 没匹配到、输入为空，tr 仍然返回 0，
# 于是 || 分支永远不会执行，端口会被展开成空字符串，打印出 http://localhost:/doc.html。
BACKEND_PORT=$(grep -E '^BACKEND_PORT=' .env | cut -d= -f2 | tr -d '[:space:]')
BACKEND_PORT=${BACKEND_PORT:-8080}

# ---------- 子命令 ----------
case "${1:-up}" in
  logs)
    exec $COMPOSE logs -f --tail=100
    ;;
  stop)
    info "停止服务..."
    $COMPOSE stop
    ;;
  down)
    info "停止并删除容器（数据卷保留）..."
    $COMPOSE down
    ;;
  reset)
    warn "此操作会删除数据库与上传文件，且不可恢复！"
    read -r -p "确认继续？输入 yes 回车： " ans
    [ "$ans" = "yes" ] || { info "已取消。"; exit 0; }
    $COMPOSE down -v
    info "已清空。重新执行 ./deploy.sh 可全新部署。"
    ;;
  up|"")
    info "开始构建并启动（首次约需 5-10 分钟）..."
    $COMPOSE up -d --build

    info "等待 MySQL 就绪..."
    for i in $(seq 1 60); do
      if docker exec network-device-mysql mysqladmin ping -h 127.0.0.1 \
           -u root -p"$(grep -E '^MYSQL_ROOT_PASSWORD=' .env | cut -d= -f2 | tr -d '[:space:]')" --silent >/dev/null 2>&1; then
        break
      fi
      sleep 2
    done

    echo
    $COMPOSE ps
    echo
    info "部署完成！"
    echo "  前端地址： http://localhost:${FRONTEND_PORT}"
    echo "  接口文档： http://localhost:${BACKEND_PORT}/doc.html"
    echo "  默认账号： admin / admin123"
    echo
    echo "  查看日志： ./deploy.sh logs"
    echo "  停止服务： ./deploy.sh stop"
    ;;
  *)
    fail "未知命令：$1（可用：up / logs / stop / down / reset）"
    ;;
esac
