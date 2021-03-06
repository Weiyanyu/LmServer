package top.yeonon.lmserver.core.ioc;

import org.apache.log4j.Logger;
import top.yeonon.lmserver.core.annotation.Autowire;
import top.yeonon.lmserver.core.annotation.Bean;
import top.yeonon.lmserver.core.annotation.Configuration;
import top.yeonon.lmserver.core.utils.ClassUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * BeanProcessor抽象基类，包含共用的方法
 * @Author yeonon
 * @date 2018/5/29 0029 21:27
 **/
public abstract class AbstractBeanProcessor implements BeanProcessor {

    private static final Logger log = Logger.getLogger(AbstractBeanProcessor.class);


    //bean maps
    private static final Map<Class<?>, Object> beanMaps = new HashMap<>();

    //class map
    private static final Map<String, Class<?>> classMaps = new HashMap<>();

    /**
     * 处理所有的Bean
     *
     * @param packageName 包名
     */
    @Override
    public void beanProcessor(String packageName, boolean isMultiThread) {
        //获取该包下的所有类
        Set<Class<?>> classSets = ClassUtil.getClassFromPackage(packageName, isMultiThread);

        try {
            for (Class<?> clz : classSets) {
                if (clz != null) {
                    classMaps.put(clz.getTypeName(), clz);
                }
                //如果该类上没有注解，或者clz为null（有可能）就表明不需要往下执行逻辑
                if (clz != null && clz.getAnnotations().length != 0) {
                    //先判断该类是否是一个配置类，配置类和普通的组件分开处理
                    if (clz.isAnnotationPresent(Configuration.class)) {
                        processConfigBean(clz);
                    } else {
                        //如果是普通的组件，那么就做对应的处理
                        Annotation[] annotations = clz.getAnnotations();
                        for (Annotation annotation : annotations) {
                            if (annotation.annotationType().isAnnotationPresent(Bean.class)) {
                                beanMaps.put(clz, clz.newInstance());
                            }
                        }
                    }
                }
            }

            //处理依赖注入
            beanMaps.forEach(this::processBeanWire);



        } catch (IllegalAccessException | InstantiationException e) {
            log.error(e.toString());
        }
    }

    private void processConfigBean(Class<?> clz) {
        try {
            //加载Config类，并放入到BeanMaps中（逻辑上来说，Config类也是Bean，但实际上是分开处理）
            //这样处理的目的是方便处理Config类里的Bean
            Object configInstance = clz.newInstance();
            if (beanMaps.get(clz) == null) {
                beanMaps.put(clz, configInstance);
                log.info("load configuration : " + clz);
            } else {
                log.info("this configuration have been loaded ： " + clz);
            }

            //加载有Bean注解的方法（暂时仅支持注解在方法上的Bean）
            Method[] methods = clz.getDeclaredMethods();
            for (Method method : methods) {
                Object returnInstance;
                //这里要调用这个方法，因为用户会对方法做一些配置
                //TODO 暂时不支持配置Bean的方法上有参数，还没有想好如何做，现在已经没有灵感了 ^_^ （2018年6月26日21:20:32）
                returnInstance = method.invoke(configInstance);
                Class<?> returnType = returnInstance.getClass();
                if (beanMaps.get(returnType) == null) {
                    beanMaps.put(returnType, returnInstance);
                    log.info("load Bean ： " + returnType);
                } else {
                    log.info("this Bean has been loaded : " + returnType);
                }
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * 实现依赖注入
     *
     * @param clz          类
     * @param beanInstance 类实例
     */
    private void processBeanWire(Class<?> clz, Object beanInstance) {
        try {
            if (clz.getAnnotations() != null) {
                Annotation[] annotations = clz.getAnnotations();
                for (Annotation annotation : annotations) {
                    //判断这个类上的注解是否也是Component(注解上可以有注解)
                    if (annotation.annotationType().isAnnotationPresent(Bean.class)) {
                        Field[] fields = clz.getDeclaredFields();
                        //获取所有字段
                        for (Field field : fields) {
                            if (field.isAnnotationPresent(Autowire.class)) {
                                field.setAccessible(true);
                                Class<?> fieldClass = field.getType();

                                if (getBeanMaps().get(fieldClass) != null) {
                                    //如果容器中存在这个字段的类，则将其赋值
                                    field.set(beanInstance, getBeanMaps().get(fieldClass));
                                }
                            }
                        }
                    }
                }
            }
        } catch (IllegalAccessException e) {
            log.error(e.getCause().toString());
        }
    }


    /**
     * 子类处理Bean的具体逻辑
     */
    protected abstract void processBean(boolean isMultiThread);


    public Map<Class<?>, Object> getBeanMaps() {
        return beanMaps;
    }

    public Map<String, Class<?>> getType() {
        return classMaps;
    }
}
