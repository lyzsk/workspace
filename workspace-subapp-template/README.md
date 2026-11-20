# 子应用接入模板

workspace 主应用的子应用骨架。基于 Vue 3 + TypeScript + Element Plus + Vite，通过 qiankun 2 兼容桥接入口挂载。

两个形态同时可用：

- **独立运行**：`pnpm dev` 起在 3010，脱离主应用单独开发调试
- **微前端挂载**：被主应用加载，在它的内容区内渲染

这套模板解决的是「接入成本和接入一致性」：登录、鉴权、主题、语言、路由、错误处理这些跨应用的事情已经接好，新子应用只需要写业务页面。

---

## 一、三步接入

### 1. 复制模板并改三个配置

```bash
cp -r micro-subapp-template my-subapp
cd my-subapp
pnpm install
```

打开 `.env.development` 和 `.env.production`，改这几项：

| 变量 | 含义 | 要求 |
|---|---|---|
| `VITE_PORT` | 开发端口 | 与主应用登记的地址一致，不能漂移 |
| `VITE_APP_CODE` | 应用编码 | **必须**与主应用 `sys_app.code` 完全一致 |
| `VITE_APP_BASE_PATH` | 路由前缀 | **必须**与主应用 `sys_app.base_path` 完全一致 |
| `VITE_API_PROXY` | 后端地址 | 指向本子应用自己的服务 |
| `VITE_BASE_URL` | 部署路径 | 生产环境改成 `/你的路径/` |

`VITE_APP_CODE` 写错的表现是：页面能显示，但主应用收不到子应用的事件，表现为加载动画不消失、地址栏不同步。这类问题不报错，只能靠核对配置发现。

修改应用编码或部署路径时，还要同步修改 `.env.micro`、`public/qiankun-entry.js` 中的 ESM 地址和两个生命周期全局名称。微前端模式先执行 `pnpm build:micro` 与 `pnpm serve:micro`，再由当前宿主加载。

### 2. 在主应用登记

在 workspace 后台「应用管理」里新增一条记录：

- `code` = `VITE_APP_CODE`
- `base_path` = `VITE_APP_BASE_PATH`
- `module_entry` = `/ies/micro/qiankun.html`
- `style_entry` = `/ies/micro/subapp-micro.css`

主应用会根据 `sys_app` 自动追加应用入口，不需要为应用根节点手工新增 `MicroFrontendHost` 菜单。子应用页面如需登记中央菜单，使用本子应用的 `app_code`，`path` 填完整宿主路径，`component` 留空，并授权给同应用角色。当前 `hidden = 1` 表示显示、`hidden = 0` 表示隐藏。

### 3. 写业务

新页面放 `src/views/`，新接口放 `src/api/`，照着 `views/equipment` 和 `api/equipment.ts` 复制改名即可。页面结构按下一节的骨架来，不要各写一套。

---

## 二、契约

### 主应用注入的属性

字段名不可修改，改了两边一起改。

| 字段 | 类型 | 用途 |
|---|---|---|
| `container` | `HTMLElement` | qiankun 提供的挂载容器 |
| `domElement` | `HTMLElement` | 主应用自研加载器提供的容器，二选一 |
| `appCode` | `string` | 应用编码，用于事件过滤 |
| `basePath` | `string` | 路由前缀 |
| `mode` | `"micro" \| "standalone"` | 运行形态。宿主固定传 `micro`，独立启动传 `standalone` |
| `theme` | `"light" \| "dark"` | 当前主题 |
| `locale` | `string` | 当前语言 |
| `user` | `{ username, realName? }` | 当前登录用户 |
| `getToken` | `() => string \| null` | 取 token 的方法，每次现取 |
| `navigate` | `(path) => void` | 请求主应用跳转 |
| `onSessionExpired` | `() => void` | 会话过期通知 |
| `getAppAuthorization` | `() => Promise<AppAuthorization>` | 读取当前应用的中央角色、权限和菜单授权 |

角色与权限不通过 `user` 或额外 props 注入。微前端模式由 `store/modules/user.ts` 调 `getAppAuthorization()` 异步加载，页面使用 `hasRole()` / `hasPermission()` 判断；独立运行使用本地演示角色。

访问这些属性统一走 `src/micro/context.ts`，业务代码不要直接读 `props`。

### 事件

通道 `workspace:micro-frontend`，CustomEvent，`version` 固定为 `1`。

**子应用发给主应用**（用 `emitAppEvent` 发送）

| 事件 | 时机 |
|---|---|
| `app.ready` | 挂载并完成首帧渲染后 |
| `app.route.changed` | 子应用内部路由变化后 |
| `app.logout` | 用户点击登出 |
| `app.session.expired` | 收到 401 |
| `app.auth.failed` | 收到 403 |
| `app.error` | 未捕获异常 |
| `app.theme.changed` | 子应用主动改主题（本模板不主动改） |

**主应用发给子应用**（用 `onHostEvent` 接收）

| 事件 | 子应用应做什么 |
|---|---|
| `host.theme.changed` | 更新根节点 `data-theme` |
| `host.locale.changed` | 切换语言包 |
| `host.session.expired` | 清空内存中的用户信息 |
| `host.logout` | 清空内存中的用户信息 |

---

## 三、页面骨架

三个示例页（设备清单 / 设备台账 / 保养记录）用的是同一套骨架，新页面直接照抄：

