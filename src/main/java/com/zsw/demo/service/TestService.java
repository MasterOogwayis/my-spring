package com.zsw.demo.service;

import com.zsw.demo.mvcframework.annotation.ZService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ZhangShaowei on 2019/3/25 13:37
 **/
@Slf4j
@ZService
public class TestService implements ITestService {


    @Override
    public void sayHello(String name) {
        System.out.println("Hello " + name);
    }



}
