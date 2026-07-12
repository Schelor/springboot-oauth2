package com.example.springboot.oauth2.server.controller;

import com.example.springboot.oauth2.server.service.ApiCallService;
import com.example.springboot.oauth2.server.service.FeignCallService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DemoController {

    private final ApiCallService apiCallService;
    private final FeignCallService feignCallService;

    /**
     * 原有方式：JDK HTTPClient + RestClient（命令式）
     * 手动获取 token、手动拼 URL、手动写 header
     *
     * 示例：GET http://localhost:8082/call-hello?name=Alice&age=25
     */
    @GetMapping("/call-hello")
    public Map<String, Object> callHello(@RequestParam Map<String, String> params) {
        return apiCallService.callHello(params);
    }

    /**
     * 新方式：OpenFeign（声明式）
     * token 注入、URL 拼接、序列化全部由框架完成，业务代码零 HTTP 细节
     *
     * 示例：GET http://localhost:8082/feign-call-hello?name=Alice&age=25
     */
    @GetMapping("/feign-call-hello")
    public Map<String, Object> feignCallHello(@RequestParam Map<String, String> params) {
        return feignCallService.callHello(params);
    }
}
