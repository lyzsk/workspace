# 2026-11-17 微前端主题同步与退出登录修复

> 按 workspace 约定：本文只给代码与操作说明，源码由用户手动复制粘贴。
> 目标：进入子系统时带入 workspace 主题；子系统切换主题同步回 workspace；子系统退出登录直接回到 workspace 登录页。

## 0. 事件约定

统一事件名：`workspace:micro-frontend`

| type | 方向 | 说明 |
|---|---|---|
| `host.theme.changed` | 宿主 → 子应用 | 宿主主题变化，`theme` 为 `light` / `dark` |
| `app.theme.changed` | 子应用 → 宿主 | 子应用切换主题，`theme` 为 `light` / `dark` |
| `app.logout` | 子应用 → 宿主 | 子应用请求退出登录 |

本期先共用 `html.dark` 一个全局 class；CSS 隔离阶段再改成容器级。

---

## A. workspace 宿主改动

### A1. `workspace-ui/src/utils/microFrontendEvents.ts`

把 `MicroFrontendEventType` 和 `MicroFrontendEventDetail` 改成：

```ts
export type MicroFrontendEventType =
    | "host.app.entered"
    | "host.app.left"
    | "host.app.error"
    | "host.theme.changed"
    | "app.theme.changed"
    | "app.logout";

export interface MicroFrontendEventDetail {
    type: MicroFrontendEventType;
    appCode: string;
    path: string;
    time: string;
    message?: string;
    theme?: "light" | "dark";
}
```

### A2. `workspace-ui/src/utils/singleSpa.ts`

`MicroRegistrationOptions` 增加：

```ts
    appCode: string;
    theme: "light" | "dark";
```

`customProps` 改成：

```ts
        customProps: {
            domElement: options.domElement,
            workspaceToken: options.workspaceToken,
            appCode: options.appCode,
            theme: options.theme,
        },
```

### A3. `workspace-ui/src/store/modules/app.ts`

`applyTheme` 改成带 `silent` 参数，并广播宿主主题：

```ts
        function applyTheme(silent = false) {
            document.documentElement.classList.toggle("dark", isDark.value);
            if (!silent) {
                window.dispatchEvent(
                    new CustomEvent("workspace:micro-frontend", {
                        detail: {
                            type: "host.theme.changed",
                            appCode: "*",
                            path: window.location.pathname,
                            time: new Date().toISOString(),
                            theme: isDark.value ? "dark" : "light",
                        },
                    }),
                );
            }
        }
```

`toggleTheme` 不变，仍调用 `applyTheme()`。

### A4. `workspace-ui/src/views/apps/detail/MicroFrontendHost.vue`

模板不变。`<script setup>` 需要改这几处。

导入：

```ts
import { useRoute, useRouter } from "vue-router";
import { logout as logoutApi } from "@/api/auth";
import { clearDraftsForUser } from "@/utils/draft";
import { useAppStore } from "@/store/modules/app";
import { usePermissionStore } from "@/store/modules/permission";
import {
    emitMicroFrontendEvent,
    onMicroFrontendEvent,
    type MicroFrontendEventDetail,
} from "@/utils/microFrontendEvents";
```

新增：

```ts
const router = useRouter();
const appStore = useAppStore();
const permissionStore = usePermissionStore();

let offMicroEvent: (() => void) | null = null;
let loggingOut = false;

function onMicroEvent(detail: MicroFrontendEventDetail) {
    if (detail.type === "app.theme.changed" && detail.theme) {
        if (appStore.isDark !== (detail.theme === "dark")) {
            appStore.isDark = detail.theme === "dark";
            appStore.applyTheme(true);
        }
        return;
    }
    if (detail.type === "app.logout") {
        void handleSubappLogout();
    }
}

async function handleSubappLogout() {
    if (loggingOut) {
        return;
    }
    loggingOut = true;
    try {
        await logoutApi({ silent: true });
    } catch {
    }
    permissionStore.reset();
    clearDraftsForUser(userStore.user?.id);
    userStore.logout();
    await router.replace("/login");
}
```

`enter()` 里 `registerMicroApplication` 增加两个参数：

