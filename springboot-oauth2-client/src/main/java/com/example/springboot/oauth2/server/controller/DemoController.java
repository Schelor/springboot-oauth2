package com.example.springboot.oauth2.server.controller;

import com.example.springboot.oauth2.server.service.ApiCallService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DemoController {

    private final ApiCallService apiCallService;

    /**
     * 触发完整的 client_credentials 认证流程：
     * 1. 向 AuthServer 申请 JWT access_token
     * 2. 携带 token 调用 Resource Server 的 /hello 接口
     * 3. 将 Resource Server 返回的 JSON 原样透传给调用方
     *
     * 示例：GET http://localhost:8082/call-hello?name=Alice&age=25
     */
    @GetMapping("/call-hello")
    public Map<String, Object> callHello(@RequestParam Map<String, String> params) {
        return apiCallService.callHello(params);
    }
}
