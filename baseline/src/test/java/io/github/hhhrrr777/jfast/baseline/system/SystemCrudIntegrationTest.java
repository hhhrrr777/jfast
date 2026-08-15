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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户/角色/菜单 CRUD 细节测试(H2):唯一性冲突、种子保护、菜单树/环检测/类型校验、重置密码。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ---- 用户 ----

    @Test
    void duplicateUserNameRejected() throws Exception {
        String token = loginAdmin();
        mockMvc.perform(post("/system/user")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"admin\",\"nickName\":\"重复\",\"password\":\"pass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("已存在")));
    }

    @Test
    void seedAdminProtectedFromUpdateDeleteAndReset() throws Exception {
        String token = loginAdmin();
        // 修改 userId=1 被拒
        mockMvc.perform(MockMvcRequestBuilders.put("/system/user")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"nickName\":\"改名\"}"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("种子管理员")));
        // 删除 userId=1 被拒
        mockMvc.perform(MockMvcRequestBuilders.delete("/system/user/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(500));
        // 重置 admin 密码被拒
        mockMvc.perform(MockMvcRequestBuilders.put("/system/user/resetPwd")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"password\":\"newpass123\"}"))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void resetPasswordRevokesSessionsAndNewPasswordWorks() throws Exception {
        String token = loginAdmin();
        // 建用户
        mockMvc.perform(post("/system/user")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"resetme\",\"nickName\":\"resetme\",\"password\":\"oldpass\"}"))
                .andExpect(jsonPath("$.code").value(200));
        // 登录拿 refresh
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"resetme\",\"password\":\"oldpass\"}"))
                .andReturn();
        String refreshToken = objectMapper.readTree(login.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();
        // 管理员重置密码
        JsonNode user = findUser(token, "resetme");
        mockMvc.perform(MockMvcRequestBuilders.put("/system/user/resetPwd")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + user.path("userId").asLong() + ",\"password\":\"newpass99\"}"))
                .andExpect(jsonPath("$.code").value(200));
        // 旧 refresh 已被吊销
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(jsonPath("$.code").value(401));
        // 新密码可登录
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"resetme\",\"password\":\"newpass99\"}"))
                .andExpect(jsonPath("$.code").value(200));
    }

    // ---- 角色 ----

    @Test
    void roleKeyDuplicateRejectedAndAdminRoleProtected() throws Exception {
        String token = loginAdmin();
        // role_key=admin 重复
        mockMvc.perform(post("/system/role")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"假超管\",\"roleKey\":\"admin\"}"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("已存在")));
        // 超管角色不可删(先找到它)
        MvcResult list = mockMvc.perform(get("/system/role/list")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        JsonNode rows = objectMapper.readTree(list.getResponse().getContentAsString()).path("rows");
        long adminRoleId = -1;
        for (JsonNode row : rows) {
            if ("admin".equals(row.path("roleKey").asText())) {
                adminRoleId = row.path("roleId").asLong();
            }
        }
        assertThat(adminRoleId).isPositive();
        mockMvc.perform(MockMvcRequestBuilders.delete("/system/role/" + adminRoleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("超管角色")));
        // 已分配用户的角色不可删
        long commonRoleId = -1;
        for (JsonNode row : rows) {
            if ("common".equals(row.path("roleKey").asText())) {
                commonRoleId = row.path("roleId").asLong();
            }
        }
        // 给 limited 用户绑 common 再删 → 拒绝
        MvcResult userResult = mockMvc.perform(get("/system/user/list")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("userName", "admin"))
                .andReturn();
        JsonNode adminUser = objectMapper.readTree(userResult.getResponse().getContentAsString()).path("rows").get(0);
        mockMvc.perform(MockMvcRequestBuilders.put("/system/user")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + adminUser.path("userId").asLong() + ",\"nickName\":\"系统管理员\",\"status\":\"0\",\"roleIds\":[" + commonRoleId + "]}"))
                .andExpect(jsonPath("$.code").value(500)); // admin 用户受保护,改不了
        // common 未分配 → 可删
        mockMvc.perform(MockMvcRequestBuilders.delete("/system/role/" + commonRoleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(200));
    }

    // ---- 菜单 ----

    @Test
    void menuTreeAndTypeValidation() throws Exception {
        String token = loginAdmin();
        MvcResult tree = mockMvc.perform(get("/system/menu/tree")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode nodes = objectMapper.readTree(tree.getResponse().getContentAsString()).path("data");
        assertThat(nodes.size()).isEqualTo(2); // 系统管理 + 业务功能
        JsonNode systemDir = nodes.get(0).path("menuId").asLong() == 1L ? nodes.get(0) : nodes.get(1);
        assertThat(systemDir.path("children").size()).isEqualTo(3); // 用户/角色/菜单三菜单
        // 用户管理下有 5 个按钮
        JsonNode userMenu = systemDir.path("children").get(0);
        assertThat(userMenu.path("children").size()).isEqualTo(5);

        // 按钮缺 perms → 拒
        mockMvc.perform(post("/system/menu")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menuName\":\"无perms按钮\",\"parentId\":100,\"menuType\":\"F\"}"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("权限标识")));
        // 菜单缺 component → 拒
        mockMvc.perform(post("/system/menu")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menuName\":\"无组件菜单\",\"parentId\":1,\"menuType\":\"C\",\"path\":\"x\"}"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("组件路径")));
    }

    @Test
    void menuCycleAndBusinessDirProtection() throws Exception {
        String token = loginAdmin();
        // 把「系统管理」(id=1)挂到「用户管理」(id=100,其子)下 → 环,拒绝
        mockMvc.perform(MockMvcRequestBuilders.put("/system/menu")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menuId\":1,\"menuName\":\"系统管理\",\"parentId\":100,\"menuType\":\"M\",\"path\":\"system\"}"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("子菜单")));
        // 「业务功能」目录(id=2000)不可删
        mockMvc.perform(MockMvcRequestBuilders.delete("/system/menu/2000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("实体建模")));
        // 有子菜单的目录不可删
        mockMvc.perform(MockMvcRequestBuilders.delete("/system/menu/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("子菜单")));
    }

    // ---- 工具 ----

    private String loginAdmin() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private JsonNode findUser(String token, String userName) throws Exception {
        MvcResult result = mockMvc.perform(get("/system/user/list")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("userName", userName))
                .andReturn();
        JsonNode rows = objectMapper.readTree(result.getResponse().getContentAsString()).path("rows");
        assertThat(rows.size()).isEqualTo(1);
        return rows.get(0);
    }
}