```ts
        const cleanup = await registerMicroApplication({
            name: `workspace:${current.code}`,
            path: current.basePath,
            entry: current.moduleEntry,
            styleEntry: current.styleEntry,
            appCode: current.code,
            theme: appStore.isDark ? "dark" : "light",
            domElement: microContainer.value,
            workspaceToken: userStore.token,
            onError: (message) => {
                if (!disposed) {
                    error.value = message;
                    emitMicroFrontendEvent("host.app.error", {
                        appCode: current.code,
                        path: current.basePath,
                        message,
                    });
                }
            },
        });
```

`onMounted` / `onUnmounted` 改成：

```ts
onMounted(() => {
    offMicroEvent = onMicroFrontendEvent(onMicroEvent);
    load();
});

onUnmounted(async () => {
    disposed = true;
    offMicroEvent?.();
    offMicroEvent = null;
    await cleanupCurrentMicro();
});
```

### A5. `workspace-ui/src/views/index/index.vue`

不需要改。宿主主题按钮仍然调用 `appStore.toggleTheme()`，`applyTheme` 会广播 `host.theme.changed`。

### A6. 可选：主题持久化

如果希望刷新后保持主题，把 `store/modules/app.ts` 的持久化改为：

```ts
        persist: {
            key: "workspace-app",
            pick: ["locale", "isDark"],
        },
```

---

## B. EAP 子应用改动

### B1. 新增 `art-design-pro/src/utils/microBridge.ts`

```ts
export const MICRO_EVENT = 'workspace:micro-frontend'

export type MicroTheme = 'light' | 'dark'

let micro = false
let hostTheme: MicroTheme = 'light'
let appCode = ''

export function setMicroContext(context: { theme?: MicroTheme; appCode?: string }): void {
  micro = true
  hostTheme = context.theme ?? 'light'
  appCode = context.appCode ?? ''
}

export function isMicroMode(): boolean {
  return micro
}

export function getHostTheme(): MicroTheme {
  return hostTheme
}

export function emitMicroEvent(type: string, payload: Record<string, unknown> = {}): void {
  window.dispatchEvent(
    new CustomEvent(MICRO_EVENT, {
      detail: {
        type,
        appCode,
        path: window.location.pathname,
        time: new Date().toISOString(),
        ...payload
      }
    })
  )
}

export function onMicroEvent(listener: (detail: any) => void): () => void {
  const handler = (event: Event) => {
    listener((event as CustomEvent).detail)
  }
  window.addEventListener(MICRO_EVENT, handler)
  return () => window.removeEventListener(MICRO_EVENT, handler)
}
```

### B2. `art-design-pro/src/micro.ts`

`MicroProps` 改成：

```ts
import { applyMicroTheme, onMicroEvent, setMicroContext } from './utils/microBridge'

interface MicroProps {
  domElement: Element
  workspaceToken: string
  appCode?: string
  theme?: 'light' | 'dark'
}
```

`mount` 开头增加：

```ts
  setMicroContext({ theme: props.theme, appCode: props.appCode })
```

`initStore(app)` 之后、`app.mount` 之前增加：

```ts
  applyMicroTheme(props.theme ?? 'light')
```

`mount` 里注册宿主主题监听（在 `app.mount` 之后）：

```ts
  hostThemeOff = onMicroEvent((detail) => {
    if (detail.type === 'host.theme.changed' && detail.theme) {
      applyMicroTheme(detail.theme)
    }
  })
```

文件顶部声明：

```ts
let hostThemeOff: (() => void) | null = null
```

`unmount` 里增加：

```ts
  hostThemeOff?.()
  hostThemeOff = null
```

### B3. `art-design-pro/src/App.vue`

导入：

```ts
import { isMicroMode } from './utils/microBridge'
```

`onBeforeMount` 改成：

```ts
onBeforeMount(() => {
  toggleTransition(true)
  if (!isMicroMode()) {
    initializeTheme()
  }
})
```

### B4. `art-design-pro/src/utils/ui/animation.ts`

导入：

```ts
import { applyMicroTheme, emitMicroEvent, isMicroMode } from '@/utils/microBridge'
```

`toggleTheme` 改成：

