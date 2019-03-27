package com.zsw.demo.web;

import com.zsw.demo.mvcframework.annotation.ZAutowired;
import com.zsw.demo.mvcframework.annotation.ZController;
import com.zsw.demo.mvcframework.annotation.ZRequestMapping;
import com.zsw.demo.service.ITestService;

import javax.servlet.http.HttpServletRequest;

/**
 * @author ZhangShaowei on 2019/3/25 13:36
 **/
@ZController
@ZRequestMapping("test")
public class TestController {

    @ZAutowired
    private ITestService testService;

    @ZRequestMapping("name")
    public String test(String name) {
        this.testService.sayHello(name);
        return name;
    }

    @ZRequestMapping("zsw")
    public String test(HttpServletRequest request) {
        String str = request.getParameter("zsw");
        this.testService.sayHello(str);
        return str;
    }


}
