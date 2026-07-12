package com.example.springboot.oauth2.server.feign;

import feign.QueryMap;
import feign.RequestLine;

import java.util.Map;

/**
 * OpenFeign 声明式接口：定义对 Resource Server 的调用契约。
 *
 * <p>只需声明意图（HTTP 方法 + 路径 + 参数），不写任何 HTTP 实现代码。
 * Feign 框架在运行时生成代理，处理序列化、连接等细节。</p>
 *
 * <p>对比 ApiCallService (RestClient)：
 * <ul>
 *   <li>RestClient：手动 UriComponentsBuilder 拼 URL、手动写 header、手动调用 retrieve()</li>
 *   <li>Feign：只需一个 @RequestLine 注解，URL 参数、header 注入全部由框架完成</li>
 * </ul>
 * </p>
 */
public interface ResourceServerClient {

    /**
     * 调用 Resource Server 的 GET /hello 接口。
     *
     * @param params 任意 query 参数（如 name=Alice&age=25），由 @QueryMap 自动拼入 URL
     * @return Resource Server 返回的 JSON，反序列化为 Map
     */
    @RequestLine("GET /hello")
    Map<String, Object> hello(@QueryMap Map<String, String> params);
}