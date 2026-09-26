#!/usr/bin/env bash
# ============================================================
# Personal Website 服务器安装脚本（Ubuntu 22.04，幂等）
#
# 用法：
#   1. 上传产物到服务器：
#        scp -P 56220 app.jar            root@103.39.64.76:/opt/personal-website/
#        scp -P 56220 -r dist/*          root@103.39.64.76:/opt/personal-website/web/
#        scp -P 56220 -r uploads/*       root@103.39.64.76:/opt/personal-website/uploads/
#        scp -P 56220 personal_website.sql app.env personal-website.service setup.sh root@103.39.64.76:/tmp/psite/
#   2. 执行：ssh -p 56220 root@103.39.64.76 'bash /tmp/psite/setup.sh'
#
# 依赖：openjdk-17-jre-headless、mysql-server（见 README 部署章节）
# ============================================================
set -euo pipefail

APP_DIR=/opt/personal-website
STAGE=/tmp/psite
SERVICE=personal-website

mkdir -p "$APP_DIR/web" "$APP_DIR/uploads" "$APP_DIR/logs" "$APP_DIR/config"

# ---- 1. 数据库：仅当目标库为空时导入初始数据 ----
if [ -f "$STAGE/personal_website.sql" ]; then
  TABLES=$(mysql -N -B -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='personal_website';")
  if [ "$TABLES" = "0" ]; then
    mysql personal_website < "$STAGE/personal_website.sql"
    echo "[setup] database imported."
  else
    echo "[setup] database already contains $TABLES tables, skip import."
  fi
fi

# ---- 2. 环境变量文件（含数据库密码，仅 root 可读）----
install -m 600 "$STAGE/app.env" "$APP_DIR/app.env"

# ---- 3. systemd 服务 ----
install -m 644 "$STAGE/personal-website.service" "/etc/systemd/system/$SERVICE.service"
systemctl daemon-reload
systemctl enable "$SERVICE" >/dev/null 2>&1 || true
systemctl restart "$SERVICE"

sleep 6
echo "[setup] service status: $(systemctl is-active $SERVICE)"
echo "[setup] listening: $(ss -tln | grep ':8086' || echo 'NOT LISTENING')"
curl -fsS -o /dev/null -w "[setup] local health: %{http_code}\n" "http://127.0.0.1:8086/api/statistic/today" || true
