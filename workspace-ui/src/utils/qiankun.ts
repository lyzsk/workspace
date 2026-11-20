import { loadMicroApp, type MicroApp } from "qiankun";
import type { ScopedAuthBridge, HostUserSummary } from "@/microfrontend/authBridge";

interface MicroApplicationOptions {
    name: string;
    path: string;
    initialPath?: string;
    entry: string;
    styleEntry?: string;
    domElement: HTMLElement;
    appCode: string;
    theme: "light" | "dark";
    locale: string;
    user: HostUserSummary | null;
    authBridge: ScopedAuthBridge;
}

interface SharedStyle {
    link: HTMLLinkElement;
    loaded: Promise<void>;
    references: number;
}

const sharedStyles = new Map<string, SharedStyle>();

function styleKey(name: string, href: string) {
    return `${name}|${href}`;
}

async function acquireStyle(name: string, href: string) {
    const key = styleKey(name, href);
    const existing = sharedStyles.get(key);
    if (existing) {
        existing.references += 1;
        await existing.loaded;
        return;
    }
    const link = document.createElement("link");
    link.rel = "stylesheet";
    link.href = href;
    link.setAttribute("data-workspace-micro-style", name);
    const entry: SharedStyle = {
        link,
        references: 1,
        loaded: Promise.resolve(),
    };
    entry.loaded = new Promise<void>((resolve, reject) => {
        link.onload = () => resolve();
        link.onerror = () => reject(new Error(`微前端样式加载失败：${href}`));
    });
    sharedStyles.set(key, entry);
    document.head.appendChild(link);
    try {
        await entry.loaded;
    } catch (error) {
        if (sharedStyles.get(key) === entry) {
            sharedStyles.delete(key);
            link.remove();
        }
        throw error;
    }
}

function releaseStyle(name: string, href: string) {
    const key = styleKey(name, href);
    const entry = sharedStyles.get(key);
    if (!entry) {
        return;
    }
    entry.references -= 1;
    if (entry.references <= 0) {
        sharedStyles.delete(key);
        entry.link.remove();
    }
}

export async function mountMicroApplication(
    options: MicroApplicationOptions,
): Promise<() => Promise<void>> {
    if (options.styleEntry) {
        await acquireStyle(options.name, options.styleEntry);
    }
    let microApp: MicroApp | undefined;
    try {
        microApp = loadMicroApp(
            {
                name: options.name,
                entry: options.entry,
                container: options.domElement,
                props: {
                    domElement: options.domElement,
                    basePath: options.path,
                    initialPath: options.initialPath,
                    appCode: options.appCode,
                    mode: "micro",
                    theme: options.theme,
                    locale: options.locale,
                    user: options.user,
                    authBridge: options.authBridge,
                    ...options.authBridge,
                },
            },
            {
                sandbox: true,
            },
        );
        await microApp.mountPromise;
    } catch (error) {
        await microApp?.unmount().catch(() => undefined);
        if (options.styleEntry) {
            releaseStyle(options.name, options.styleEntry);
        }
        throw error;
    }

    let cleaned = false;
    return async () => {
        if (cleaned) {
            return;
        }
        cleaned = true;
        try {
            await microApp.unmount();
        } finally {
            if (options.styleEntry) {
                releaseStyle(options.name, options.styleEntry);
            }
            options.domElement.replaceChildren();
        }
    };
}
