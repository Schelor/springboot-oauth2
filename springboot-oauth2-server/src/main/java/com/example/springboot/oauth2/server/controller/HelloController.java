package com.example.springboot.oauth2.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HelloController {

    /**
     * 受 OAuth2 保护的接口：将客户端传入的所有 Query 参数原样以 JSON 返回。
     * 调用者必须在 Header 中携带有效的 Bearer Token，否则返回 401。
     */
    @GetMapping("/hello")
    public Map<String, String> hello(@RequestParam Map<String, String> params) {
        return params;
    }
}
