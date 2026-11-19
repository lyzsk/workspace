<template>
    <div class="login-page" :class="{ 'is-dark': appStore.isDark }">
        <ParticleBackground
            :key="appStore.isDark ? 'dark' : 'light'"
            class="page-particles"
            :count="160"
            :color="
                appStore.isDark
                    ? ['#93c5fd', '#67e8f9', '#c4b5fd']
                    : ['#3b82f6', '#06b6d4', '#8b5cf6']
            "
            :line-color="appStore.isDark ? '#93c5fd' : '#3b82f6'"
            :line-opacity="appStore.isDark ? 0.9 : 0.75"
            :line-width="1.8"
            :glow-color="
                appStore.isDark
                    ? 'rgba(147, 197, 253, 0.5)'
                    : 'rgba(59, 130, 246, 0.35)'
            "
        />
        <div class="login-visual" aria-hidden="true"></div>
        <section class="login-brand">
            <div class="brand-inner">
                <div class="brand-badge">
                    <img src="/apple-touch-icon.png" alt="芯源" />
                </div>
                <h1>{{ t("app.name") }}</h1>
                <p>{{ t("app.subtitle") }}</p>
            </div>
        </section>

        <section class="login-panel">
            <div class="login-actions">
                <button
                    class="icon-btn"
                    :title="appStore.locale === 'zh-CN' ? 'English' : '简体中文'"
                    :aria-label="appStore.locale === 'zh-CN' ? 'English' : '简体中文'"
                    @click="toggleLocale"
                >
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                        <path
                            fill="currentColor"
                            d="m18.5 10l4.4 11h-2.155l-1.201-3h-4.09l-1.199 3h-2.154L16.5 10zM10 2v2h6v2h-1.968a18.2 18.2 0 0 1-3.62 6.301a15 15 0 0 0 2.335 1.707l-.75 1.878A17 17 0 0 1 9 13.725a16.7 16.7 0 0 1-6.201 3.548l-.536-1.929a14.7 14.7 0 0 0 5.327-3.042A18 18 0 0 1 4.767 8h2.24A16 16 0 0 0 9 10.877a16.2 16.2 0 0 0 2.91-4.876L2 6V4h6V2zm7.5 10.885L16.253 16h2.492z"
                        />
                    </svg>
                </button>
                <button
                    class="icon-btn theme-btn"
                    :title="appStore.isDark ? t('header.light') : t('header.dark')"
                    :aria-label="appStore.isDark ? t('header.light') : t('header.dark')"
                    @click="toggleTheme"
                >
                    <svg v-if="appStore.isDark" viewBox="0 0 24 24" aria-hidden="true">
                        <path
                            fill="currentColor"
                            d="M12 18a6 6 0 1 1 0-12a6 6 0 0 1 0 12M11 1h2v3h-2zm0 19h2v3h-2zM3.515 4.929l1.414-1.414L7.05 5.636L5.636 7.05zM16.95 18.364l1.414-1.414l2.121 2.121l-1.414 1.414zm2.121-14.85l1.414 1.415l-2.121 2.121l-1.414-1.414zM5.636 16.95l1.414 1.414l-2.121 2.121l-1.414-1.414zM23 11v2h-3v-2zM4 11v2H1v-2z"
                        />
                    </svg>
                    <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                        <path
                            fill="currentColor"
                            d="M10 7a7 7 0 0 0 12 4.9v.1c0 5.523-4.477 10-10 10S2 17.523 2 12S6.477 2 12 2h.1A6.98 6.98 0 0 0 10 7m-6 5a8 8 0 0 0 15.062 3.762A9 9 0 0 1 8.238 4.938A8 8 0 0 0 4 12"
                        />
                    </svg>
                </button>
            </div>

            <div class="login-card">
                <h2>{{ t("login.title") }}</h2>
                <p class="login-subtitle">{{ t("login.subtitle") }}</p>

                <el-form
                    ref="formRef"
                    :model="form"
                    :rules="rules"
                    size="large"
                    @keyup.enter="submit"
                >
                    <el-form-item prop="username">
                        <el-input
                            v-model.trim="form.username"
                            :placeholder="t('login.usernamePlaceholder')"
                            autocomplete="username"
                        />
                    </el-form-item>

                    <el-form-item prop="password">
                        <el-input
                            v-model.trim="form.password"
                            type="password"
                            show-password
                            :placeholder="t('login.passwordPlaceholder')"
                            autocomplete="current-password"
                        />
                    </el-form-item>

                    <div class="slider-wrap">
                        <DragVerify
                            ref="sliderRef"
                            v-model:value="sliderPassed"
                            :text="t('login.slider')"
                            :success-text="t('login.sliderSuccess')"
                            textColor="var(--text-muted)"
                            progressBarBg="var(--primary)"
                            :background="appStore.isDark ? '#17171c' : '#F1F1F4'"
                            handlerBg="var(--panel-bg)"
                        />
                    </div>

                    <el-button
                        class="login-btn"
                        type="primary"
                        :loading="loading"
                        @click="submit"
                    >
                        {{ loading ? t("login.loading") : t("login.button") }}
                    </el-button>
                </el-form>
            </div>
        </section>

        <transition name="fade">
            <div v-if="entering" class="login-loading">
                <svg
                    xmlns="http://www.w3.org/2000/svg"
                    aria-hidden="true"
                    role="img"
                    viewBox="0 0 24 24"
                    class="login-spinner"
                >
                    <circle cx="4" cy="12" r="3" fill="currentColor">
                        <animate
                            id="loginDot1"
                            attributeName="cy"
                            begin="0;loginDot3.end+0.25s"
                            calcMode="spline"
                            dur="0.6s"
                            keySplines=".33,.66,.66,1;.33,0,.66,.33"
                            values="12;6;12"
                        ></animate>
                    </circle>
                    <circle cx="12" cy="12" r="3" fill="currentColor">
                        <animate
                            attributeName="cy"
                            begin="loginDot1.begin+0.1s"
                            calcMode="spline"
                            dur="0.6s"
                            keySplines=".33,.66,.66,1;.33,0,.66,.33"
                            values="12;6;12"
                        ></animate>
                    </circle>
                    <circle cx="20" cy="12" r="3" fill="currentColor">
                        <animate
                            id="loginDot3"
                            attributeName="cy"
                            begin="loginDot1.begin+0.2s"
                            calcMode="spline"
                            dur="0.6s"
                            keySplines=".33,.66,.66,1;.33,0,.66,.33"
                            values="12;6;12"
                        ></animate>
                    </circle>
                </svg>
                <p>{{ t("login.entering") }}</p>
            </div>
        </transition>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import {
    ElMessage,
    ElNotification,
    type FormInstance,
    type FormRules,
} from "element-plus";
import DragVerify from "@/components/core/forms/drag-verify/index.vue";
import ParticleBackground from "@/components/core/views/login/particle-background/index.vue";
import { useAppStore } from "@/store/modules/app";
import { useUserStore } from "@/store/modules/user";
import { usePermissionStore } from "@/store/modules/permission";
import { toggleThemeWithAnimation } from "@/utils/themeAnimation";

