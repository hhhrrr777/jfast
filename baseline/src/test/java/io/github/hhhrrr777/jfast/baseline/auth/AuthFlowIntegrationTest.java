package io.github.hhhrrr777.jfast.baseline.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hhhrrr777.jfast.baseline.system.mapper.SysRefreshTokenMapper;
import org.junit.jupiter.api.BeforeEach;
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
 * 认证域全流程接口测试(H2 内存库):登录 → 刷新(rotation) → 访问受保护端点 → 登出 → 锁定。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysRefreshTokenMapper refreshTokenMapper;

    @Autowired
    private io.github.hhhrrr777.jfast.baseline.auth.service.LoginFailureGuard loginFailureGuard;

    @BeforeEach
    void cleanTokens() {
        refreshTokenMapper.delete(null);
        // 防爆破 guard 为单例内存态,逐用例复位避免测试间污染
        loginFailureGuard.reset();
    }

    private String loginJson(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private JsonNode loginOk(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.expiresIn").value(7200))
                .andExpect(jsonPath("$.data.refreshExpiresIn").value(604800))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    @Test
    void loginIssuesDualTokenAndPersistsRefresh() throws Exception {
        JsonNode data = loginOk("admin", "admin123");
        assertThat(data.path("accessToken").asText()).isNotBlank();
        assertThat(data.path("refreshToken").asText()).isNotBlank();

        Long rows = refreshTokenMapper.selectCount(new LambdaQueryWrapper<>());
        assertThat(rows).isEqualTo(1L);
    }

    @Test
    void loginWithWrongPasswordRejected() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("admin", "wrong-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"));
    }

    @Test
    void accessTokenGrantsProtectedEndpoint() throws Exception {
        JsonNode data = loginOk("admin", "admin123");
        String accessToken = data.path("accessToken").asText();
        mockMvc.perform(get("/auth/info").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void protectedEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/auth/info"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void refreshRotatesTokenAndInvalidatesOld() throws Exception {
        JsonNode data = loginOk("admin", "admin123");
        String refreshToken = data.path("refreshToken").asText();

        // 刷新成功,签发新双 token
        MvcResult refreshed = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode newData = objectMapper.readTree(refreshed.getResponse().getContentAsString()).path("data");
        assertThat(newData.path("refreshToken").asText()).isNotEqualTo(refreshToken);

        // 旧 refresh 已被 rotation 作废
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        // rotation 后库中仍只有一行(同设备覆盖)
        assertThat(refreshTokenMapper.selectCount(new LambdaQueryWrapper<>())).isEqualTo(1L);
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        JsonNode data = loginOk("admin", "admin123");
        String refreshToken = data.path("refreshToken").asText();

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 登出后 refresh 行已删除,无法再刷新
        assertThat(refreshTokenMapper.selectCount(new LambdaQueryWrapper<>())).isZero();
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void multiDeviceRefreshRowsCoexist() throws Exception {
        // 设备 A
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\",\"deviceId\":\"web\"}"))
                .andExpect(status().isOk());
        // 设备 B
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\",\"deviceId\":\"mobile\"}"))
                .andExpect(status().isOk());

        // 同一用户两个设备各自一行 refresh token
        assertThat(refreshTokenMapper.selectCount(null)).isEqualTo(2L);
    }

    @Test
    void locksAfterMaxFailures() throws Exception {
        // 阈值 5:前 5 次普通失败
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("admin", "bad" + i)))
                    .andExpect(jsonPath("$.code").value(500));
        }
        // 第 6 次即使密码正确也被锁定拦截
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("admin", "admin123")))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("锁定")));
    }
}
