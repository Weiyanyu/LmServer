package top.yeonon.lmserver.web.databind;

import org.apache.log4j.Logger;
import top.yeonon.lmserver.web.http.LmRequest;
import top.yeonon.lmserver.web.http.LmWebRequest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 抽象参数绑定策略，采用模板方法模式
 * @Author yeonon
 * @date 2018/11/25 0025 14:21
 **/
public abstract class AbstractParamBindStrategy implements ParamBindStrategy {

    private static final Logger log = Logger.getLogger(AbstractParamBindStrategy.class);


    @Override
    public Object[] execute(Method method, Object instance, LmWebRequest webRequest) {
        method.setAccessible(true);
        int paramSize = method.getParameterCount();
        Object[] args = new Object[paramSize];
        putParams(args, paramSize, webRequest, method, instance);
        return args;
    }

    /**
     * 如果参数是引用类型，即对象，那么就采用这个方法处理
     * @param clz 该引用类型的类对象
     * @param request 请求
     * @return 新创建的实例对象
     */
    protected final Object processObjectParam(Class<?> clz, LmRequest request) {
        try {
            Object instance = clz.newInstance();
            for (Field field : clz.getDeclaredFields()) {
                field.setAccessible(true);
                StringBuilder builder = new StringBuilder(field.getType().getName());
                //为了复用typeNameEnum，这里还需要修改一下从Java反射中得到的字段typename
                String newTypeName = builder.insert(0,'L').append(";").toString().replaceAll("\\.","/");
                TypeNameEnum typeNameEnum = TypeNameEnum.getType(newTypeName);
                if (typeNameEnum == null) {
                    //如果还是对象，即typeNameEnum里没有包含的，那么就递归调用processObjectParam
                    field.set(instance, processObjectParam(field.getType(), request));
                    return instance;
                }
                //为刚刚构造出来的实例设置对象
                field.set(instance, typeNameEnum.handle(field.getName(), request));
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException e) {
            log.error(e.getMessage());
        }
        return null;
    }


    protected abstract void putParams(Object[] args, int paramSize, LmWebRequest request, Method method, Object instance);

}
