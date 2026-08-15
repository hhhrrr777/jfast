package io.github.hhhrrr777.jfast.baseline.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 权限域端到端测试(H2 内存库):登录受限用户 → /auth/info 权限集合 →
 * 受限接口 403 → 绑角色后放行。按钮级权限的前后端双校验之「后端安全边界」半边。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PermissionFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 全流程:创建无角色用户 → 登录 → 无权限访问受限接口 403 →
     * 绑定只读角色后 system:user:list 通过、写接口仍 403。
     */
    @Test
    void buttonLevelPermissionEndToEnd() throws Exception {
        // admin 登录(种子数据含超管角色)
        String adminToken = login("admin", "admin123");

        // admin 的 /auth/info 含超管权限标识与角色
        mockMvc.perform(get("/auth/info").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions").isArray())
                .andExpect(jsonPath("$.data.roles[0]").value("admin"));

        // admin 可访问受限接口
        mockMvc.perform(get("/system/user/list").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber());

        // 建一个无角色受限用户
        String userToken = login("limited", createUser(adminToken, "limited"));

        // 受限用户:info 无 admin 角色、无权限
        MvcResult info = mockMvc.perform(get("/auth/info").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode data = objectMapper.readTree(info.getResponse().getContentAsString()).path("data");
        assertThat(data.path("roles").toString()).doesNotContain("admin");
        assertThat(data.path("permissions").size()).isZero();

        // 受限用户直调受限接口:403(前端 v-hasPermi 的后端边界)
        mockMvc.perform(get("/system/user/list").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/system/user")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"x1\",\"nickName\":\"x1\",\"password\":\"pass123\"}"))
                .andExpect(status().isForbidden());

        // 给受限用户绑只读角色(仅 system:user:list + system:user:query 菜单)
        String roleId = createReadOnlyRole(adminToken);
        assignRole(adminToken, "limited", roleId);

        // 新登录后(权限集合在 token 生命周期内从库装载,旧 token 亦即时生效)可读不可写
        mockMvc.perform(get("/system/user/list").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/system/user")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"x2\",\"nickName\":\"x2\",\"password\":\"pass123\"}"))
                .andExpect(status().isForbidden());

        // 未登录直调:401
        mockMvc.perform(get("/system/user/list"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 停用用户即时失效:持旧 access token 访问任意认证接口 → 401(每请求从库装载)。
     */
    @Test
    void disabledUserTokenInstantlyInvalid() throws Exception {
        String adminToken = login("admin", "admin123");
        String password = createUser(adminToken, "todisable");
        String token = login("todisable", password);
        mockMvc.perform(get("/auth/info").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // admin 停用该用户
        JsonNode limited = findUser(adminToken, "todisable");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/system/user")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + limited.path("userId").asLong()
                                + ",\"nickName\":\"待停用\",\"status\":\"1\"}"))
                .andExpect(status().isOk());

        // 旧 token 立即失效(401)
        mockMvc.perform(get("/auth/info").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    /**
     * /auth/routers:admin 返回完整树;受限用户只返回其角色可见菜单。
     */
    @Test
    void routersFilteredByPermission() throws Exception {
        String adminToken = login("admin", "admin123");
        MvcResult routers = mockMvc.perform(get("/auth/routers").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode tree = objectMapper.readTree(routers.getResponse().getContentAsString()).path("data");
        // 顶层含「系统管理」与「业务功能」目录
        assertThat(tree.toString()).contains("系统管理", "业务功能");
        // 系统管理下含三个管理菜单
        assertThat(tree.toString()).contains("用户管理", "角色管理", "菜单管理");

        // 无角色用户:routers 为空数组
        String password = createUser(adminToken, "norole");
        String token = login("norole", password);
        MvcResult empty = mockMvc.perform(get("/auth/routers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        JsonNode emptyTree = objectMapper.readTree(empty.getResponse().getContentAsString()).path("data");
        assertThat(emptyTree.isArray()).isTrue();
        assertThat(emptyTree.size()).isZero();
    }

    // ---- 工具方法 ----

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    /** 建用户并返回其初始密码(明文,仅测试内使用)。 */
    private String createUser(String adminToken, String userName) throws Exception {
        String password = userName + "123";
        mockMvc.perform(post("/system/user")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"" + userName + "\",\"nickName\":\"" + userName
                                + "\",\"password\":\"" + password + "\",\"status\":\"0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        return password;
    }

    /** 建一个仅含用户查询权限的角色,返回 roleId。 */
    private String createReadOnlyRole(String adminToken) throws Exception {
        // 只勾「用户管理」菜单(id 100,perms=system:user:list)
        MvcResult result = mockMvc.perform(post("/system/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"只读角色\",\"roleKey\":\"readonly\",\"roleSort\":9,"
                                + "\"status\":\"0\",\"menuIds\":[100]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("roleId").asText();
    }

    private void assignRole(String adminToken, String userName, String roleId) throws Exception {
        JsonNode user = findUser(adminToken, userName);
        String body = "{\"userId\":" + user.path("userId").asLong()
                + ",\"nickName\":\"" + user.path("nickName").asText() + "\""
                + ",\"status\":\"" + user.path("status").asText() + "\""
                + ",\"roleIds\":[" + roleId + "]}";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/system/user")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private JsonNode findUser(String adminToken, String userName) throws Exception {
        MvcResult result = mockMvc.perform(get("/system/user/list")
                        .header("Authorization", "Bearer " + adminToken)
                        .queryParam("userName", userName))
                .andExpect(status().isOk()).andReturn();
        JsonNode rows = objectMapper.readTree(result.getResponse().getContentAsString()).path("rows");
        assertThat(rows.size()).isEqualTo(1);
        return rows.get(0);
    }
}