```ts
const toggleTheme = () => {
  const store = useSettingStore()
  const next = store.systemThemeType === LIGHT ? DARK : LIGHT

  if (isMicroMode()) {
    const theme = next === DARK ? 'dark' : 'light'
    applyMicroTheme(theme)
    emitMicroEvent('app.theme.changed', { theme })
  } else {
    useTheme().switchThemeStyles(next)
  }

  useCommon().refresh()
}
```

### B5. `art-design-pro/src/store/modules/user.ts`

把 `logOut` 拆成 `clearSession` 和 `logOut`：

```ts
    const clearSession = () => {
      const currentUserId = info.value.userId
      if (currentUserId) {
        localStorage.setItem(StorageConfig.LAST_USER_ID_KEY, String(currentUserId))
      }

      info.value = {}
      isLogin.value = false
      isLock.value = false
      lockPassword.value = ''
      accessToken.value = ''
      refreshToken.value = ''
      sessionStorage.removeItem('iframeRoutes')
      useMenuStore().setHomePath('')
      resetRouterState(500)
    }

    const logOut = () => {
      clearSession()
      const currentRoute = router.currentRoute.value
      const redirect = currentRoute.path !== '/login' ? currentRoute.fullPath : undefined
      router.push({
        name: 'Login',
        query: redirect ? { redirect } : undefined
      })
    }
```

返回对象增加 `clearSession`。

### B6. `art-design-pro/src/api/auth.ts`

增加：

```ts
export function fetchLogout() {
  return request.post<void>({
    url: '/api/auth/logout'
  })
}
```

### B7. `art-design-pro/src/components/core/layouts/art-header-bar/widget/ArtUserMenu.vue`

导入：

```ts
  import { emitMicroEvent, isMicroMode } from '@/utils/microBridge'
  import { fetchLogout } from '@/api/auth'
```

`loginOut` 的确认回调改成：

```ts
      }).then(async () => {
        if (isMicroMode()) {
          try {
            await fetchLogout()
          } catch {
          }
          userStore.clearSession()
          emitMicroEvent('app.logout')
          return
        }
        userStore.logOut()
      })
```

### B8. `art-design-pro/src/micro.ts` 的 `applyMicroTheme` 使用

`applyMicroTheme` 在 `microBridge.ts` 里实现，依赖 EAP 的 setting store 和主题枚举：

```ts
import { SystemThemeEnum } from '@/enums/appEnum'
import { useSettingStore } from '@/store/modules/setting'

export function applyMicroTheme(theme: MicroTheme): void {
  const store = useSettingStore()
  const next = theme === 'dark' ? SystemThemeEnum.DARK : SystemThemeEnum.LIGHT
  store.setGlopTheme(next, next)
  document.documentElement.classList.toggle('dark', theme === 'dark')
  document.documentElement.style.setProperty('--custom-radius', `${store.customRadius}rem`)
}
```

注意：不要在 micro 模式调用 `useTheme().setSystemTheme`，它会用 `setAttribute('class', ...)` 覆盖 `<html>` 的 class 列表。

---

## C. 验证步骤

1. 用户手动启动 workspace-admin、workspace-ui、EAP 后端、EAP dev server。
2. 从 workspace 进入 IES：
   - workspace 当前是暗色 → 子应用应为暗色；
   - workspace 当前是亮色 → 子应用应为亮色。
3. 在子应用 header 切主题：
   - 子应用切换；
   - workspace 的主题 class 和右上角图标同步。
4. 在 workspace header 切主题：
   - workspace 切换；
   - 子应用同步。
5. 在 workspace 与子应用之间来回切换页面，主题保持不变。
6. 在子应用用户菜单点退出：
   - 子应用调用自己的 `/api/auth/logout`；
   - 宿主调用 workspace `/api/auth/logout`；
   - 页面回到 workspace `/login`，不出现子应用的登录页。
7. 独立运行 EAP（3006）：本地登录、退出、主题切换保持原行为。

## D. 已知限制

- 本期共用 `html.dark`，没有做 CSS 隔离；Tailwind/Element Plus 的全局样式冲突仍未解决。
- 只同步 light/dark，不同步主色、圆角、菜单风格。
- token 仍在 URL query 里、路由仍是 memory history；这两项属于后续契约。
