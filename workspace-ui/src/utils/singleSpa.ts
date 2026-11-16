import {
    addErrorHandler,
    getAppStatus,
    registerApplication,
    removeErrorHandler,
    start,
    unregisterApplication,
} from "single-spa";

interface MicroRegistrationOptions {
    name: string;
    path: string;
    entry: string;
    styleEntry?: string;
    domElement: HTMLElement;
    workspaceToken: string;
    onError: (message: string) => void;
}

let started = false;

async function loadStyle(name: string, href: string) {
    const attribute = "data-workspace-micro-style";
    const existing = document.head.querySelector<HTMLLinkElement>(
        `link[${attribute}="${name}"]`,
    );
    if (existing) {
        return;
    }
    const link = document.createElement("link");
    link.rel = "stylesheet";
    link.href = href;
    link.setAttribute(attribute, name);
    const loaded = new Promise<void>((resolve, reject) => {
        link.onload = () => resolve();
        link.onerror = () => reject(new Error(`微前端样式加载失败：${href}`));
    });
    document.head.appendChild(link);
    await loaded;
}

export async function registerMicroApplication(
    options: MicroRegistrationOptions,
): Promise<() => Promise<void>> {
    if (getAppStatus(options.name) !== null) {
        await unregisterApplication(options.name);
    }
    if (options.styleEntry) {
        await loadStyle(options.name, options.styleEntry);
    }

    const errorHandler: Parameters<typeof addErrorHandler>[0] = (error) => {
        if (error.appOrParcelName === options.name) {
            options.onError(error.message || "微前端加载失败");
        }
    };
    addErrorHandler(errorHandler);

    registerApplication({
        name: options.name,
        app: () => import(/* @vite-ignore */ options.entry),
        activeWhen: (location) =>
            location.pathname === options.path ||
            location.pathname.startsWith(`${options.path}/`),
        customProps: {
            domElement: options.domElement,
            workspaceToken: options.workspaceToken,
        },
    });

    if (!started) {
        start();
        started = true;
    }

    return async () => {
        removeErrorHandler(errorHandler);
        if (getAppStatus(options.name) !== null) {
            await unregisterApplication(options.name);
        }
        document.head
            .querySelector(`link[data-workspace-micro-style="${options.name}"]`)
            ?.remove();
    };
}
