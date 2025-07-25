package com.dsb.sqlexecutor.service;

import com.dsb.sqlexecutor.model.DatabaseConfig;
import com.dsb.sqlexecutor.repository.SqlExecutorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SqlExecutorService {

    @Autowired
    private SqlExecutorRepository sqlExecutorRepository;

    private final Map<String, DatabaseConfig> databaseConfigMap = new HashMap<>();
    private final Map<String, String> computerDatabaseNameMap = new HashMap<>();
    private final Map<String, DataSource> computerDataSourceMap = new HashMap<>();

    // 获取计算机名称
    private String getComputerName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    // 执行查询
    public List<Map<String, Object>> executeQuery(String sql) {
        String computerName = getComputerName();
        String databaseName = computerDatabaseNameMap.get(computerName);
        if (databaseName == null) {
            throw new IllegalStateException("请先选择或添加数据库配置");
        }
        DataSource dataSource = computerDataSourceMap.get(computerName);
        sqlExecutorRepository.setDataSource(dataSource);
        return sqlExecutorRepository.executeQuery(sql);

    }

    // 执行更新
    public int executeUpdate(String sql) {
        String computerName = getComputerName();
        String tenantId = computerDatabaseNameMap.get(computerName);
        if (tenantId == null) {
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
        DataSource dataSource = sqlExecutorRepository.createDataSource(
                config.getJdbcUrl(),
                config.getUsername(),
                config.getPassword());
        computerDataSourceMap.put(name, dataSource);
    }

    // 切换数据库
    public void switchDatabase(String name) {
        String computerName = getComputerName();
        DatabaseConfig config = databaseConfigMap.get(name);

        if (config == null) {
            throw new IllegalArgumentException("数据库配置不存在: " + name);
        }

        if (name.equals(computerDatabaseNameMap.get(computerName))){

        }else {
            computerDatabaseNameMap.put(computerName, name);
            DataSource dataSource = sqlExecutorRepository.createDataSource(config.getJdbcUrl(), config.getUsername(), config.getPassword());
            computerDataSourceMap.put(computerName, dataSource);
        }


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
        String computerName = getComputerName();

        DataSource dataSource = computerDataSourceMap.get(computerName);
        if (dataSource == null) {
            return Collections.emptyList();
        }
        sqlExecutorRepository.setDataSource(dataSource);
        return sqlExecutorRepository.getDatabases();

    }

    // 获取当前计算机选择的数据库配置名
    public String getCurrentDatabase() {
        String computerName = getComputerName();
        return computerDatabaseNameMap.getOrDefault(computerName, null);
    }

    // 获取当前计算机选择的数据库配置对象
    public DatabaseConfig getCurrentDatabaseConfig() {
        String currentDatabase = getCurrentDatabase();
        return databaseConfigMap.get(currentDatabase);
    }

    // 检查是否有有效的数据库配置
    public boolean hasValidDatabaseConfig() {
        String computerName = getComputerName();
        String currentDatabase = computerDatabaseNameMap.get(computerName);
        return currentDatabase != null &&
                databaseConfigMap.containsKey(currentDatabase) &&
                databaseConfigMap.get(currentDatabase) != null;
    }

    // 获取表的元数据
    public List<Map<String, Object>> getTableMetadata() {
        String computerName = getComputerName();
        String tenantId = computerDatabaseNameMap.get(computerName);
        if (tenantId == null) {
            throw new IllegalStateException("请先选择或添加数据库配置");
        }
        return sqlExecutorRepository.getTableMetadata();

    }

    // 获取列的元数据
    public List<Map<String, Object>> getColumnMetadata() {
        String computerName = getComputerName();
        String tenantId = computerDatabaseNameMap.get(computerName);
        if (tenantId == null) {
            throw new IllegalStateException("请先选择或添加数据库配置");
        }
        return sqlExecutorRepository.getColumnMetadata();

    }
}