```vue
<div class="page-container">
    <!-- 查询区默认收起，点表格工具栏的搜索图标才展开 -->
    <section v-show="showSearch" class="page-toolbar">
        <div class="search-field">
            <span class="search-label">字段名</span>
            <el-input v-model="keyword" />
        </div>
        <div class="search-actions">
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
        </div>
    </section>

    <section class="page-body">
        <div class="table-toolbar">
            <div class="table-toolbar-left">
                <span class="table-hint">说明文字</span>
            </div>
            <div class="table-toolbar-right">
                <el-button text :icon="Search" @click="showSearch = !showSearch" />
                <el-button text :icon="Refresh" @click="handleSearch" />
            </div>
        </div>

        <el-table :data="pagedRows" stripe>...</el-table>

        <div class="table-pagination">
            <el-pagination layout="total, sizes, prev, pager, next" background />
        </div>
    </section>
</div>
```

四条约定：

1. **查询区默认收起**（`showSearch = ref(false)`）。主应用用户管理就是这么做的，展开时才出现第二张卡片；写成常显会和主应用的形态不一致。
2. **表格不传 `border`**。主应用表格默认只有斑马纹（`TableHeader` 里 `border = false`），外框由表格卡片提供；再加 `border` 会变成卡片框 + 表格框的双层描边。
3. **分页带 `background`**，放在卡片内右下角（`.table-pagination`）。主应用对分页另有定制：按钮透明底 + 1px 描边、当前页主色填充。
4. **页面里不写 scoped 样式**。公共外观全在 `src/styles/index.scss`，页面只写模板和逻辑；三页统一样式、改一处三页同步。

### 底色与留白

| 项 | 取值 | 说明 |
|---|---|---|
| 页面底色 | `var(--app-bg, #fafbfc)` | 主应用在 `:root` 上定义，子应用直接继承；**不要**用 `--el-bg-color-page`（Element Plus 是 `#f2f3f5`，和主应用不是一个色） |
| 嵌入时内边距 | 0 | 留白交给宿主 `.content` 的 24px |
| 独立运行时内边距 | 16px | 根节点带 `data-mode="standalone"` 时才生效 |

---

## 四、硬性约束

这几条踩过坑，不接受例外。

**1. 不写全局样式。**
`html`、`body`、裸 `*` 这些选择器会直接改写主应用。所有样式收在 `.sub-app-root` 下，包括 `.page-*`、`.table-*` 这些页面级类。写 SCSS 时用 `:deep()` 或加前缀，不要图省事写全局。

**2. 不自己切换 `html.dark`。**
主应用已经控制它。子应用再 toggle 一次，两边互相抵消，切主题会闪。子应用只读 `props.theme`，把 `data-theme` 写在自己根节点上。

**3. 不带侧边栏和顶栏。**
主应用已经提供。模板只输出内容区。照搬后台模板会出双菜单。

**4. token 不落 localStorage。**
每次都从 `getToken()` 现取。缓存会导致切账号后拿到旧 token，而且多个子应用共用一个域名下的存储，容易互相覆盖。

**5. 401 不自己跳登录页。**
上报给主应用，由它统一跳。多个子应用各跳一次，用户会看到地址栏来回闪。

**6. 页面底色不写死。**
用 `var(--app-bg, #fafbfc)`，跟宿主走。写死 `#f2f3f5` 或 `#fafbfc` 都会在换主题、换宿主时露馅。

---

## 五、排查

| 现象 | 原因 | 处理 |
|---|---|---|
| 主应用里白屏，控制台 404 | 入口地址不对或端口漂移 | 核对 `VITE_PORT` 与主应用登记的 entry |
| 页面出来了，加载动画不消失 | `appCode` 不匹配，`app.ready` 被过滤 | 核对 `VITE_APP_CODE` |
| 主应用样式被打乱 | 子应用写了全局样式 | 检查 `styles/index.scss` 和组件的非 scoped 样式 |
| 切主题时页面闪一下 | 子应用自己 toggle 了 `html.dark` | 删掉，只保留 `data-theme` |
| 接口 401 | token 取不到 | 检查主应用是否传了 `getToken` |
| 弹窗被主应用盖住 | z-index 冲突 | 本模板用 2000，主应用用 3000 起，需要时调 `App.vue` |
| 独立运行白屏 | 未走独立启动分支 | 访问 `http://localhost:3010/?standalone=1` |
| 子应用区域底色发灰 | 用了 `--el-bg-color-page` | 改用 `var(--app-bg, #fafbfc)` |
| 内容四周留白明显偏大 | 子应用和宿主各加了一层内边距 | 检查根节点的 `data-mode`，嵌入时应为 `micro` |
| 表格像套了两层框 | `el-table` 传了 `border` | 去掉 `border`，外框交给表格卡片 |
| 一进页面就是两张卡片 | 查询区写成了常显 | 加 `showSearch = ref(false)` + 工具栏搜索图标 |

---

## 六、命令

```bash
pnpm dev          # 独立运行，3010
pnpm build        # 独立生产构建，产物在 dist
pnpm build:micro  # qiankun 构建，产物在 dist/micro
pnpm serve:micro  # 以 3010 预览 dist/micro，供当前宿主 /ies 代理加载
pnpm preview      # 预览独立构建产物
pnpm type-check   # 类型检查
```

---

## 七、文档

| 文档 | 读者 | 内容 |
|---|---|---|
| `README.md` | 接入者 | 怎么用、契约、约束、排查 |
| `TEMPLATE-SPEC.md` | 接入者 + 维护者 | 目录与文件职责、分层规则、扩展步骤 |
