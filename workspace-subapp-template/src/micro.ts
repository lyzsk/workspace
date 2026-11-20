import { nextTick } from "vue";
import { applyTheme } from "./micro/theme";
import { setMicroContext } from "./micro/context";
import { emitAppEvent } from "./micro/events";
import type { MicroAppProps } from "./micro/contract";
import type { SubAppInstance, createSubApp as CreateSubAppFn } from "./app/createApp";

/**
 * qiankun 生命周期。
 *
 * 主应用加载本子应用时会依次调用 bootstrap -> mount，离开时调用 unmount。
 * 属性变化（例如切换主题、切换语言）时调用 update。
 *
 * 与 single-spa 的协议完全一致，因此同一份代码既能被 qiankun 加载，
 * 也能被主应用早期的自研加载器加载，迁移期不用改子应用。
 */

let instance: SubAppInstance | null = null;
let createSubApp: typeof CreateSubAppFn | null = null;

/**
 * 首次挂载前的初始化。
 * 这里不做任何事：本模板没有需要在挂载前完成的全局准备工作。
 */
export async function bootstrap(): Promise<void> {
    // 预留钩子。需要预加载字典、初始化埋点时在这里补充
}

/**
 * 挂载。
 *
 * 顺序很重要，不能调换：
 * 1. 先写入上下文，让后续创建的路由知道自己是微前端模式
 * 2. 再加载应用模块，此时 router 会使用 memory history
 * 3. 最后创建并挂载实例
 *
 * 如果先加载模块再写上下文，路由会按独立模式初始化，地址栏会被子应用改写。
 */
export async function mount(props: MicroAppProps): Promise<void> {
    setMicroContext(props);

    if (!createSubApp) {
        // 动态加载，保证应用模块在上下文就绪之后才执行
        const module = await import("./app/createApp");
        createSubApp = module.createSubApp;
    }

    const container = props.container ?? props.domElement;
    if (!container) {
        throw new Error("子应用挂载失败：宿主未提供挂载容器");
    }

    instance = createSubApp({ props, mode: "micro", container });

    // 等首帧渲染完成再通知宿主，否则宿主收到 ready 时拿到的还是空容器
    await nextTick();
    emitAppEvent("app.ready");
}

/**
 * 卸载。
 *
 * 必须把所有监听器和 Vue 实例都清掉。微前端的特点是反复进出同一个子应用，
 * 漏掉一次清理，切十次就会累积十份监听器，表现为「改一次主题触发十次重绘」。
 */
export async function unmount(): Promise<void> {
    if (!instance) {
        return;
    }
    instance.destroy();
    instance = null;
}

/**
 * 宿主属性变更。
 *
 * qiankun 在 props 变化时会调用这里，而不是重新挂载。
 * 严格来说主题变更已经通过事件总线推送，这里做一次兜底，防止事件丢失。
 */
export async function update(props: MicroAppProps): Promise<void> {
    setMicroContext(props);
    applyTheme(props.theme ?? "light");
}