const { t } = useI18n();
const appStore = useAppStore();
const userStore = useUserStore();
const router = useRouter();
const route = useRoute();

const formRef = ref<FormInstance>();
const sliderRef = ref<{ reset: () => void } | null>(null);
const sliderPassed = ref(false);
const loading = ref(false);
const entering = ref(false);

const form = reactive({
    username: "admin",
    password: "",
});

const rules = computed<FormRules>(() => ({
    username: [
        {
            required: true,
            message: t("login.usernamePlaceholder"),
            trigger: "blur",
        },
    ],
    password: [
        {
            required: true,
            message: t("login.passwordPlaceholder"),
            trigger: "blur",
        },
    ],
}));

function toggleLocale() {
    appStore.setLocale(appStore.locale === "zh-CN" ? "en" : "zh-CN");
}

function toggleTheme(event: MouseEvent) {
    toggleThemeWithAnimation(event, () => appStore.toggleTheme());
}

async function submit() {
    if (!formRef.value) {
        return;
    }

    const valid = await formRef.value.validate().catch(() => false);
    if (!valid) {
        return;
    }

    if (!sliderPassed.value) {
        ElMessage.warning(t("login.sliderRequired"));
        return;
    }

    loading.value = true;
    try {
        await userStore.login({
            username: form.username,
            password: form.password,
        });

        usePermissionStore().reset();

        ElNotification({
            title: t("login.success"),
            message: `${t("login.welcome")}, ${t("app.name")}!`,
            type: "success",
            duration: 2500,
            zIndex: 10000,
        });

        entering.value = true;
        await new Promise((resolve) => setTimeout(resolve, 800));

        const redirect =
            typeof route.query.redirect === "string"
                ? route.query.redirect
                : "/";
        await router.replace(redirect);
    } catch (error) {
        entering.value = false;
        const message =
            error instanceof Error && error.message === "INVALID_CREDENTIALS"
                ? t("login.error")
                : error instanceof Error
                  ? error.message
                  : t("login.error");

        ElMessage.error(message);
        sliderRef.value?.reset();
        sliderPassed.value = false;
    } finally {
        loading.value = false;
    }
}

onMounted(() => {
    if (route.query.reason === "expired") {
        ElMessage.warning("登录已过期，请重新登录");
    } else if (route.query.reason === "idle") {
        ElMessage.warning("长时间未操作，已自动退出");
    }
});
</script>

