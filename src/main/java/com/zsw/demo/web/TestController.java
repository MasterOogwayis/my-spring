package com.zsw.demo.web;

import com.zsw.demo.mvcframework.annotation.ZAutowired;
import com.zsw.demo.mvcframework.annotation.ZController;
import com.zsw.demo.mvcframework.annotation.ZRequestMapping;
import com.zsw.demo.service.ITestService;

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


}
