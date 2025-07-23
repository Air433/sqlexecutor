package com.dsb.sqlexecutor.controller;

import com.dsb.sqlexecutor.model.DatabaseConfig;
import com.dsb.sqlexecutor.service.SqlExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.*;

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
        model.addAttribute("databaseConfig", new DatabaseConfig());
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
        model.addAttribute("databaseConfig", new DatabaseConfig());
        model.addAttribute("databaseConfigs", sqlExecutorService.getAllDatabaseConfigs());
        model.addAttribute("databases", sqlExecutorService.getDatabases());
        model.addAttribute("currentDatabase", sqlExecutorService.getCurrentDatabase());

        return "index";
    }
}