package io.github.hhhrrr777.jfast.baseline.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.hhhrrr777.jfast.baseline.system.domain.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 按用户ID联查其所有启用角色可见的菜单(角色停用/逻辑删除的不算)。
     * 超管不走此查询,直接取全量。
     */
    @Select("""
            SELECT DISTINCT m.* FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.menu_id
            JOIN sys_user_role ur ON ur.role_id = rm.role_id
            JOIN sys_role r ON r.role_id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND m.status = '0' AND r.status = '0' AND r.del_flag = '0'
            ORDER BY m.parent_id, m.order_num
            """)
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);
}
