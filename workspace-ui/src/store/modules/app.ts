import { defineStore } from "pinia";
import { ref } from "vue";
import { emitMicroFrontendEvent } from "@/utils/microFrontendEvents";

export type Locale = "zh-CN" | "en";

export const useAppStore = defineStore(
    "app",
    () => {
        const isDark = ref(true);
        const locale = ref<Locale>("zh-CN");
        const activeAppName = ref("");
        const activeAppPageTitles = ref<string[]>([]);
        const activeAppPagePath = ref("");

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

        function setActiveApp(name: string) {
            activeAppName.value = name;
            activeAppPageTitles.value = [];
            activeAppPagePath.value = "";
        }

        function setActiveAppPage(titles: string[], path: string) {
            activeAppPageTitles.value = titles;
            activeAppPagePath.value = path;
        }

        function clearActiveApp() {
            activeAppName.value = "";
            activeAppPageTitles.value = [];
            activeAppPagePath.value = "";
        }

        return {
            isDark,
            locale,
            activeAppName,
            activeAppPageTitles,
            activeAppPagePath,
            applyTheme,
            toggleTheme,
            setLocale,
            setActiveApp,
            setActiveAppPage,
            clearActiveApp,
        };
    },
    {
        persist: {
            key: "workspace-app",
            pick: ["locale", "isDark"],
        },
    },
);
