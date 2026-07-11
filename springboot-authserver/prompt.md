
你是一个资深的Java技术专家，对Spring生态技术非常熟悉，我现在需要搭建一个AuthServer。
我已经初始化了一个SpringBoot4+Gradle+Java25的工程
具体需求如下：
1.修改依赖,我需要使用SpringBoot管理的jetty作为Web Server
2.我希望能搭建一个基于SpringBoot+SpringSecurity+OAuth2.0+JWT的AuthServer，使用client_credentials模式
3.使用JPA+Hibernate+SQLServer作为数据层
4.设计一个表，用来颁发client_id,client_secret,用来做客户端的请求验证
5.当前工程只做Server端的功能
6.一步步的指导我，你做的每一个功能都需要我确认，然后继续
7.我已经开发了一部分内容，相关代码如下
build.gradle配置如下：
```declarative
plugins {
    id 'java'
    id 'org.springframework.boot' version "${springBootVersion}"
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    maven {
        url 'https://maven.aliyun.com/repository/public/'
    }
    maven {
        url 'https://maven.aliyun.com/repository/spring/'
    }
    mavenCentral()
}

dependencies {
    // 1. 主依赖引入 BOM
    implementation platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")

    // Web 容器：排除 Tomcat，使用 Jetty
    implementation('org.springframework.boot:spring-boot-starter-web') {
        exclude group: 'org.springframework.boot', module: 'spring-boot-starter-tomcat'
    }
    implementation 'org.springframework.boot:spring-boot-starter-jetty'

    // Security & OAuth2 Authorization Server
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.security:spring-security-oauth2-authorization-server'

    // JPA & SQLServer
//    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
//    runtimeOnly 'com.microsoft.sqlserver:mssql-jdbc'
    // JPA & H2 内存数据库 (后续替换为 SQLServer 只需改这里和配置文件)
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'com.h2database:h2'

    // RestClient (保留你的原有配置)
    implementation 'org.springframework.boot:spring-boot-starter-restclient'

    // 2. 为 annotationProcessor 显式引入 BOM
    annotationProcessor platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'

    // 3. 为测试相关的配置显式引入 BOM (以防万一)
    testImplementation platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
    testCompileOnly platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
    testAnnotationProcessor platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-security-test'
    testCompileOnly 'org.projectlombok:lombok'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    testAnnotationProcessor 'org.projectlombok:lombok'
}

tasks.named('test') {
    useJUnitPlatform()
}

```
实体类：SysOAuthClientPO
```declarative

package com.example.demogradle.entity;


import jakarta.persistence.*;
import lombok.*;

/**
 * 自定义 OAuth2 客户端实体类
 * 对应数据库表：sys_oauth_client
 */
@Data
@Entity
@Table(name = "sys_oauth_client")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysOAuthClientPO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 客户端ID (对应请求时的 client_id)
     * 必须唯一
     */
    @Column(nullable = false, unique = true, length = 100)
    private String clientId;

    /**
     * 客户端密文 (对应请求时的 client_secret)
     * 【重要】Spring Authorization Server 强制要求此字段存储的是 BCrypt 加密后的密文！
     */
    @Column(nullable = false, length = 200)
    private String clientSecret;

    /**
     * 授权范围，多个逗号分隔 (例如: "read,write")
     * 在 client_credentials 模式下，代表该客户端能访问的资源范围
     */
    @Column(length = 500)
    private String scopes;

    /**
     * 客户端描述/名称，方便在 H2 控制台里辨认
     */
    @Column(length = 255)
    private String description;
}

```
