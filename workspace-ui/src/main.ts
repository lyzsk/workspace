import { createApp } from "vue";
import { createPinia } from "pinia";
import piniaPluginPersistedstate from "pinia-plugin-persistedstate";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import "element-plus/theme-chalk/dark/css-vars.css";
import "./assets/styles/element-theme.scss";
import Particles from "@tsparticles/vue3";
import { loadFull } from "tsparticles";
import App from "./App.vue";
import router from "./router";
import i18n from "./langs";
import { getMe } from "./api/auth";
import { permission } from "./directives/permission";
import "./plugins/iconify";
import { useAppStore } from "./store/modules/app";
import { useUserStore } from "./store/modules/user";
import { installDevtoolsGuard } from "./utils/devtoolsGuard";
import "./assets/styles/index.scss";
import "./assets/styles/table-buttons.scss";
import "./assets/styles/sidebar.scss";

const app = createApp(App);

const pinia = createPinia();
pinia.use(piniaPluginPersistedstate);
app.use(pinia);

const appStore = useAppStore(pinia);
appStore.applyTheme();

app.use(router);
app.use(i18n);
app.use(ElementPlus);
app.directive("permission", permission);

app.use(Particles, {
    init: async (engine) => {
        await loadFull(engine);
    },
});

const disposeDevtoolsGuard = installDevtoolsGuard();
if (import.meta.hot) {
    import.meta.hot.dispose(disposeDevtoolsGuard);
}

app.mount("#app");

const userStore = useUserStore(pinia);
if (userStore.isLogin) {
    getMe()
        .then((user) => userStore.setUser(user))
        .catch(() => {});
}
