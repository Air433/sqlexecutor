package com.dsb.sqlexecutor.controller;

import com.dsb.sqlexecutor.model.DatabaseConfig;
import com.dsb.sqlexecutor.service.SqlExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/")
public class SqlExecutorController {

    private Logger log = LoggerFactory.getLogger(SqlExecutorController.class);

    @Autowired
    private SqlExecutorService sqlExecutorService;

    // 首页
    @GetMapping
    public String index(Model model) {
        model.addAttribute("sql", "");
        model.addAttribute("result", null);
        model.addAttribute("databaseConfig", new DatabaseConfig());
        model.addAttribute("databaseConfigs", sqlExecutorService.getAllDatabaseConfigs());
        model.addAttribute("databases", sqlExecutorService.getDatabases());
        model.addAttribute("currentDatabase", sqlExecutorService.getCurrentDatabase());
        return "index";
    }

    // 执行SQL语句
    // 执行SQL语句
    @PostMapping("/execute")
    public String executeSql(@RequestParam String sql,
                             @RequestParam(required = false) String database,
                             Model model) {
        // 切换数据库（如果指定）
        if (database != null && !database.isEmpty()) {
            try {
                sqlExecutorService.switchDatabase(database);
            } catch (Exception e) {
                model.addAttribute("error", "切换数据库失败: " + e.getMessage());
            }
        }

        // 执行SQL
        try {
            sql = sql.trim();
            if (sql.toLowerCase().startsWith("select") ||
                    sql.toLowerCase().startsWith("show") ||
                    sql.toLowerCase().startsWith("desc")) {
                // 查询语句
                List<Map<String, Object>> result = sqlExecutorService.executeQuery(sql);
                // 处理结果，确保是普通的Map
                List<Map<String, Object>> processedResult = new ArrayList<>();
                for (Map<String, Object> row : result) {
                    processedResult.add(new LinkedHashMap<>(row));
                }
                model.addAttribute("result", processedResult);
                model.addAttribute("message", "查询成功，返回 " + result.size() + " 条记录");
            } else {
                // 更新语句
                int rowsAffected = sqlExecutorService.executeUpdate(sql);
                model.addAttribute("message", "操作成功，影响行数: " + rowsAffected);
            }
        } catch (Exception e) {
            model.addAttribute("error", "执行SQL出错: " + e.getMessage());
            log.error("执行SQL出错", e);
        }

        // 填充页面数据
        model.addAttribute("sql", sql);
        model.addAttribute("databaseConfig", new DatabaseConfig());
        model.addAttribute("databaseConfigs", sqlExecutorService.getAllDatabaseConfigs());
        model.addAttribute("databases", sqlExecutorService.getDatabases());
        model.addAttribute("currentDatabase", sqlExecutorService.getCurrentDatabase());

        return "index";
    }

    // 添加数据库配置
    @PostMapping("/add-database")
    public String addDatabase(@Valid @ModelAttribute DatabaseConfig databaseConfig,
                              BindingResult bindingResult,
                              Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("sql", "");
            model.addAttribute("result", null);
            model.addAttribute("databaseConfigs", sqlExecutorService.getAllDatabaseConfigs());
            model.addAttribute("databases", sqlExecutorService.getDatabases());
            return "index";
        }

        try {
            // 使用数据库名称作为配置名称
            sqlExecutorService.addDatabaseConfig(databaseConfig.getDatabaseName(), databaseConfig);
            model.addAttribute("message", "数据库配置添加成功");
        } catch (Exception e) {
            model.addAttribute("error", "添加数据库配置失败: " + e.getMessage());
        }

        // 填充页面数据
        model.addAttribute("sql", "");
        model.addAttribute("result", null);
        model.addAttribute("databaseConfig", databaseConfig!=null?databaseConfig:new DatabaseConfig());
        model.addAttribute("databaseConfigs", sqlExecutorService.getAllDatabaseConfigs());
        model.addAttribute("databases", sqlExecutorService.getDatabases());

