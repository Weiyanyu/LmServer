package top.yeonon.lmserver.web.annotation;

import top.yeonon.lmserver.core.annotation.Bean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Filter过滤器
 * @Author yeonon
 * @date 2018/5/24 0024 16:23
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Bean
public @interface Filter {
    String[] value();

    int order() default 0;

}
