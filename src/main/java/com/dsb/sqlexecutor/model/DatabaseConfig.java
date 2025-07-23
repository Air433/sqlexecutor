package com.dsb.sqlexecutor.model;

import jakarta.validation.constraints.NotEmpty;

public class DatabaseConfig {

    @NotEmpty(message = "数据库名称不能为空")
    private String databaseName;

    @NotEmpty(message = "用户名不能为空")
    private String username;

    @NotEmpty(message = "密码不能为空")
    private String password;

    private String host = "localhost";
    private int port = 1433;
    private String driverClass = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    private String jdbcUrl;

    // Getter和Setter方法
    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    public String getJdbcUrl() {
        // 如果用户没有手动设置JDBC URL，则自动生成
        if (jdbcUrl == null) {
            return generateJdbcUrl();
        }
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    // 生成SQL Server的JDBC连接URL
    private String generateJdbcUrl() {
        StringBuilder url = new StringBuilder();
        url.append("jdbc:sqlserver://")
                .append(host)
                .append(":")
                .append(port)
                .append(";databaseName=")
                .append(databaseName);

        // 添加SQL Server特有的参数
        url.append(";encrypt=true;trustServerCertificate=true");

        return url.toString();
    }

    public String generateJdbcUrl(String databaseName) {
        StringBuilder url = new StringBuilder();
        url.append("jdbc:sqlserver://")
                .append(host)
                .append(":")
                .append(port)
                .append(";databaseName=")
                .append(databaseName);

        // 添加SQL Server特有的参数
        url.append(";encrypt=true;trustServerCertificate=true");

        return url.toString();
    }
}