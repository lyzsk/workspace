# 模板规范

本文档定义模板的目录职责、文件边界和扩展方式。所有基于本模板开发的子应用都按这套结构组织，不做例外。

---

## 一、目录树

```
micro-subapp-template/
├─ index.html                    HTML 入口，只引 main.ts
├─ vite.config.ts                构建与开发服务配置
├─ package.json                  依赖与脚本
├─ tsconfig.json                 TS 项目引用入口
├─ tsconfig.app.json             应用侧 TS 配置
├─ tsconfig.node.json            构建脚本侧 TS 配置
├─ .env.development              开发环境变量
├─ .env.production               生产环境变量
├─ .gitignore
├─ README.md                     接入指南（面向使用者）
├─ TEMPLATE-SPEC.md              本文件（面向使用者与维护者）
├─ public/                       静态资源，构建时原样拷贝
└─ src/
   ├─ main.ts                    应用入口，导出生命周期 + 独立启动
   ├─ micro.ts                   qiankun 生命周期实现
   ├─ App.vue                    根组件，只放路由出口与样式作用域
   ├─ env.d.ts                   环境变量与全局类型声明
   ├─ app/
   │  └─ createApp.ts            应用工厂，双形态共用
   ├─ micro/                     跨应用层，与主应用交互的一切都在这
   │  ├─ contract.ts             属性与事件的类型契约
   │  ├─ context.ts              运行时上下文与运行形态判断
   │  ├─ events.ts               事件总线
   │  ├─ theme.ts                主题同步
   │  └─ auth.ts                 鉴权与 token
   ├─ api/                       接口层
   │  ├─ http.ts                 请求封装与错误处理
   │  ├─ equipment.ts            示例业务接口
   │  └─ mock/
   │     └─ equipment.ts         示例接口的本地模拟数据
   ├─ config/
   │  └─ index.ts                应用配置集中管理
   ├─ router/
   │  └─ index.ts                路由工厂
   ├─ store/
   │  └─ modules/
   │     └─ user.ts              用户状态
   ├─ types/
   │  └─ api.ts                  通用接口类型
   ├─ styles/
   │  └─ index.scss              基础样式：底色、卡片、工具行、表格、分页
   └─ views/
      ├─ equipment/
      │  └─ index.vue            示例页：查询 + 表格 + 分页
      ├─ ledger/
      │  └─ index.vue            示例页：台账，同一套骨架
      └─ maintenance/
         └─ index.vue            示例页：保养记录，同一套骨架
```

---

## 二、分层与依赖方向

依赖只能从上往下，不能反向。

```
views/          页面，只关心展示与交互
   ↓
store/ api/     状态与数据，业务逻辑集中在这里
   ↓
micro/ config/  跨应用契约与配置
   ↓
main.ts micro.ts app/   入口与装配
```

`micro/` 不允许依赖 `views/`、`store/`、`api/`。它是基础层，反向依赖会造成循环。

业务代码读宿主信息时只能调 `micro/context.ts` 暴露的函数，不直接读 `props` 对象。

---

## 三、文件职责

### 入口层

| 文件 | 职责 | 改动时机 |
|---|---|---|
| `index.html` | HTML 骨架，固定引用 `/src/main.ts`。微前端模式下这个文件会被主应用抓取，`<div id="app">` 是挂载点 | 改标题、加 CDN 脚本 |
| `src/main.ts` | 导出 qiankun 生命周期；非宿主环境时自行启动 | 增加全局插件（如 i18n、图标库）时在这里注册 |
| `src/micro.ts` | 实现 `bootstrap / mount / unmount / update`，负责上下文写入顺序、容器解析、ready 事件 | 需要预加载字典、埋点初始化时改 `bootstrap` |
| `src/app/createApp.ts` | 创建 Vue 实例、Pinia、路由、Element Plus，注入用户与主题，返回销毁函数 | 增加全局中间件、全局组件时改 |

`main.ts` 里有两行关键逻辑不要动：`export { bootstrap, mount, unmount, update }` 和 `__POWERED_BY_QIANKUN__` 判断。前者是 qiankun 识别子应用的依据，后者区分运行形态。

### 跨应用层 `src/micro/`

| 文件 | 职责 | 改动时机 |
|---|---|---|
| `contract.ts` | 属性接口、事件类型、事件名常量。**唯一契约来源** | 主应用新增注入字段或事件时，先改这里 |
| `context.ts` | 持有宿主属性，提供 `getAppCode / getBasePath / getContainer / navigateTo / getRunMode / isMicroMode` | 需要新的上下文查询时扩展 |
| `events.ts` | `emitAppEvent` 发事件、`onHostEvent` 收事件，带 appCode 过滤 | 需要新事件类型时同步改 `contract.ts` |
| `theme.ts` | 主题状态与同步。微前端只写 `data-theme`，独立运行才切 `html.dark` | 需要自定义主题变量时扩展 |
| `auth.ts` | 取 token、取用户、通过宿主鉴权桥读取应用授权、上报 401/403、登出 | 主应用 token 或授权机制变化时改 |

`auth.ts` 里没有登录方法，这是设计决定，不要往上加。

