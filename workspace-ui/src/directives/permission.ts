import { watchEffect, type ObjectDirective } from "vue";
import { useUserStore } from "@/store/modules/user";

const stops = new WeakMap<HTMLElement, () => void>();
const displays = new WeakMap<HTMLElement, string>();
const codes = new WeakMap<HTMLElement, string>();

function applyPermission(el: HTMLElement, code: string) {
    const userStore = useUserStore();
    const allowed = !code || userStore.hasPerm(code);
    const display = displays.get(el) ?? "";
    el.style.display = allowed ? display : "none";
}

export const permission: ObjectDirective<HTMLElement, string> = {
    mounted(el, binding) {
        displays.set(el, el.style.display);
        codes.set(el, binding.value);
        const stop = watchEffect(() => applyPermission(el, codes.get(el) ?? ""));
        stops.set(el, stop);
    },
    updated(el, binding) {
        codes.set(el, binding.value);
        applyPermission(el, binding.value);
    },
    beforeUnmount(el) {
        stops.get(el)?.();
        stops.delete(el);
        displays.delete(el);
        codes.delete(el);
    },
};
