import type { Directive } from "vue";
import { useUserStore } from "@/store/modules/user";

export const permission: Directive<HTMLElement, string> = {
    mounted(el, binding) {
        const userStore = useUserStore();
        if (binding.value && !userStore.hasPerm(binding.value)) {
            el.parentNode?.removeChild(el);
        }
    },
};
