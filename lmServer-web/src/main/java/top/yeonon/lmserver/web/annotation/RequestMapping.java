package top.yeonon.lmserver.web.annotation;


import top.yeonon.lmserver.web.http.LmRequest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**请求映射
 * @Author yeonon
 * @date 2018/5/23 0023 18:22
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequestMapping {

    /**
     * 请求路径
     * @return
     */
    String[] value() default {};

    /**
     * 请求方法
     * @return
     */
    LmRequest.LMHttpMethod method() default LmRequest.LMHttpMethod.GET;
}
