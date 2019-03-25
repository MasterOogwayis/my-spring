package com.zsw.demo.servlet;

import com.zsw.demo.mvcframework.annotation.ZAutowired;
import com.zsw.demo.mvcframework.annotation.ZController;
import com.zsw.demo.mvcframework.annotation.ZRequestMapping;
import com.zsw.demo.mvcframework.annotation.ZService;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ZhangShaowei on 2019/3/25 10:15
 **/
@Slf4j
public class ZSWDispatcherServlet extends HttpServlet {
    private static final long serialVersionUID = -1216009112806834418L;

    /**
     * 保存 application.properties 配置文件中的内容
     */
    private Properties properties = new Properties();

    /**
     * 所有扫描到的类名
     */
    private List<String> classNames = new ArrayList<>();

    /**
     * 模拟的IOC容器
     */
    private Map<String, Object> ioc = new ConcurrentHashMap<>(256);

    /**
     * url 和 method关系
     */
    private Map<String, Method> handlerMappings = new HashMap<>();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doDispatch(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doDispatch(req, resp);
    }


    @SneakyThrows
    private void doDispatch(HttpServletRequest request, HttpServletResponse response) {
        String url = request.getRequestURI();
        String contextPath = request.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
        if (!this.handlerMappings.containsKey(url)) {
            response.setStatus(404);
            response.getWriter().write("404 Not Found!");
            return;
        }

        Method method = this.handlerMappings.get(url);
        String beanName = this.toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        Map<String, String[]> map = request.getParameterMap();

        Class<?>[] parameterTypes = method.getParameterTypes();
        Object value = method.invoke(this.ioc.get(beanName), new Object[]{map.get("name")});
        response.getWriter().write(String.valueOf(value));
    }

    @Override
    public void init(ServletConfig config) {
        // 1. 加载配置文件
        loadConfig(config.getInitParameter("contextConfigLocation"));

        // 2. 扫描相关的类
        doScanner(this.properties.getProperty("scanPackage"));

        // 3. 初始化类，放入ioc
        doInstance();

        // 4. 完成依赖注入
        doAutowrite();


        // 5.初始化 Handlermapping
        initHandlermapping();

        log.debug("ZSWDispatcherServlet inited!");

    }

    private void initHandlermapping() {
        if (this.ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : this.ioc.entrySet()) {
            String key = entry.getKey();
            Object object = entry.getValue();
            Class<?> clazz = object.getClass();
            if (!clazz.isAnnotationPresent(ZController.class)) {
                continue;
            }

            // 写在类上面的根路径
            String baseurl = "";
            if (clazz.isAnnotationPresent(ZRequestMapping.class)) {
                baseurl = clazz.getAnnotation(ZRequestMapping.class).value();
            }

            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(ZRequestMapping.class)) {
                    continue;
                }
                ZRequestMapping annotation = method.getAnnotation(ZRequestMapping.class);
                String url = ("/" + baseurl + "/" + annotation.value()).replaceAll("/+", "/");
                this.handlerMappings.put(url, method);
                log.debug("Mapped url={}, method={}", url, method);
            }


        }


    }

    private void doAutowrite() {
        if (this.ioc.isEmpty()) {
            return;
        }

        this.ioc.forEach((key, value) -> {
            Field[] fields = value.getClass().getDeclaredFields();
            try {
                for (Field field : fields) {
                    if (!field.isAnnotationPresent(ZAutowired.class)) {
                        continue;
                    }
                    ZAutowired zAutowired = field.getAnnotation(ZAutowired.class);
                    String beanName = zAutowired.value();
                    if (StringUtils.isBlank(beanName)) {
                        beanName = field.getType().getName();
                    }
                    field.setAccessible(true);
                    field.set(value, this.ioc.get(beanName));

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @SneakyThrows
    private void doInstance() {
        if (this.classNames.isEmpty()) {
            return;
        }
        for (String className : this.classNames) {
            Class<?> clazz = Class.forName(className);
            // 加了注解的类才初始化
            if (clazz.isAnnotationPresent(ZController.class)) {
                Object object = clazz.newInstance();
                String beanName = this.toLowerFirstCase(clazz.getSimpleName());
                this.ioc.put(beanName, object);
            } else if (clazz.isAnnotationPresent(ZService.class)) {
                // 1. 自定义beanName
                ZService service = clazz.getAnnotation(ZService.class);
                String beanName = service.value();
                // 2. 类名首字母小写
                if (StringUtils.isBlank(beanName)) {
                    beanName = this.toLowerFirstCase(clazz.getSimpleName());
                }
                Object object = clazz.newInstance();
                this.ioc.put(beanName, object);
                // 3. 根据类型自动赋值
                for (Class<?> i : clazz.getInterfaces()) {
                    if (this.ioc.containsKey(i.getName())) {
                        throw new Exception(i.getName() + " is already exists!");
                    }
                    // 把接口类型直接当成key
                    this.ioc.put(i.getName(), object);
                }
            } else {
                continue;
            }
        }


    }

    /**
     * @param scanPackage 包路径
     */
    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classPath = new File(url.getFile());
        for (File file : classPath.listFiles()) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                String className = scanPackage + "." + file.getName().replace(".class", "");
                this.classNames.add(className);
            }
        }

    }

    /**
     * 加载配置文件
     *
     * @param contextConfigLocation
     */
    @SneakyThrows
    private void loadConfig(String contextConfigLocation) {
        // 从类路径下找到spring主配置文件所在路径
        @Cleanup InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        this.properties.load(resourceAsStream);


    }


    private String toLowerFirstCase(String string) {
        if (StringUtils.isBlank(string)) {
            return "";
        }
        char c;
        if ((c = string.charAt(0)) < 'a') {
            c += 32;
            return c + string.substring(1);
        } else {
            return string;
        }


    }

}
