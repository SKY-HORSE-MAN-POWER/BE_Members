package com.leeforgiveness.memberservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@EnableCaching
@SpringBootApplication
@RefreshScope
@EnableDiscoveryClient
public class MemberserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemberserviceApplication.class, args);
    }

}
