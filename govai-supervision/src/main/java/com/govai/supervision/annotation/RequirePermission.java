package com.govai.supervision.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口权限码校验注解
 * 标注在 Controller 方法上，指定访问该接口所需的权限码（sys_permission.permission_code）。
 * ADMIN 角色内置放行；其余角色需拥有对应权限码。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /** 所需权限码，如 "user:add" */
    String value();
}
