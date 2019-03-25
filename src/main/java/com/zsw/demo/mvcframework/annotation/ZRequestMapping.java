package com.zsw.demo.mvcframework.annotation;


import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ZRequestMapping {

    String value() default "";

}
