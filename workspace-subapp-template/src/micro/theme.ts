import { ref } from "vue";
import { isMicroMode } from "./context";
import type { ThemeMode } from "./contract";

/**
 * 主题同步。
 *
 * 这里有一个容易踩错的地方，先说结论：
 *
 * 微前端模式下，子应用**不要**去 toggle `document.documentElement` 的 dark 类。
 * 原因是主应用已经用 `html.dark` 控制全局主题，Element Plus 的暗色变量也挂在
 * `html.dark` 下。子应用再改一次，等于两个应用抢同一个开关，切换主题时会互相抵消。
 *
 * 正确做法分两种场景：
 * - 微前端：什么都不动。子应用样式只要用 Element Plus 的 CSS 变量，就会自动跟随。
 *   额外把主题写到根节点的 data-theme 属性上，供自定义样式（非 EP 变量）使用。
 * - 独立运行：没有主应用，需要自己切换 html.dark，否则暗色样式不生效。
 *
 * 用 ref 保存主题而不是直接查 DOM，是因为 applyTheme 会在 app.mount 之前被调用，
 * 那时根节点还不存在，查 DOM 会拿不到元素。
 */

const currentTheme = ref<ThemeMode>("light");

/** 应用主题 */
export function applyTheme(theme: ThemeMode): void {
    currentTheme.value = theme;

    if (isMicroMode()) {
        return;
    }
    document.documentElement.classList.toggle("dark", theme === "dark");
}

/** 供组件绑定的响应式主题 */
export function useTheme() {
    return currentTheme;
}

/** 判断是否暗色，供业务代码写条件逻辑时使用 */
export function isDark(): boolean {
    return currentTheme.value === "dark";
}
