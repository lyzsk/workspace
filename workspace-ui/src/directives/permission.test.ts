import { beforeEach, afterEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { nextTick } from "vue";
import type { DirectiveBinding, VNode } from "vue";
import { permission } from "@/directives/permission";
import { useUserStore } from "@/store/modules/user";
import type { UserInfo } from "@/types/api";

function user(permissions: string[]): UserInfo {
    return {
        id: "1",
        username: "admin",
        realName: "管理员",
        gender: 0,
        roles: [],
        permissions,
    };
}

beforeEach(() => {
    vi.stubGlobal("BroadcastChannel", undefined);
    vi.stubGlobal("window", {
        localStorage: { removeItem: vi.fn() },
    });
    setActivePinia(createPinia());
});

afterEach(() => {
    vi.unstubAllGlobals();
});

describe("permission directive", () => {
    it("reacts to permission changes and restores the original display", async () => {
        const store = useUserStore();
        store.token = "token";
        store.user = user(["sys:app:update"]);
        store.status = "authenticated";
        const el = { style: { display: "inline-flex" } } as HTMLElement;
        const binding = { value: "sys:app:update" } as DirectiveBinding<string>;
        const vnode = null as unknown as VNode<never, HTMLElement>;

        permission.mounted?.(el, binding, vnode, null);
        expect(el.style.display).toBe("inline-flex");

        store.setUser(user([]));
        await nextTick();
        expect(el.style.display).toBe("none");

        store.setUser(user(["sys:app:update"]));
        await nextTick();
        expect(el.style.display).toBe("inline-flex");
        permission.beforeUnmount?.(el, binding, vnode, null);
    });

    it("uses the latest permission code after a directive update", async () => {
        const store = useUserStore();
        store.token = "token";
        store.user = user(["sys:app:update"]);
        store.status = "authenticated";
        const el = { style: { display: "inline-flex" } } as HTMLElement;
        const vnode = null as unknown as VNode<never, HTMLElement>;
        const first = { value: "sys:app:update" } as DirectiveBinding<string>;
        const second = { value: "sys:app:delete" } as DirectiveBinding<string>;

        permission.mounted?.(el, first, vnode, null);
        permission.updated?.(el, second, vnode, vnode);
        expect(el.style.display).toBe("none");

        store.setUser(user(["sys:app:delete"]));
        await nextTick();
        expect(el.style.display).toBe("inline-flex");
        permission.beforeUnmount?.(el, second, vnode, null);
    });
});
