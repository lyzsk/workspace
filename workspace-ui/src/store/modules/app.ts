import { defineStore } from "pinia";
import { ref } from "vue";

export type Locale = "zh-CN" | "en";

export const useAppStore = defineStore(
    "app",
    () => {
        const isDark = ref(true);
        const locale = ref<Locale>("zh-CN");

        function applyTheme() {
            document.documentElement.classList.toggle("dark", isDark.value);
        }

        function toggleTheme() {
            isDark.value = !isDark.value;
            applyTheme();
        }

        function setLocale(value: Locale) {
            locale.value = value;
            localStorage.setItem("workspace-locale", value);
        }

        return { isDark, locale, applyTheme, toggleTheme, setLocale };
    },
    {
        persist: {
            key: "workspace-app",
            pick: ["locale"],
        },
    },
);
