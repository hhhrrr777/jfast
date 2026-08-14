package ${project.basePackage}.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
<#if project.database == "mysql">        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
<#elseif project.database == "postgresql">        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
<#elseif project.database == "dm">        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.DM));
<#elseif project.database == "kingbase">        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.KINGBASE_ES));
<#elseif project.database == "opengauss">        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
</#if>        return interceptor;
    }
}
