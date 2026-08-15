#!/usr/bin/env bash
# 认证域全流程 curl 冒烟(需后端已在 :8080 运行)
set -u
BASE=http://localhost:8080
TMP="$(dirname "$0")"
pass=0; fail=0
ok()  { echo "PASS: $1"; pass=$((pass+1)); }
bad() { echo "FAIL: $1"; fail=$((fail+1)); }

echo "== 1. 登录成功签发双 token =="
LOGIN=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123","deviceId":"web"}')
echo "$LOGIN"
ACCESS=$(echo "$LOGIN" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["accessToken"])')
REFRESH=$(echo "$LOGIN" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["refreshToken"])')
[ -n "$ACCESS" ] && [ -n "$REFRESH" ] && ok "登录签发 access+refresh" || bad "登录未签发 token"

echo "== 2. 带 access 访问受保护端点 =="
INFO=$(curl -s "$BASE/auth/info" -H "Authorization: Bearer $ACCESS")
echo "$INFO"
echo "$INFO" | grep -q '"username":"admin"' && ok "受保护端点返回当前用户" || bad "受保护端点异常"

echo "== 3. 无 token 访问受保护端点(预期 401)=="
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/auth/info")
[ "$CODE" = "401" ] && ok "未登录返回 401" || bad "未登录未返回 401($CODE)"

echo "== 4. 刷新(rotation)=="
R1=$(curl -s -X POST "$BASE/auth/refresh" -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$REFRESH\"}")
NEWREFRESH=$(echo "$R1" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["refreshToken"])')
[ -n "$NEWREFRESH" ] && [ "$NEWREFRESH" != "$REFRESH" ] && ok "刷新签出新 refresh" || bad "刷新失败"

echo "== 5. 旧 refresh 重放(预期 401)=="
R2=$(curl -s -X POST "$BASE/auth/refresh" -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$REFRESH\"}")
echo "$R2" | grep -q '"code":401' && ok "旧 refresh 已作废" || bad "旧 refresh 仍可用"

echo "== 6. 登出删本端 refresh =="
curl -s -X POST "$BASE/auth/logout" -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$NEWREFRESH\"}" >/dev/null
R3=$(curl -s -X POST "$BASE/auth/refresh" -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$NEWREFRESH\"}")
echo "$R3" | grep -q '"code":401' && ok "登出后 refresh 失效" || bad "登出后 refresh 仍可用"

echo "== 7. 防爆破:连错 5 次锁定 =="
for i in 1 2 3 4 5; do
  curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d '{"username":"locktest","password":"bad"}' >/dev/null
done
LOCKED=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d '{"username":"locktest","password":"bad"}')
echo "$LOCKED"
echo "$LOCKED" | grep -q '锁定' && ok "连续失败触发锁定" || bad "未触发锁定"

echo
echo "结果: PASS=$pass FAIL=$fail"
[ "$fail" = "0" ]
