package top.yeonon.lmserver.filter;

import top.yeonon.lmserver.http.LmRequest;
import top.yeonon.lmserver.http.LmResponse;

/**
 * @Author yeonon
 * @date 2018/5/24 0024 16:12
 **/
public interface LmFilter extends Comparable<LmFilter> {

    /**
     * 在业务逻辑之前
     * @param request 请求
     */
    void before(LmRequest request);

    /**
     * 在业务逻辑之后
     * @param response 响应
     */
    void after(LmResponse response);
}
