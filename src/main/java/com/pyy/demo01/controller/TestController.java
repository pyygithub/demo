package com.pyy.demo01.controller;

import com.alibaba.fastjson.JSON;
import com.pyy.demo01.aes.SecurityParameter;
import com.pyy.demo01.vo.Person;
import org.springframework.web.bind.annotation.*;

@RestController
public class TestController {
    /*
     * 测试返回数据，会自动加密
     * @return
     */
    @GetMapping("/get")
    @SecurityParameter
    public Person get() {
        Person person = new Person();
        person.setId("123");
        person.setUsername("admin");
        person.setTel("1345312231");
        return person;
    }
    /*
     * 自动解密，并将返回信息加密
     * @param info
     * @return
     */
    @RequestMapping("/save")
    @SecurityParameter
    public Object save(@RequestBody Person info) {
        System.out.println(JSON.toJSON(info));
        return info;
    }

}