        return "index";
    }

    // 切换数据库
    @GetMapping("/switch-database/{name}")
    public String switchDatabase(@PathVariable String name, Model model) {
        try {
            sqlExecutorService.switchDatabase(name);
            model.addAttribute("message", "已切换到数据库: " + name);
        } catch (Exception e) {
            model.addAttribute("error", "切换数据库失败: " + e.getMessage());
        }

        // 填充页面数据
        model.addAttribute("sql", "");
        model.addAttribute("result", null);
        model.addAttribute("databaseConfig",  sqlExecutorService.getCurrentDatabaseConfig());
        model.addAttribute("databaseConfigs", sqlExecutorService.getAllDatabaseConfigs());
        model.addAttribute("databases", sqlExecutorService.getDatabases());
        model.addAttribute("currentDatabase", sqlExecutorService.getCurrentDatabase());

        return "index";
    }


    // 测试数据库连接并获取数据库列表
    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody Map<String, String> request) {
        Map result = new HashMap<>();
        try {
            String host = request.get("host");
            String portStr = request.get("port");
            String username = request.get("username");
            String password = request.get("password");

            // 解析端口
            int port = 1433; // 默认端口
            if (portStr != null && !portStr.isEmpty()) {
                port = Integer.parseInt(portStr);
            }

            // 创建临时连接
            String jdbcUrl = "jdbc:sqlserver://" + host + ":" + port+ ";encrypt=true;trustServerCertificate=true";
            DataSource dataSource = createDataSource(jdbcUrl, username, password);

            // 测试连接
            try (Connection connection = dataSource.getConnection()) {
                // 获取数据库列表
                List<String> databases = getDatabases(connection);

                result.put("success", true);
                result.put("databases", databases);
                return ResponseEntity.ok(result);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }


    }

    // 保存数据库配置
    @PostMapping("/save-connection")
    public ResponseEntity<Map<String, Object>> saveConnection(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            String host = request.get("host");
            String portStr = request.get("port");
            String username = request.get("username");
            String password = request.get("password");
            String database = request.get("database");

            // 解析端口
            int port = 1433; // 默认端口
            if (portStr != null && !portStr.isEmpty()) {
                port = Integer.parseInt(portStr);
            }

            // 生成连接名称
            String connectionName = host + "_" + database;

            // 创建数据库配置
            DatabaseConfig config = new DatabaseConfig();
            config.setDatabaseName(database);
            config.setUsername(username);
            config.setPassword(password);
            config.setJdbcUrl("jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + database + ";encrypt=true;trustServerCertificate=true");

            // 保存配置
            sqlExecutorService.addDatabaseConfig(connectionName, config);

            result.put("connectionName", connectionName);
            result.put("success", true);
            result.put("message", "数据库配置已保存");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    // 创建数据源
    private DataSource createDataSource(String url, String username, String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    // 获取数据库列表
    private List<String> getDatabases(Connection connection) throws SQLException {
        List<String> databases = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sys.databases")) {

            while (rs.next()) {
                databases.add(rs.getString("name"));
            }
        }
        return databases;
    }

    @GetMapping("/table-metadata")
    public ResponseEntity<List<Map<String, Object>>> getTableMetadata() {
        try {
            List<Map<String, Object>> metadata = sqlExecutorService.getTableMetadata();
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            log.error("获取表元数据出错", e);
            return ResponseEntity.badRequest().body(null);
        }
    }


    // 在 SqlExecutorController 中添加
    @GetMapping("/column-metadata")
    public ResponseEntity<List<Map<String, Object>>> getColumnMetadata() {
        try {
            // 查询 INFORMATION_SCHEMA.COLUMNS 获取列信息
            List<Map<String, Object>> columns = sqlExecutorService.getColumnMetadata();
            return ResponseEntity.ok(columns);
        } catch (Exception e) {
            log.error("获取列元数据出错", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/metadata")
    public ResponseEntity<Map<String, Object>> getMetadata() {
        try {
            List<Map<String, Object>> tableMetadata = sqlExecutorService.getTableMetadata();
            List<Map<String, Object>> columns = sqlExecutorService.getColumnMetadata();
            List<Object> tables = tableMetadata.stream()
                    .map(Map::values).flatMap(Collection::stream)
                    .collect(Collectors.toList());

            Map<String, List<String>> columnMap = columns.stream()
                    .collect(Collectors.groupingBy(
                            row -> ((String) row.get("TABLE_NAME")).toLowerCase(), // 按表名分组并转为小写
                            Collectors.mapping(
                                    row -> (String) row.get("COLUMN_NAME"), // 提取列名
                                    Collectors.toList()
                            )
                    ));
            Map result = new HashMap();
            result.put("tableNames", tables);
            result.put("tableColumns", columnMap);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取表元数据出错", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 删除数据库配置
    @DeleteMapping("/delete-database/{name}")
    public ResponseEntity<String> deleteDatabase(@PathVariable String name) {
        try {
            sqlExecutorService.deleteDatabaseConfig(name);
            return ResponseEntity.ok("数据库配置删除成功");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("删除数据库配置出错", e);
            return ResponseEntity.status(500).body("删除数据库配置时发生错误");
        }
    }

}