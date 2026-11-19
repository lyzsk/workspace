# Workspace 第一阶段 Nacos / Gateway 骨架实施说明

> 日期：2026-11-19  
> 状态：代码、编译与离线单元测试已完成；未启动服务，未连接真实 Nacos，未操作数据库。  
> 对应计划：`docs/cv/2026-11-19-workspace-app-authorization-refactor.md` 第 1～3 节。

## 1. 本阶段边界

已完成：

- 根工程引入 Spring Cloud `2025.0.0`、Spring Cloud Alibaba `2025.0.0.0` BOM，Spring Boot 保持 `3.5.16`。
- 新增 Reactive 的 `workspace-gateway` 启动模块，默认端口 `61115`。
- `workspace-admin` 的注册名改为 `workspace-service`，保留原端口 `61114` 和现有 MVC、认证实现。
- Gateway 与 workspace-service 均接入 Nacos Discovery，默认地址为 `localhost:8848`。
- Gateway 提供 `/api/**`、`/ws/**` 负载均衡路由、RequestId 透传/生成、Result 外形错误响应。
- 提供 Nacos Config 的 shared/gateway Data ID 与业务路由占位示例。
- 新增不启动 Spring 上下文、不依赖真实 Nacos 的单元测试。

本阶段不包含 token introspection、可信身份头清洗、限流、前端 qiankun 改造、Redis、数据库迁移或现有认证逻辑修改。

## 2. 路由约定

本地默认路由与 Nacos 示例均保留完整请求路径，不使用隐式前缀移除：

| 路径 | 目标 | order |
| --- | --- | --- |
| `/api/**` | `lb://workspace-service` | 100 |
| `/ws/**` | `lb:ws://workspace-service` | 100 |

`lb:ws://` 是 Spring Cloud Gateway 对负载均衡 WebSocket 路由的 URI 写法；目标服务名仍为 `workspace-service`。

业务路由必须先于平台 `/api/**` 匹配。`workspace-gateway/src/main/resources/nacos/workspace-gateway.yaml.example` 使用不存在的占位服务展示规则：

```yaml
- id: business-app-example-api
  uri: lb://example-service
  order: 0
  predicates:
    - Path=/api/example-app/**
```

实际接入时，将 `example-app` 替换为新业务的 `appCode`，将 `example-service` 替换为该业务在 Nacos 中的服务名。该示例不指向任何现有子应用。若 Nacos 的 `workspace-gateway.yaml` 接管路由，应保留完整的路由列表，包括平台 API 与 WebSocket 路由，避免只发布新增业务路由而丢失本地兜底路由。

## 3. 配置项

### 3.1 Gateway

| 配置项 | 环境变量 | 默认值 | 用途 |
| --- | --- | --- | --- |
| `server.port` | `GATEWAY_PORT` | `61115` | Gateway 监听端口 |
| `spring.cloud.nacos.server-addr` | `NACOS_SERVER_ADDR` | `localhost:8848` | Discovery 与 Config 地址 |
| `spring.cloud.nacos.discovery.enabled` | `NACOS_DISCOVERY_ENABLED` | `true` | 是否启用 Nacos Discovery |
| `spring.cloud.nacos.discovery.register-enabled` | `NACOS_REGISTER_ENABLED` | `true` | 是否注册 Gateway 实例 |
| `spring.cloud.nacos.config.enabled` | `NACOS_CONFIG_ENABLED` | `false` | 默认关闭远程配置；`nacos` profile 开启 |
| `spring.cloud.nacos.config.group` | `NACOS_CONFIG_GROUP` | `DEFAULT_GROUP` | Nacos Config 分组 |
| `spring.profiles.active` | `SPRING_PROFILES_ACTIVE` | 未设置 | 设为 `nacos` 时导入远程 shared/gateway 配置 |

启用 Nacos Config 时设置 `SPRING_PROFILES_ACTIVE=nacos`。`application-nacos.yml` 按以下顺序导入两个 Data ID：

1. `workspace-shared.yaml`
2. `workspace-gateway.yaml`

