# 任意子应用跨应用免密跳转交接

> 日期：2026-11-19  
> 范围：workspace 内任意已授权子应用之间的跳转，包括 A→B、B→A、C→A、C→B。  
> 执行方式：在子应用所在电脑直接修改源码，不再放入 docs/cv。  
> 当前状态：workspace 宿主已具备通用能力；各子应用跳转入口尚未实际修改，浏览器联调未完成。

## 1. 根因与统一修复口径

workspace 挂载每个子应用时都会注入当前用户、token 获取能力和 `authBridge.navigate(path)`：

- `workspace-ui/src/utils/qiankun.ts`
- `workspace-ui/src/microfrontend/authBridge.ts`

如果子应用通过 `window.location.href`、`window.open`、目标应用独立域名、`moduleEntry` 或目标应用登录页跳转，就会离开 workspace，宿主注入的认证能力不会跟随到新页面。

所有子应用的跨应用入口统一调用：

```ts
authBridge.navigate(targetWorkspacePath);
```

`targetWorkspacePath` 由目标应用在 workspace 中注册的 `basePath` 和可选内部页面路径组成。不得针对 A→B、B→A 等组合分别写补丁，不保存第二份 token，不把 token 拼到 URL。

## 2. 各子应用新增公共辅助函数

在各子应用相同的前端工具目录新增 `src/utils/workspace-navigation.ts`；若项目没有 `@` 别名，调用处改用现有相对路径规范。

```ts
export interface WorkspaceNavigationBridge {
    navigate(path: string): void;
}

function normalizeBasePath(basePath: string): string {
    const value = basePath.trim().replace(/^\/+|\/+$/g, "");
    if (!value) {
        throw new Error("目标应用 basePath 不能为空");
    }
    return `/${value}`;
}

function normalizePagePath(path: string): string {
    const value = path.trim();
    if (!value) {
        return "";
    }
    return value.startsWith("/") ? value : `/${value}`;
}

export function navigateToWorkspaceApplication(
    authBridge: WorkspaceNavigationBridge | undefined,
    basePath: string,
    pagePath = "",
): void {
    if (!authBridge) {
        throw new Error("当前页面未接入 workspace AuthBridge");
    }
    authBridge.navigate(
        `${normalizeBasePath(basePath)}${normalizePagePath(pagePath)}`,
    );
}
```

该函数只生成宿主路由并调用宿主导航，不识别具体应用名，不处理 token。

## 3. 在 qiankun 生命周期保存 AuthBridge

子应用必须在 mount 时保存宿主注入的 `authBridge`，在 unmount 时清理。以下代码合并进现有入口，不重复创建 mount，不覆盖原有挂载和卸载逻辑：

```ts
import type { WorkspaceNavigationBridge } from "@/utils/workspace-navigation";

let workspaceAuthBridge: WorkspaceNavigationBridge | undefined;

export async function mount(props: {
    authBridge?: WorkspaceNavigationBridge;
}) {
    workspaceAuthBridge = props.authBridge;
}

export async function unmount() {
    workspaceAuthBridge = undefined;
}
```

若现有入口已经向组件或 store 提供 `authBridge`，直接复用现有保存位置，不再增加第二份全局状态。

## 4. 替换所有跨应用入口

进入目标应用首页：

```ts
import { navigateToWorkspaceApplication } from "@/utils/workspace-navigation";

function openTargetApplication() {
    navigateToWorkspaceApplication(
        workspaceAuthBridge,
        targetApplicationBasePath,
    );
}
```

进入目标应用指定页面：

```ts
navigateToWorkspaceApplication(
    workspaceAuthBridge,
    targetApplicationBasePath,
    targetPagePath,
);
```

路径格式示例：

```ts
navigateToWorkspaceApplication(workspaceAuthBridge, "/app-b");
navigateToWorkspaceApplication(workspaceAuthBridge, "/app-a", "/orders/123");
navigateToWorkspaceApplication(workspaceAuthBridge, "/app-c", "/reports");
```

以上 `/app-a`、`/app-b`、`/app-c` 仅演示格式，实际值必须替换为 workspace `sys_app.base_path` 中已有配置。

需要全局搜索并替换的跨应用跳转形式：

```text
window.location.href
window.location.assign
window.location.replace
window.open
router.push 中指向其他应用独立页面的调用
目标应用独立 origin / moduleEntry / 登录页
```

子应用内部页面跳转仍使用自身 router，不要为了本项修复改动内部路由。

## 5. 明确不做

- 不修改 workspace 宿主来硬编码任意两个应用的跳转关系。
- 不新增前端 `appCode → basePath` 映射；宿主路由已经使用 `sys_app.base_path`。
- 不在子应用持久化 token，不在 URL 携带 token。
- 不在缺少 AuthBridge 时回退到目标应用独立登录页，这会掩盖接入遗漏。

## 6. 验收矩阵

至少执行：

1. A→B→A。
2. B→C→A。
3. C→A→C。
4. 进入目标应用首页和指定内部页面各一次。

每次检查：

- 浏览器 origin 始终为 workspace。
- 路径等于目标应用 `basePath` 或 `basePath + pagePath`。
- 不出现目标应用登录页。
- 目标应用请求携带宿主当前 access token。
- 当前用户无目标应用权限时，由 workspace runtime 返回无权访问，不绕到独立页面。

## 7. 异常分流

- `当前页面未接入 workspace AuthBridge`：该子应用 mount 未保存宿主 props，先完成第 3 节。
- 宿主显示应用不存在：目标 `basePath` 与 `sys_app.base_path` 不一致。
- 宿主显示无权访问：检查用户—应用授权、目标应用启停和有效角色，不回退独立登录。
- 已调用 `authBridge.navigate` 仍进入登录页：记录导航前后完整 URL，检查目标地址是否确为 workspace 路由，再排查子应用自身认证守卫。