### 数据层

| 文件 | 职责 | 改动时机 |
|---|---|---|
| `api/http.ts` | axios 实例、Bearer 注入、401 上报、403 提示、返回解包 | 需要新增通用请求能力时改 |
| `api/equipment.ts` | 示例业务接口：类型定义 + 请求函数 + mock 开关 | 新业务按此复制 |
| `api/mock/equipment.ts` | 本地模拟数据，支持搜索与分页 | 后端未就绪时补充 |
| `types/api.ts` | `ApiResult`、`PageResult`、`PageQuery`、状态码 | 后端返回结构变化时改 |
| `store/modules/user.ts` | 用户态：`applyHostSession` 注入身份，`refreshAuthorization` 加载中央角色与权限，无登录方法，不持久化 | 权限判断用 `hasRole` / `hasPermission` |

接口函数的命名与签名约定：`listXxx(query): Promise<PageResult<Xxx>>`、`getXxx(id): Promise<Xxx>`、`createXxx(data): Promise<void>`。页面不直接调 axios。

### 配置与路由

| 文件 | 职责 | 改动时机 |
|---|---|---|
| `config/index.ts` | 集中读取环境变量，业务代码不直接读 `import.meta.env` | 新增环境变量时在这里补字段 |
| `router/index.ts` | 路由工厂，按模式选 history；`afterEach` 上报路由变化 | 新增页面时加路由记录 |

路由的两条规则：

1. 微前端模式用 `createMemoryHistory`，子应用不碰 `window.location`
2. 新增路由必须挂在 `routes` 数组里，路径不加 `basePath` 前缀（前缀由主应用负责）

### 视图层

| 文件 | 职责 | 改动时机 |
|---|---|---|
| `App.vue` | `ElConfigProvider` + `.sub-app-root` + `RouterView`；根节点写 `data-theme`（主题）与 `data-mode`（运行形态） | 需要调整全局 z-index 时改 |
| `styles/index.scss` | 全部收在 `.sub-app-root` 内的基础样式：页面底色、卡片、工具行、表格、分页 | 需要公共布局类时扩展 |
| `views/equipment/index.vue` | 示例页：查询卡片 + 表格卡片 + 分页 | 新业务按此复制 |
| `views/ledger/index.vue` | 示例页：台账，骨架同设备清单，数据为静态演示 | 同上 |
| `views/maintenance/index.vue` | 示例页：保养记录，骨架同设备清单，数据为静态演示 | 同上 |

页面统一结构（三个示例页完全一致，新页面不做例外）：

```vue
<div class="page-container">
    <!-- 查询区默认收起 -->
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

    <!-- 表格卡片：工具行 + 表格 + 分页 -->
    <section class="page-body">
        <div class="table-toolbar">
            <div class="table-toolbar-left"><span class="table-hint">说明文字</span></div>
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

页面里的状态固定四个：`showSearch`（查询区展开状态，默认 `false`）、`pageNum`、`pageSize`，以及业务查询条件。列表数据按 `filteredRows → pagedRows` 两级计算，分页在前端切片；接真实接口时把 `pagedRows` 换成接口返回。

不要给表格传 `border`。主应用表格默认 `border = false`（见主应用 `TableHeader`），外框由 `.page-body` 提供；传了 `border` 会得到卡片框 + 表格框的双层描边。

---

## 四、样式规则

**允许**：`.sub-app-root` 内的页面级类（`.page-container`、`.page-toolbar`、`.page-body`、`.table-toolbar`、`.table-pagination`）；组件内 `<style scoped>`；宿主定义的 CSS 变量。

**禁止**：`html`、`body`、裸 `*`、`:root` 全局选择器；在 `.sub-app-root` 之外定义样式（包括 `.page-*` 这类“看起来是模板私有”的类名——主应用也有 `.table-toolbar`，不隔离时会互相覆盖）；`!important` 覆盖主应用样式。

### 颜色取宿主变量

主应用在 `:root` 上定义了这些变量，子应用同处一个文档，直接继承即可。每个取值都带兜底，独立运行时不会掉色。

| 变量 | 用途 | 兜底值 |
|---|---|---|
| `--app-bg` | 页面底色、表头底色 | `#fafbfc`（暗色 `#0a0a0a`） |
| `--panel-bg` | 卡片底色 | `#ffffff` |
| `--border` | 卡片描边、表格分隔线、分页按钮描边 | `#e4e7ed` |
| `--text` | 正文与表头文字 | `#383853` |
| `--text-muted` | 标签、说明文字 | `#7987a1` |
| `--primary` | 主色，当前页码填充 | `#5d87ff` |
| `--slider-bg` | 表格行悬浮底色 | `#f2f4f5` |

写法示例：

```scss
background-color: var(--panel-bg, #ffffff);
border: 1px solid var(--border, #e4e7ed);
```

**不要用 `--el-bg-color-page` 当页面底色**：Element Plus 明亮主题取的是 `#f2f3f5`，主应用是 `#fafbfc`，两个色摆在一起子应用区域明显发灰。

### 底色与内边距

