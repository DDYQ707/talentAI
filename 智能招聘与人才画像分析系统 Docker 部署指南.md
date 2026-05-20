# 智能招聘与人才画像分析系统 Docker 部署指南

## 一、环境要求

请确保本机已经安装：

| 软件            | 推荐版本 |
| --------------- | -------- |
| JDK             | 17       |
| Maven           | 3.9+     |
| Docker Desktop  | 最新版   |
| Git             | 最新版   |
| Node.js（前端） | 18+      |

**二、安装 Docker Desktop**

https://www.docker.com/products/docker-desktop/

## 1. 管理员打开 PowerShell

执行：

```
wsl --install
```

------

## 2. 更新 WSL

```
wsl --update
```

------

## 3. 设置 WSL2 默认版本

```
wsl --set-default-version 2
```

安装后：

- 勾选 WSL2

- 启动 Docker Desktop

- 等待 Engine running

  # 三、项目目录结构

  项目目录：

  ```
  talent-ai-system
   ├── docker
   │    ├── docker-compose.yml
   │    ├── mysql
   │    ├── redis
   │    ├── nacos
   │    └── minio
   │
   ├── talent-ai-backend
   └── talent-ai-frontend
  ```

  ------

  # 四、启动 Docker 基础服务

  进入 docker 目录：

  ```
  cd docker
  ```

  启动：

  ```
  docker compose up -d
  ```

  ------

  ## 查看容器状态

  ```
  docker ps
  ```

  正常情况下会看到：

  | 服务  | 端口        |
  | ----- | ----------- |
  | MySQL | 3307        |
  | Redis | 6380        |
  | Nacos | 8848        |
  | MinIO | 9000 / 9001 |

**六、Nacos 登录**

浏览器访问：

```
http://localhost:8848/nacos
```

默认账号：

```
用户名：nacos
密码：nacos
```

# 七、MinIO 登录

访问：

```
http://localhost:9001
```

默认账号密码：

```
minioadmin
minioadmin
```



**七、初始化 MySQL 数据库**

 创建数据库

见数据库文档  创建六个数据库（ddl在talent_ai_schema.sql里，要分开，不是直接运行）

| 微服务             | 建议库名              | 主要表                                                       |
| ------------------ | --------------------- | ------------------------------------------------------------ |
| talent-auth        | `talent_auth_db`      | `sys_*`、`auth_verification_code`、`candidate_profile`       |
| talent-job         | `talent_job_db`       | `job_post`、`job_application`、`application_stage_log`、`offer*` |
| talent-resume      | `talent_resume_db`    | `resume*`、`resume_attachment`                               |
| talent-interview   | `talent_interview_db` | `interview`、`interview_evaluation`                          |
| talent-talent-pool | `talent_pool_db`      | `talent_pool_record`、`talent_tag`、`talent_pool_tag`        |
| talent-ai-agent    | `talent_ai_db`        | `ai_*`（不含 chat 时可拆 `talent_ai_chat_db`）               |
| talent-analytics   | —                     | 无业务表，通过 Feign/消息聚合各服务 API                      |

# 八、后端启动

进入：

```
cd talent-ai-backend
```

------

## Maven 安装依赖

```
mvn clean install
```

------

## 启动服务

依次启动：

```
talent-gateway
talent-auth
talent-job
talent-resume
talent-agent
talent-analytics
```