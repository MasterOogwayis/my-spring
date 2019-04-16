package com.zsw.demo.servlet;

import com.zsw.demo.mvcframework.annotation.ZRequestParam;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author ZhangShaowei on 2019/3/27 14:16
 **/
@Getter
@Setter
public class Handler {

    private Pattern pattern;

    private Method method;

    private Object controller;

    private Class<?>[] paramTypes;


    private Map<String, Integer> paramIndexMapping;

    public Handler(Pattern pattern, Method method, Object controller) {
        this.pattern = pattern;
        this.method = method;
        this.controller = controller;
        this.paramTypes = method.getParameterTypes();
        paramIndexMapping = new HashMap<>();
        this.analysisParamIndex();
    }


    public Object invoke(Object[] paramValues) throws InvocationTargetException, IllegalAccessException {
        return this.method.invoke(this.controller, paramValues);
    }


    private void analysisParamIndex() {

        // 多个参数，多个注解，所以是个二位数组
        Annotation[][] annotations = this.method.getParameterAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            for (Annotation annotation : annotations[i]) {
                if (annotation instanceof ZRequestParam) {
                    // 注意啊，jdk无法直接获取参数名称，spring是通过 asm 操作字节码获取的。所以最好吧名字填上去
                    String paramName = ((ZRequestParam) annotation).name();
                    if (StringUtils.isNoneEmpty(paramName)) {
                        this.paramIndexMapping.put(paramName, i);
                    }
                }
            }
        }

        //提取方法中的request和response参数
        Class<?>[] paramsTypes = this.method.getParameterTypes();
        for (int i = 0; i < paramsTypes.length; i++) {
            Class<?> type = paramsTypes[i];
            if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
                this.paramIndexMapping.put(type.getName(), i);
            }
        }

    }


}