```scss
.sub-app-root {
    --sub-app-page-bg: var(--app-bg, #fafbfc);
    background-color: var(--sub-app-page-bg);

    &[data-theme="dark"] {
        --sub-app-page-bg: var(--app-bg, #0a0a0a);
    }

    // 嵌入宿主时留白交给宿主，独立运行才自己加
    &[data-mode="standalone"] {
        padding: 16px;
    }
}
```

`data-mode` 由 `App.vue` 写入，值来自 `getRunMode()`。注意判断依据是 `props.mode`，不是“上下文有没有值”——独立运行同样会写一份上下文。

### 卡片与表格

卡片尺寸对齐主应用：搜索卡片 `padding: 16px 18px`、圆角 7px；表格卡片 `padding: 16px 18px 18px`、圆角 8px；两者都是 1px 描边 + 双层轻投影。

表格参数：`stripe` 开、`border` 关、表头底色取 `--app-bg`、表头字重 600、圆角 10px。分页统一 `background`，按钮透明底 + 1px 描边，当前页主色填充。这些都在 `styles/index.scss` 里定义一次，页面不要重复写。

需要响应主题时用上面的宿主变量，不要写死颜色；需要区分自定义主题时读根节点属性：

```scss
.sub-app-root[data-theme="dark"] .my-panel {
    background-color: #141414;
}
```

---

## 五、事件时序

```
主应用加载子应用
   ↓
bootstrap()          子应用初始化，无副作用
   ↓
mount(props)         写入上下文 → 创建应用 → 挂载 DOM
   ↓
nextTick
   ↓
emit app.ready       主应用收到后收起 loading
   ↓
用户操作 → 路由变化 → emit app.route.changed → 主应用同步地址栏
   ↓
主应用切主题 → emit host.theme.changed → 子应用更新 data-theme
   ↓
离开 → unmount() → 销毁实例 → 注销监听器
```

`app.ready` 必须在 `nextTick` 之后发。提前发的话，主应用收到时容器还是空的，会拿到错误的尺寸，表现出来是内容区高度塌陷。

---

## 六、扩展操作

### 新增一个页面

1. 复制 `src/views/equipment/index.vue`，改名后保留骨架：查询卡片（默认收起）+ 表格卡片（工具行 + 表格 + 分页）
2. 把示例字段换成业务字段，查询条件按需增减；列表数据仍按 `filteredRows → pagedRows` 组织
3. 在 `src/router/index.ts` 的 `routes` 里加一条记录
4. 应用入口由主应用后端按 `sys_app` 自动追加，不要为每个子应用页面新增指向 `apps/detail/MicroFrontendHost` 的菜单。需要在中央菜单登记子页面时，新增本子应用 `app_code` 的 `MENU`，`path` 填完整宿主路径，`component` 留空，并授权给同应用角色
5. 当前菜单可见性约定为 `hidden = 1` 表示显示、`hidden = 0` 表示隐藏，手工编写 SQL 时必须按此取值

页面里只写模板与逻辑，不新增 scoped 样式。确实需要业务私有样式时，写在组件内 `<style scoped>` 里，不要动 `styles/index.scss` 的公共类。

### 新增一个接口

1. 在 `src/api/<业务>.ts` 里加类型与函数
2. 需要 mock 时在 `src/api/mock/` 下加同名文件，并在函数里按 `AppConfig.micro.useMock` 分流
3. 页面里调用接口函数，不直接调 `get` / `post`

### 新增一个 store

在 `src/store/modules/` 下加文件，用 setup 语法。不启用持久化——宿主注入的信息不需要落盘，落盘会留下过期副本。

### 新增全局组件或插件

在 `src/app/createApp.ts` 里注册。不要在 `main.ts` 里注册，否则微前端挂载时不会生效。

---

## 七、代码风格

与主应用 `workspace-ui` 保持一致：

- 4 空格缩进，双引号，语句结尾带分号
- 注释与 Docstring 用中文，解释「为什么这样做」，不复述代码
- 变量与函数名用完整单词，不用缩写
- 少封装：只有确实被多处复用的逻辑才抽函数
- 提前返回，压平嵌套

---

## 八、提交前检查

- [ ] `pnpm type-check` 无报错
- [ ] `pnpm dev` 独立运行，页面正常，无控制台报错
- [ ] `pnpm build` 独立构建通过
- [ ] `pnpm build:micro` 微前端构建通过，`dist/micro/subapp-micro.js` 保留 bootstrap/mount/unmount/update 导出
- [ ] `public/qiankun-entry.js` 的应用编码、全局生命周期名称和 ESM 地址与当前配置一致
- [ ] `VITE_APP_CODE` 与 `VITE_APP_BASE_PATH` 与主应用登记一致
- [ ] 新页面骨架与示例页一致：查询卡片默认收起、表格不传 `border`、分页带 `background`
- [ ] 新增样式没有写在 `.sub-app-root` 之外
- [ ] 底色用的是 `var(--app-bg, ...)`，没有用 `--el-bg-color-page`
- [ ] 新增事件已在 `contract.ts` 中补充类型
- [ ] 页面里没有直接读 `props`，走的是 `micro/context.ts`
