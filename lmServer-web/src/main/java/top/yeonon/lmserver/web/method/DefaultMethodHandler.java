package top.yeonon.lmserver.web.method;


import top.yeonon.lmserver.web.databind.ASMParamBindStrategy;
import top.yeonon.lmserver.web.http.LmRequest;
import top.yeonon.lmserver.web.http.LmResponse;
import top.yeonon.lmserver.web.http.LmWebRequest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * 处理请求的类
 * <p>
 * Ps： 一个类可能有多个处理Url的方法，即一个classInstance可能有多个method，这种情况也会有多个handler，
 * 但是他们的method字段不同(classInstance相同)。
 *
 * @Author yeonon
 * @date 2018/5/23 0023 18:24
 **/
public class DefaultMethodHandler implements MethodHandler {
    //类的一个实例
    private Object classInstance;

    //映射的方法
    private Method method;

    /**
     * @param classInstance 实例
     * @param method        处理映射的方法
     */
    public DefaultMethodHandler(Object classInstance, Method method) {
        this.classInstance = classInstance;
        this.method = method;
    }

    @Override
    public Object execute(LmRequest request, LmResponse response)
            throws InvocationTargetException, IllegalAccessException {
        Object res = null;
        Object[] args;
        //TODO 这里有个问题，当用户使用框架的时候，无法得知是否有用到-parameters参数，现在暂时先搁置，后面再回来做
        args = ASMParamBindStrategy.INSTANCE.execute(method, classInstance, new LmWebRequest(request, response));
        res = this.method.invoke(this.classInstance, args);
        return res;
    }
}
