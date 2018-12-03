package top.yeonon.lmserver.web.databind;

import org.apache.log4j.Logger;
import top.yeonon.lmserver.web.http.LmWebRequest;
import top.yeonon.lmserver.web.process.WebBeanProcessor;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;


/**
 * 基于Java8 -parameters编译参数 的参数绑定策略（但目前还没想到什么好办法来自动选择策略）
 * @Author yeonon
 * @date 2018/11/25 0025 14:01
 **/
public class PrimevalParamStrategy extends AbstractParamBindStrategy {

    private static final Logger log = Logger.getLogger(PrimevalParamStrategy.class);

    protected static final String REQUEST_TYPE_NAME = "LmRequest";

    protected static final String RESPONSE_TYPE_NAME = "LmResponse";

    public static final PrimevalParamStrategy INSTANCE = new PrimevalParamStrategy();

    private PrimevalParamStrategy() {}

    /**
     * 如果Java8在编译的时候加入了-parameters参数，那么反射可以直接获得用户编写的参数名
     * 故不需要特殊处理参数名
     * @param args 参数数组
     * @param paramSize 参数长度
     * @param webRequest 包装后的请求
     * @param method 方法实例
     * @param instance 该方法所在的类实例
     */
    @Override
    protected void putParams(Object[] args, int paramSize, LmWebRequest webRequest, Method method, Object instance) {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < paramSize; i++) {

            //先处理两个特殊的对象 request和response
            String typeName = parameters[i].getType().getTypeName();
            if (typeName.equals(REQUEST_TYPE_NAME)) {
                args[i] = webRequest.getLmRequest();
                continue;
            } else if (typeName.equals(RESPONSE_TYPE_NAME)){
                args[i] = webRequest.getLmResponse();
                continue;
            }

            StringBuilder builder = new StringBuilder(typeName);
            //为了复用typeNameEnum，这里还需要修改一下从Java反射中得到的字段typename
            String newTypeName = builder.insert(0,'L').append(";").toString().replaceAll("\\.","/");
            TypeNameEnum typeNameEnum = TypeNameEnum.getType(newTypeName);
            if (typeNameEnum == null) {
                Class<?> type = WebBeanProcessor.getClassType(typeName);
                args[i] = processObjectParam(type, webRequest.getLmRequest());
            } else {
                //获取参数名称
                String paramName = parameters[i].getName();
                args[i] = typeNameEnum.handle(paramName, webRequest.getLmRequest());
            }
        }
    }
}
