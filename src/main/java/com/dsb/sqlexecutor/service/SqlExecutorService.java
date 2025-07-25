package com.dsb.sqlexecutor.service;

import com.dsb.sqlexecutor.model.DatabaseConfig;
import com.dsb.sqlexecutor.repository.SqlExecutorRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SqlExecutorService {

    @Autowired
    private SqlExecutorRepository sqlExecutorRepository;

    private final Map<String, DatabaseConfig> databaseConfigMap = new HashMap<>();
    private String currentDatabase;

//    public SqlExecutorService() {
//        // 初始化默认数据库配置（不依赖外部组件）
//        DatabaseConfig defaultConfig = new DatabaseConfig();
//        defaultConfig.setDatabaseName("dsb");
//        defaultConfig.setUsername("sa");
//        defaultConfig.setPassword("123456");
//        defaultConfig.setHost("localhost");
//        defaultConfig.setPort(1433);
//        defaultConfig.setJdbcUrl(String.format(
//                "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=true;trustServerCertificate=true",
//                defaultConfig.getHost(),
//                defaultConfig.getPort(),
//                defaultConfig.getDatabaseName()
//        ));
//
//        databaseConfigMap.put("default", defaultConfig);
//        currentDatabase = "default";
//    }

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

//    @PostConstruct
//    public void init() {
//        // 使用 application.properties 中的配置作为默认配置
//        DatabaseConfig defaultConfig = new DatabaseConfig();
//        // 从 JDBC URL 中提取数据库名、主机和端口
//        String[] parts = jdbcUrl.split(";");
//        String databaseName = null;
//        String host = null;
//        int port = 0;
//        for (String part : parts) {
//            if (part.startsWith("databaseName=")) {
//                databaseName = part.substring("databaseName=".length());
//            } else if (part.startsWith("jdbc:sqlserver://")) {
//                String hostPort = part.substring("jdbc:sqlserver://".length());
//                String[] hostPortParts = hostPort.split(":");
//                host = hostPortParts[0];
//                port = Integer.parseInt(hostPortParts[1]);
//            }
//        }
//        defaultConfig.setDatabaseName(databaseName);
//        defaultConfig.setUsername(username);
//        defaultConfig.setPassword(password);
//        defaultConfig.setHost(host);
//        defaultConfig.setPort(port);
//        defaultConfig.setJdbcUrl(jdbcUrl);
//
//        databaseConfigMap.put("default", defaultConfig);
//        currentDatabase = "default";
//        switchDatabase("default");
//    }


    // 在依赖注入完成后执行初始化
//    @PostConstruct
//    public void init() {
//        switchDatabase("default");
//    }

    // 执行查询
    public List<Map<String, Object>> executeQuery(String sql) {
        if (!hasValidDatabaseConfig()) {
            throw new IllegalStateException("请先选择或添加数据库配置");
        }
        return sqlExecutorRepository.executeQuery(sql);
    }

    // 执行更新
    public int executeUpdate(String sql) {
        if (!hasValidDatabaseConfig()) {
            throw new IllegalStateException("请先选择或添加数据库配置");
        }
        return sqlExecutorRepository.executeUpdate(sql);
    }

    // 添加数据库配置
    public void addDatabaseConfig(String name, DatabaseConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("数据库配置不能为空");
        }

        // 确保JDBC URL包含必要的SQL Server参数
        if (config.getJdbcUrl() == null || !config.getJdbcUrl().contains("encrypt=")) {
            String url = String.format(
                    "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=true;trustServerCertificate=true",
                    config.getHost(),
                    config.getPort(),
                    config.getDatabaseName()
            );
            config.setJdbcUrl(url);
        }

        databaseConfigMap.put(name, config);
    }

    // 切换数据库
    public void switchDatabase(String name) {
        DatabaseConfig config = databaseConfigMap.get(name);

        if (config == null) {
            throw new IllegalArgumentException("数据库配置不存在: " + name);
        }

        DataSource dataSource = sqlExecutorRepository.createDataSource(
                config.getJdbcUrl(),
                config.getUsername(),
                config.getPassword());

        currentDatabase = name;

        sqlExecutorRepository.setDataSource(dataSource);
    }

    // 获取所有数据库配置
    public Map<String, DatabaseConfig> getAllDatabaseConfigs() {
        Map<String, DatabaseConfig> validConfigs = new HashMap<>();
        for (Map.Entry<String, DatabaseConfig> entry : databaseConfigMap.entrySet()) {
            if (entry.getValue() != null) {
                validConfigs.put(entry.getKey(), entry.getValue());
            }
        }
        return validConfigs;
    }

    // 获取数据库列表
    public List<String> getDatabases() {
        if (!hasValidDatabaseConfig()) {
            return Collections.emptyList();
//            throw new IllegalStateException("请先选择或添加数据库配置");
        }
        return sqlExecutorRepository.getDatabases();
    }

    // 获取当前数据库配置名
    public String getCurrentDatabase() {
        return currentDatabase;
    }

    // 获取当前数据库配置对象
    public DatabaseConfig getCurrentDatabaseConfig() {
        return databaseConfigMap.get(currentDatabase);
    }

    // 检查是否有有效的数据库配置
    public boolean hasValidDatabaseConfig() {
        return currentDatabase != null &&
                databaseConfigMap.containsKey(currentDatabase) &&
                databaseConfigMap.get(currentDatabase) != null;
    }

    // 获取表的元数据
    public List<Map<String, Object>> getTableMetadata() {
        if (!hasValidDatabaseConfig()) {
            throw new IllegalStateException("请先选择或添加数据库配置");
        }
        // 执行获取元数据的查询
        return sqlExecutorRepository.getTableMetadata();
    }

    // 在 SqlExecutorService 中添加
    public List<Map<String, Object>> getColumnMetadata() {
        if (!hasValidDatabaseConfig()) {
            throw new IllegalStateException("请先选择或添加数据库配置");
        }
        return sqlExecutorRepository.getColumnMetadata();
    }
}