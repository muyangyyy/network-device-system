package com.network.device;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.network.device.mapper")
public class NetworkDeviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NetworkDeviceApplication.class, args);
    }
}
