package top.yeonon.lmserver.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 配置类
 * @Author yeonon
 * @date 2018/6/26 0026 18:04
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Configuration {
    String value() default "";
}