<style scoped lang="scss">
.login-page {
    --login-title: #0b1220;
    --login-subtitle: #334155;
    position: relative;
    isolation: isolate;
    display: grid;
    grid-template-columns: minmax(0, 1.1fr) minmax(380px, 0.9fr);
    min-height: 100vh;
    overflow: hidden;
    background: #eef4ff;
}

.login-page.is-dark {
    --login-title: #ffffff;
    --login-subtitle: rgb(255 255 255 / 80%);
    background: #0b1220;
}

.page-particles {
    position: absolute;
    inset: 0;
    z-index: 0;
    pointer-events: auto;
}

.login-visual {
    position: absolute;
    inset: 0;
    z-index: 1;
    pointer-events: none;
    background-color: #eef4ff;
    background-image: url("../../assets/images/hero-bg.jpg");
    background-position: center center;
    background-size: contain;
    background-repeat: no-repeat;
    opacity: 0.8;
}

.login-page.is-dark .login-visual {
    background-color: #0b1220;
    opacity: 0.55;
}

.login-page::after {
    content: "";
    position: absolute;
    inset: 0;
    z-index: 2;
    pointer-events: none;
    background:
        radial-gradient(circle at 20% 30%, rgb(59 130 246 / 16%), transparent 42%),
        linear-gradient(
            135deg,
            rgb(255 255 255 / 58%),
            rgb(219 234 254 / 30%) 48%,
            rgb(255 255 255 / 62%)
        );
}

.login-page.is-dark::after {
    background:
        radial-gradient(circle at 20% 30%, rgb(37 99 235 / 20%), transparent 40%),
        linear-gradient(
            135deg,
            rgb(8 15 30 / 64%),
            rgb(15 23 42 / 28%) 48%,
            rgb(8 15 30 / 60%)
        );
}

.page-particles :deep(> div) {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
}

.page-particles :deep(canvas) {
    display: block;
    width: 100% !important;
    height: 100% !important;
}

.login-brand {
    position: relative;
    z-index: 3;
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
    padding: 48px;
    overflow: hidden;
    color: var(--login-title);
}

.brand-inner {
    position: relative;
    z-index: 2;
    width: min(420px, 100%);
}

.brand-badge {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 64px;
    height: 64px;
    padding: 6px;
    border-radius: 16px;
    background: #fff;
    overflow: hidden;
}

.brand-badge img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: contain;
}

.brand-inner h1 {
    margin: 28px 0 10px;
    color: var(--login-title);
    font-size: 44px;
    letter-spacing: -1px;
}

.brand-inner p {
    margin: 0;
    color: var(--login-subtitle);
    font-size: 16px;
}

.login-panel {
    position: relative;
    z-index: 3;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 48px;
    overflow: hidden;
}

.login-actions {
    position: absolute;
    top: 20px;
    right: 24px;
    z-index: 2;
    display: flex;
    gap: 6px;
}

.icon-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    padding: 0;
    border: 0;
    border-radius: 8px;
    background: transparent;
    color: var(--text-muted);
    cursor: pointer;
    transition:
        background 0.2s,
        color 0.2s;
}

.icon-btn:hover {
    background: var(--slider-bg);
    color: var(--text);
}

.icon-btn svg {
    display: block;
    width: 19px;
    height: 19px;
}

.icon-btn.theme-btn svg {
    width: 20px;
    height: 20px;
}

.login-card {
    position: relative;
    z-index: 2;
    width: 100%;
    max-width: 390px;
    padding: 34px 32px;
    border: 1px solid var(--border);
    border-radius: 18px;
    background: var(--panel-bg);
    box-shadow: 0 24px 70px rgb(15 23 42 / 10%);
}

.login-card h2 {
    margin: 0;
    font-size: 28px;
}

.login-subtitle {
    margin: 8px 0 26px;
    color: var(--text-muted);
    font-size: 14px;
}

.slider-wrap {
    margin: 18px 0 22px;
}

.login-btn {
    width: 100%;
    height: 44px;
}

:deep(.el-input__wrapper) {
    height: 44px;
}

.login-loading {
    position: fixed;
    inset: 0;
    z-index: 10000;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 18px;
    background: rgb(8 15 30 / 48%);
    color: #fff;
    backdrop-filter: blur(3px);
}

.login-spinner {
    width: 42px;
    height: 42px;
    color: #60a5fa;
}

.login-loading p {
    margin: 0;
    font-size: 14px;
    letter-spacing: 0.08em;
}

.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}

@media (max-width: 900px) {
    .login-page {
        grid-template-columns: 1fr;
    }

    .login-brand {
        display: none;
    }

    .login-panel {
        padding: 24px;
    }

    .login-actions {
        top: 14px;
        right: 16px;
    }
}
</style>
