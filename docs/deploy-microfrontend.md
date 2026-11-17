# EAP 微前端生产形态：构建、托管与 sys_app 配置

> 2026-11-17。dev 链路（`/eap/src/micro.ts`，EAP dev server 3006）保持不变，本文只覆盖生产产物形态。
> `npm run build:micro` 已验证通过（2026-11-17，26.7s）：入口 `ies-micro.js` 导出 bootstrap/mount/unmount，主 chunk `micro-*.js` 约 2.78MB（gzip 691KB），`ies-micro.css` 约 822KB，动态 chunk 相对路径引用。

## 1. 两种形态对照

| 项 | dev | 生产 |
|---|---|---|
| `sys_app.module_entry` | `/eap/src/micro.ts` | `/eap/micro/ies-micro.js` |
| `sys_app.style_entry` | `NULL` | `/eap/micro/ies-micro.css` |
| `/eap/**` 由谁提供 | EAP Vite dev server（3006） | nginx：静态产物 + 反向代理 |
| 子应用 API 前缀 | `/eap`（`.env.development`） | `/eap`（`.env.micro` / `.env.production`），不变 |

子应用 API 路径两种形态完全一致（`/eap/api/**`、`/eap/toolapi/**`），所以宿主侧只换静态资源的来源，不动子应用代码。

## 2. 构建

```bash
cd D:\dev\eap-tool-service\art-design-pro
npm run build:micro        # 产物在 dist/micro/（仅微前端）
npm run build:all          # 同时产出独立运行形态 dist/ + 微前端 dist/micro/
```

## 3. 本地验证生产形态（preview 联调）

不碰 dev 默认四进程，另起一个 preview 实例模拟生产：

1. `npm run build:micro`，然后 `npx vite preview --mode micro --port 3007`
   （preview 继承 `server.proxy`；`.env.micro` 里的 `VITE_API_PROXY_URL=http://127.0.0.1:8081` 保证 `/eap/api`、`/api` 兜底转发到 EAP 后端，EAP 后端 8081 需在跑）
2. `workspace-ui/.env.development.local` 增加（该文件不入库）：
   ```
   VITE_EAP_UI_PROXY=http://localhost:3007
   VITE_EAP_UI_ORIGIN=http://localhost:3007
   ```
   重启 workspace-ui。
3. dev 库执行切换 SQL：
   ```sql
   UPDATE system.sys_app
   SET module_entry = '/eap/micro/ies-micro.js',
       style_entry  = '/eap/micro/ies-micro.css'
   WHERE code = 'eap-tool-service';
   ```
4. 验收：进入 IES → 离开 → 再进入；F5 后仍在登录态；网络面板确认 `/eap/micro/ies-micro.js` 与 `ies-micro.css` 均 200、没有任何 `/eap/src/**` 请求；子应用内 API（`/eap/api/**`）正常。
5. 回切 dev：DB 执行 `UPDATE system.sys_app SET module_entry='/eap/src/micro.ts', style_entry=NULL WHERE code='eap-tool-service';`，删除 `.env.development.local` 里那两行，重启 workspace-ui。

## 4. 生产部署（nginx 同源反代，免 CORS）

```nginx
# workspace 前端 server 块内；顺序：静态产物与 API 前缀不冲突，无需特别排序

# EAP 微前端静态产物
location /eap/micro/ {
    alias /opt/eap-tool-service/art-design-pro/dist/micro/;
    expires 1h;   # 入口 ies-micro.js/css 无 hash，缓存时间别放长；chunk 带 hash 可长缓存
}

# EAP 后端 API：等价于 dev 配置的 rewrite 去 /eap 前缀
location /eap/api/ {
    proxy_pass http://127.0.0.1:8081/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}

# 机台主数据：等价于 dev 配置的 rewrite 去 /eap/toolapi 前缀
location /eap/toolapi/ {
    proxy_pass http://127.0.0.1:8081/;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
```

生产库执行第 3 节同一条切换 SQL（`V1__baseline.sql` 保持 dev 值不动，重建开发库不受影响）。

## 5. 注意事项

- **同源是设计前提**：`import('/eap/micro/ies-micro.js')` 走宿主同源，天然无 CORS。若产物放独立域名，必须另配 CORS 响应头，不建议。
- `ies-micro.js` / `ies-micro.css` 是无 hash 固定名，每次 `build:micro` 内容变化但 URL 不变——nginx 缓存策略按上面注释控制，避免发版后浏览器拿旧入口配新 chunk。
- 主 chunk 2.78MB 偏大（gzip 691KB），echarts/xlsx 等已是独立懒加载 chunk；首屏体积优化是后续可选项，不阻塞部署。
- EAP 独立运行形态（3006/8081 + `dist/` + base `/eap/`）不受本文影响。
