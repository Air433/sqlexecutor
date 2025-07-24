package com.dsb.sqlexecutor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class SqlExecutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqlExecutorApplication.class, args);
    }
}