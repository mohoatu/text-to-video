package com.example.videogen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import com.example.videogen.config.VideogenProperties;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = VideogenProperties.class)
public class VideogenApplication {
    public static void main(String[] args) {
        SpringApplication.run(VideogenApplication.class, args);
    }
}
