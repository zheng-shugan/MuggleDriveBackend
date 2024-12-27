package com.muggle.annotation;

import java.lang.annotation.*;
import org.springframework.web.bind.annotation.Mapping;

@Mapping
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface GlobalInterceptor {

  /**
   * 校验参数
   *
   * @return
   */
  boolean checkParam() default false;

  /**
   * 校验是否需要登录
   *
   * @return
   */
  boolean checkLogin() default false;

  /**
   * 校验是否需要管理员
   *
   * @return
   */
  boolean checkAdmin() default false;
}
