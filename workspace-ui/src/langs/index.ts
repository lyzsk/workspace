import { createI18n } from "vue-i18n";
import zhCN from "./zh-CN";
import en from "./en";

const saved = localStorage.getItem("workspace-locale");
const locale = saved === "en" ? "en" : "zh-CN";

const i18n = createI18n({
    legacy: false,
    locale,
    fallbackLocale: "zh-CN",
    messages: {
        "zh-CN": zhCN,
        en,
    },
});

export default i18n;
