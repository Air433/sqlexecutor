package com.dsb.sqlexecutor.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@Repository
public class SqlExecutorRepository {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public SqlExecutorRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // 切换数据源
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // 执行查询语句
    public List<Map<String, Object>> executeQuery(String sql) {
        return jdbcTemplate.queryForList(sql);
    }

    // 执行更新语句（INSERT/UPDATE/DELETE）
    public int executeUpdate(String sql) {
        return jdbcTemplate.update(sql);
    }

    // 获取数据库列表
    // 获取SQL Server中的所有数据库
    public List<String> getDatabases() {
        return jdbcTemplate.queryForList("SELECT name FROM sys.databases", String.class);
    }

    // 创建新的数据源
    public DataSource createDataSource(String url, String username, String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }
}