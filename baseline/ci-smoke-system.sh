#!/usr/bin/env bash
# 权限域全流程 curl 冒烟(需后端已在 :8080 运行,MySQL schema 已建)
set -u
BASE=http://localhost:8080
pass=0; fail=0
ok()  { echo "PASS: $1"; pass=$((pass+1)); }
bad() { echo "FAIL: $1"; fail=$((fail+1)); }

echo "== 1. admin 登录并取权限集合 =="
LOGIN=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123","deviceId":"web"}')
ACCESS=$(echo "$LOGIN" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["accessToken"])')
[ -n "$ACCESS" ] && ok "admin 登录" || bad "admin 登录失败"

INFO=$(curl -s "$BASE/auth/info" -H "Authorization: Bearer $ACCESS")
echo "$INFO" | grep -q '"admin"' && echo "$INFO" | grep -q '\*:\*:\*' \
  && ok "/auth/info 含 admin 角色与全量权限标识" || bad "/auth/info 权限集合异常"

echo "== 2. /auth/routers 返回完整路由树 =="
ROUTERS=$(curl -s "$BASE/auth/routers" -H "Authorization: Bearer $ACCESS")
echo "$ROUTERS" | grep -q '系统管理' && echo "$ROUTERS" | grep -q '业务功能' \
  && ok "路由树含系统管理与业务功能目录" || bad "路由树异常"

echo "== 3. admin 可访问三组管理接口 =="
for ep in "/system/user/list" "/system/role/list" "/system/menu/tree"; do
  CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE$ep" -H "Authorization: Bearer $ACCESS")
  [ "$CODE" = "200" ] && ok "GET $ep 200" || bad "GET $ep $CODE"
done

echo "== 4. 建受限用户(无角色)=="
curl -s -X POST "$BASE/system/user" -H "Authorization: Bearer $ACCESS" -H 'Content-Type: application/json' \
  -d '{"userName":"smoke_limited","nickName":"冒烟受限","password":"smoke123","status":"0"}' >/dev/null
LIMITED_LOGIN=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"smoke_limited","password":"smoke123"}')
LIMITED_ACCESS=$(echo "$LIMITED_LOGIN" | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["accessToken"])')
[ -n "$LIMITED_ACCESS" ] && ok "受限用户登录" || bad "受限用户登录失败"

echo "== 5. 受限用户直调管理接口(预期 403)=="
for ep in "/system/user/list" "/system/role/list" "/system/menu/tree"; do
  CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE$ep" -H "Authorization: Bearer $LIMITED_ACCESS")
  [ "$CODE" = "403" ] && ok "无权限 GET $ep 403" || bad "无权限 GET $ep 返回 $CODE(应 403)"
done
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/system/user" \
  -H "Authorization: Bearer $LIMITED_ACCESS" -H 'Content-Type: application/json' \
  -d '{"userName":"hacker","nickName":"hacker","password":"hack123"}')
[ "$CODE" = "403" ] && ok "无权限写接口 403" || bad "无权限写接口返回 $CODE"

echo "== 6. 受限用户路由树为空 =="
LIMITED_ROUTERS=$(curl -s "$BASE/auth/routers" -H "Authorization: Bearer $LIMITED_ACCESS")
[ "$(echo "$LIMITED_ROUTERS" | python3 -c 'import sys,json;print(len(json.load(sys.stdin)["data"]))')" = "0" ] \
  && ok "受限用户 routers 为空" || bad "受限用户 routers 非空"

echo "== 7. 种子保护:admin 账号不可改/删 =="
R=$(curl -s -X PUT "$BASE/system/user" -H "Authorization: Bearer $ACCESS" -H 'Content-Type: application/json' \
  -d '{"userId":1,"nickName":"改名"}')
echo "$R" | grep -q '种子管理员' && ok "admin 修改被拒" || bad "admin 修改未被拒"
R=$(curl -s -X DELETE "$BASE/system/user/1" -H "Authorization: Bearer $ACCESS")
echo "$R" | grep -q '种子管理员' && ok "admin 删除被拒" || bad "admin 删除未被拒"

echo "== 8. 清理冒烟用户 =="
# 先把受限用户清掉(userId=1 保护不影响)
USERS=$(curl -s "$BASE/system/user/list?userName=smoke_limited" -H "Authorization: Bearer $ACCESS")
LIMITED_ID=$(echo "$USERS" | python3 -c 'import sys,json;print(json.load(sys.stdin)["rows"][0]["userId"])')
R=$(curl -s -X DELETE "$BASE/system/user/$LIMITED_ID" -H "Authorization: Bearer $ACCESS")
echo "$R" | grep -q '"code":200' && ok "冒烟用户已清理" || bad "冒烟用户清理失败: $R"

echo
echo "结果: PASS=$pass FAIL=$fail"
[ "$fail" = "0" ]
