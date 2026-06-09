package com.wallrunner.client.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Spring DI 配置类。
 *
 * 职责：
 * - 扫描 com.wallrunner.client 包下的所有 Spring Bean。
 * - 加载 classpath 下的 client.properties 外部配置。
 *
 * 设计原则：
 * - 与 server 端统一使用 Spring 构造器注入，消除硬编码单例。
 */
@Configuration
@ComponentScan(basePackages = "com.wallrunner.client")
@PropertySource("classpath:client.properties")
public class ClientSpringConfig {
}
