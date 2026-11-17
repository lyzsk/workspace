import { defineStore } from "pinia";
import { ref } from "vue";
import { emitMicroFrontendEvent } from "@/utils/microFrontendEvents";

export type Locale = "zh-CN" | "en";

export const useAppStore = defineStore(
    "app",
    () => {
        const isDark = ref(true);
        const locale = ref<Locale>("zh-CN");

        function applyTheme(silent = false) {
            document.documentElement.classList.toggle("dark", isDark.value);
            if (!silent) {
                emitMicroFrontendEvent("host.theme.changed", {
                    appCode: "*",
                    path: window.location.pathname,
                    theme: isDark.value ? "dark" : "light",
                });
            }
        }

        function toggleTheme() {
            isDark.value = !isDark.value;
            applyTheme();
        }

        function setLocale(value: Locale) {
            locale.value = value;
            localStorage.setItem("workspace-locale", value);
            emitMicroFrontendEvent("host.locale.changed", {
                appCode: "*",
                path: window.location.pathname,
                locale: value,
            });
        }

        return { isDark, locale, applyTheme, toggleTheme, setLocale };
    },
    {
        persist: {
            key: "workspace-app",
            pick: ["locale", "isDark"],
        },
    },
);
