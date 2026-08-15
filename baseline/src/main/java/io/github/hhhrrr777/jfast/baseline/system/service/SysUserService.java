package io.github.hhhrrr777.jfast.baseline.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.hhhrrr777.jfast.baseline.auth.model.LoginUser;
import io.github.hhhrrr777.jfast.baseline.auth.service.RefreshTokenService;
import io.github.hhhrrr777.jfast.baseline.common.core.TableDataVO;
import io.github.hhhrrr777.jfast.baseline.common.exception.ServiceException;
import io.github.hhhrrr777.jfast.baseline.system.domain.SysUser;
import io.github.hhhrrr777.jfast.baseline.system.domain.SysUserRole;
import io.github.hhhrrr777.jfast.baseline.system.dto.ResetPasswordDTO;
import io.github.hhhrrr777.jfast.baseline.system.dto.UserCreateDTO;
import io.github.hhhrrr777.jfast.baseline.system.dto.UserUpdateDTO;
import io.github.hhhrrr777.jfast.baseline.system.mapper.SysUserMapper;
import io.github.hhhrrr777.jfast.baseline.system.mapper.SysUserRoleMapper;
import io.github.hhhrrr777.jfast.baseline.system.vo.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户管理应用服务:分页查询、增删改查、启用禁用、重置密码、角色绑定。
 * admin 种子账号(userId=1)禁止修改与删除。
 */
@Service
public class SysUserService {

    /** 种子超管账号固定 userId(SeedDataInitializer 首插,自增从 1 起)。 */
    public static final long ADMIN_USER_ID = 1L;

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public SysUserService(SysUserMapper userMapper,
                          SysUserRoleMapper userRoleMapper,
                          PasswordEncoder passwordEncoder,
                          RefreshTokenService refreshTokenService) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * 分页查询:按账号/昵称模糊、状态精确。
     */
    public TableDataVO<UserVO> list(long pageNum, long pageSize, String userName, String nickName, String status) {
        Page<SysUser> page = userMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysUser>()
                        .like(!isBlank(userName), SysUser::getUserName, userName)
                        .like(!isBlank(nickName), SysUser::getNickName, nickName)
                        .eq(!isBlank(status), SysUser::getStatus, status)
                        .orderByAsc(SysUser::getUserId));
        List<UserVO> rows = page.getRecords().stream().map(this::toVO).toList();
        return TableDataVO.of(page.getTotal(), rows);
    }

    /**
     * 详情(含绑定角色)。
     */
    public UserVO getById(long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        return toVO(user);
    }

    /**
     * 新增:账号唯一校验 → 落库 → 绑角色。密码 BCrypt 编码。
     */
    @Transactional
    public UserVO create(UserCreateDTO dto, String operator) {
        checkUserNameUnique(dto.getUserName(), null);
        SysUser user = new SysUser();
        user.setUserName(dto.getUserName());
        user.setNickName(dto.getNickName());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setStatus(defaultStatus(dto.getStatus()));
        user.setCreateBy(operator);
        user.setCreateTime(LocalDateTime.now());
        user.setRemark(dto.getRemark());
        userMapper.insert(user);
        replaceRoles(user.getUserId(), dto.getRoleIds());
        return getById(user.getUserId());
    }

    /**
     * 修改:昵称/状态/备注/角色;停用时吊销全部 refresh token(即时踢下线)。
     */
    @Transactional
    public UserVO update(UserUpdateDTO dto, String operator) {
        checkAdminProtected(dto.getUserId(), "不允许修改种子管理员账号");
        SysUser exists = userMapper.selectById(dto.getUserId());
        if (exists == null) {
            throw new ServiceException("用户不存在");
        }
        SysUser user = new SysUser();
        user.setUserId(dto.getUserId());
        user.setNickName(dto.getNickName());
        user.setStatus(dto.getStatus());
        user.setUpdateBy(operator);
        user.setUpdateTime(LocalDateTime.now());
        user.setRemark(dto.getRemark());
        userMapper.updateById(user);
        if (dto.getRoleIds() != null) {
            replaceRoles(dto.getUserId(), dto.getRoleIds());
        }
        if ("1".equals(dto.getStatus()) && !"1".equals(exists.getStatus())) {
            refreshTokenService.revokeAllForUser(dto.getUserId());
        }
        return getById(dto.getUserId());
    }

    /**
     * 删除:物理删除(账号唯一键与逻辑删除冲突);级联删角色关联与 refresh token。
     * 不允许删除种子 admin 与当前登录用户自己。
     */
    @Transactional
    public void deleteByIds(List<Long> userIds, Long currentUserId) {
        for (Long userId : userIds) {
            checkAdminProtected(userId, "不允许删除种子管理员账号");
            if (currentUserId != null && currentUserId.equals(userId)) {
                throw new ServiceException("当前登录用户不能删除自己");
            }
        }
        for (Long userId : userIds) {
            userMapper.deleteById(userId);
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId, userId));
            refreshTokenService.revokeAllForUser(userId);
        }
    }

    /**
     * 重置密码:吊销全部 refresh token(其他端强制重登)。
     */
    @Transactional
    public void resetPassword(ResetPasswordDTO dto, String operator) {
        checkAdminProtected(dto.getUserId(), "不允许重置种子管理员密码,请登录后自行修改");
        SysUser exists = userMapper.selectById(dto.getUserId());
        if (exists == null) {
            throw new ServiceException("用户不存在");
        }
        SysUser user = new SysUser();
        user.setUserId(dto.getUserId());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setUpdateBy(operator);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        refreshTokenService.revokeAllForUser(dto.getUserId());
    }

    /**
     * 当前登录用户改自己的密码:需校验旧密码。
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword, String operator) {
        SysUser exists = userMapper.selectById(userId);
        if (exists == null || !passwordEncoder.matches(oldPassword, exists.getPassword())) {
            throw new ServiceException("原密码不正确");
        }
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateBy(operator);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        refreshTokenService.revokeAllForUser(userId);
    }

    private void checkUserNameUnique(String userName, Long excludeUserId) {
        SysUser exists = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserName, userName));
        if (exists != null && (excludeUserId == null || !exists.getUserId().equals(excludeUserId))) {
            throw new ServiceException("登录账号已存在:" + userName);
        }
    }

    private void checkAdminProtected(Long userId, String message) {
        if (userId != null && userId == ADMIN_USER_ID) {
            throw new ServiceException(message);
        }
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            userRoleMapper.insert(new SysUserRole(userId, roleId));
        }
    }

    private String defaultStatus(String status) {
        return isBlank(status) ? "0" : status;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setUserId(user.getUserId());
        vo.setUserName(user.getUserName());
        vo.setNickName(user.getNickName());
        vo.setStatus(user.getStatus());
        vo.setLoginIp(user.getLoginIp());
        vo.setLoginDate(user.getLoginDate());
        vo.setCreateTime(user.getCreateTime());
        vo.setRemark(user.getRemark());
        vo.setRoleIds(userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, user.getUserId()))
                .stream().map(SysUserRole::getRoleId).toList());
        return vo;
    }
}