两者均使用 `optional:nacos:`，Nacos Config 暂不可用时不会仅因远程配置缺失而阻止应用启动。生产环境若要求远程配置必须存在，应在部署配置中改为非 optional，并单独验收启动失败行为。

示例文件位于：

- `workspace-gateway/src/main/resources/nacos/workspace-shared.yaml.example`
- `workspace-gateway/src/main/resources/nacos/workspace-gateway.yaml.example`

发布到 Nacos 时去掉 `.example`，Data ID 分别为 `workspace-shared.yaml`、`workspace-gateway.yaml`，默认 group 为 `DEFAULT_GROUP`。凭证与命名空间等部署参数应来自环境变量或密钥管理，不写入示例。

### 3.2 workspace-service

| 配置项 | 环境变量 | 默认值 |
| --- | --- | --- |
| `spring.application.name` | 无 | `workspace-service` |
| `spring.cloud.nacos.server-addr` | `NACOS_SERVER_ADDR` | `localhost:8848` |
| `spring.cloud.nacos.discovery.enabled` | `NACOS_DISCOVERY_ENABLED` | `true` |
| `spring.cloud.nacos.discovery.register-enabled` | `NACOS_REGISTER_ENABLED` | `true` |

admin 未引入 Nacos Config，数据库与现有认证配置保持原样。

### 3.3 无 Nacos 的构建与测试

Maven 编译和单元测试不启动应用上下文，因此不要求本机存在 Nacos。直接运行：

```bash
mvn -pl workspace-gateway -am test
mvn test
```

若仅做不带注册中心的本地进程调试，可显式设置 `NACOS_DISCOVERY_ENABLED=false`、`NACOS_REGISTER_ENABLED=false`；但此时 `lb://` 路由没有服务实例来源，不能据此验证真实转发。

## 4. RequestId 与错误响应

`RequestIdWebFilter` 接受符合 `^[A-Za-z0-9._-]{1,128}$` 的 `X-Request-Id`；缺失或非法时生成 UUID。同一个值写入下游请求头与客户端响应头，并覆盖未匹配路由的错误请求。

`GatewayErrorWebExceptionHandler` 返回：

```json
{
  "code": 404,
  "msg": "资源不存在",
  "data": null
}
```

HTTP 状态与 body `code` 使用同一个数字。已覆盖 400、401、403、404、409、503 的既有中文语义，其余错误使用 `操作失败`，不向客户端返回堆栈。

## 5. 实际验证结果

在 `D:\dev\workspace` 执行，未启动或停止任何服务：

1. `mvn -pl workspace-gateway -am test`：`BUILD SUCCESS`；Gateway 4 个测试，失败 0、错误 0、跳过 0。
2. `mvn test`：`BUILD SUCCESS`；8 个 reactor 模块全部成功，共 66 个测试，失败 0、错误 0、跳过 0。
3. `mvn -pl workspace-gateway,workspace-admin dependency:tree -Dincludes=com.alibaba.nacos:nacos-client`：`BUILD SUCCESS`；两个模块均解析到 `com.alibaba.nacos:nacos-client:3.0.3`。

Maven 官方版本组合解析成功，没有更换版本。

## 6. 未做事项与验证边界

- 未启动 Gateway/admin，未验证 61115/61114 的实际监听与转发，未触碰已有 61114 进程。
- 未启动或连接真实 Nacos，未验证注册、摘流、配置刷新与 WebSocket 端到端握手。
- 未实现 token introspection、身份头防伪造、认证服务不可用到 503 的完整链路；这些属于后续阶段。
- 未修改 workspace-system、workspace-ui、现有认证代码、数据库或部署配置。
- 当前错误结构已通过直接异常处理单测验证，但尚未覆盖真实网络异常与已提交响应的全部场景。

置信度：编译、依赖版本和离线行为为高；真实 Nacos 注册与路由联调为未验证。若部署环境的 Nacos 认证、namespace、网络或服务健康检查配置不同，需要按部署参数重新验收。
