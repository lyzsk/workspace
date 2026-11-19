<template>
    <div class="profile-page">
        <aside class="profile-card page-card">
            <img class="profile-background" :src="background" alt="" />
            <img class="profile-avatar" :src="avatar" alt="用户头像" />
            <h2>{{ userStore.user?.realName || userStore.user?.username }}</h2>
            <p class="profile-username">@{{ userStore.user?.username }}</p>
            <div class="profile-meta">
                <div>
                    <SvgIcon icon="ri:mail-line" />
                    <span>{{ userStore.user?.email || "未设置邮箱" }}</span>
                </div>
                <div>
                    <SvgIcon icon="ri:building-line" />
                    <span>{{ organization }}</span>
                </div>
                <div>
                    <SvgIcon icon="ri:smartphone-line" />
                    <span>{{ userStore.user?.mobile || "未设置手机" }}</span>
                </div>
            </div>
        </aside>

        <div class="settings-column">
            <section class="settings-card page-card">
                <h1>基本设置</h1>
                <el-form
                    ref="profileFormRef"
                    :model="profileForm"
                    :rules="profileRules"
                    label-position="top"
                    class="profile-form"
                >
                    <div class="form-grid">
                        <el-form-item label="姓名" prop="realName">
                            <el-input v-model="profileForm.realName" maxlength="50" />
                        </el-form-item>
                        <el-form-item label="性别" prop="gender">
                            <el-select v-model="profileForm.gender">
                                <el-option label="保密" :value="0" />
                                <el-option label="男" :value="1" />
                                <el-option label="女" :value="2" />
                            </el-select>
                        </el-form-item>
                        <el-form-item label="邮箱" prop="email">
                            <el-input v-model="profileForm.email" maxlength="100" />
                        </el-form-item>
                        <el-form-item label="手机" prop="mobile">
                            <el-input v-model="profileForm.mobile" maxlength="20" />
                        </el-form-item>
                    </div>
                    <el-form-item label="头像" prop="avatar">
                        <div class="avatar-options">
                            <button
                                v-for="item in avatarOptions"
                                :key="item.key"
                                type="button"
                                class="avatar-option"
                                :class="{ selected: profileForm.avatar === item.key }"
                                @click="profileForm.avatar = item.key"
                            >
                                <img :src="item.src" :alt="item.key" />
                            </button>
                        </div>
                    </el-form-item>
                    <div class="form-actions">
                        <el-button
                            type="primary"
                            :loading="saving"
                            @click="saveProfile"
                        >
                            保存
                        </el-button>
                    </div>
                </el-form>
            </section>

            <section id="password-card" class="settings-card page-card">
                <h1>更改密码</h1>
                <el-form
                    ref="passwordFormRef"
                    :model="passwordForm"
                    :rules="passwordRules"
                    label-position="top"
                >
                    <el-form-item label="当前密码" prop="currentPassword">
                        <el-input
                            v-model="passwordForm.currentPassword"
                            type="password"
                            show-password
                            autocomplete="current-password"
                        />
                    </el-form-item>
                    <el-form-item label="新密码" prop="newPassword">
                        <el-input
                            v-model="passwordForm.newPassword"
                            type="password"
                            show-password
                            autocomplete="new-password"
                        />
                    </el-form-item>
                    <el-form-item label="确认新密码" prop="confirmPassword">
                        <el-input
                            v-model="passwordForm.confirmPassword"
                            type="password"
                            show-password
                            autocomplete="new-password"
                        />
                    </el-form-item>
                    <div class="form-actions">
                        <el-button
                            type="primary"
                            :loading="passwordSaving"
                            @click="savePassword"
                        >
                            保存新密码
                        </el-button>
                    </div>
                </el-form>
            </section>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import SvgIcon from "@/components/core/base/svg-icon/index.vue";
import {
    changePassword,
    getMe,
    updateProfile,
} from "@/api/auth";
import {
    avatarOptions,
    profileBackgrounds,
    resolveAvatar,
} from "@/config/assets/images";
import { useUserStore } from "@/store/modules/user";
import type { PasswordPayload, ProfilePayload, UserInfo } from "@/types/api";
import { useFormDraft } from "@/composables/useFormDraft";

const route = useRoute();
const userStore = useUserStore();
const profileFormRef = ref<FormInstance>();
const passwordFormRef = ref<FormInstance>();
const saving = ref(false);
const passwordSaving = ref(false);

const background =
    profileBackgrounds[Math.floor(Math.random() * profileBackgrounds.length)];

const profileForm = reactive<ProfilePayload>({
    realName: "",
    gender: 0,
    email: "",
    mobile: "",
    avatar: "",
});

const passwordForm = reactive<PasswordPayload>({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
});

const profileRules: FormRules = {
    realName: [
        { required: true, message: "请输入姓名", trigger: "blur" },
        { max: 50, message: "姓名不能超过 50 个字符", trigger: "blur" },
    ],
    email: [{ type: "email", message: "邮箱格式不正确", trigger: "blur" }],
};

const profileDraft = useFormDraft({
    key: "profile-form",
    form: profileForm,
    autoRestore: false,
});

const passwordRules: FormRules = {
    currentPassword: [
        { required: true, message: "请输入当前密码", trigger: "blur" },
    ],
    newPassword: [
        { required: true, message: "请输入新密码", trigger: "blur" },
        { min: 8, max: 100, message: "新密码长度须为 8~100 位", trigger: "blur" },
    ],
    confirmPassword: [
        { required: true, message: "请再次输入新密码", trigger: "blur" },
        {
            validator: (_rule, value, callback) => {
                if (value !== passwordForm.newPassword) {
                    callback(new Error("两次输入的新密码不一致"));
                } else {
                    callback();
                }
            },
            trigger: "blur",
        },
    ],
};

const avatar = computed(() =>
    resolveAvatar(userStore.user?.avatar, userStore.user?.gender),
);
const organization = computed(() => {
    const tenant = userStore.user?.tenantName || "未分配公司";
    const dept = userStore.user?.deptName || "未分配部门";
    return `${tenant} - ${dept}`;
});

function fillProfile(user: UserInfo | null) {
    if (!user) {
        return;
    }
    profileForm.realName = user.realName || "";
    profileForm.gender = user.gender ?? 0;
    profileForm.email = user.email || "";
    profileForm.mobile = user.mobile || "";
    profileForm.avatar =
        user.avatar === "avatar-male" || user.avatar === "avatar-female"
            ? user.avatar
            : user.gender === 2
              ? "avatar-female"
              : user.gender === 1
                ? "avatar-male"
                : "avatar-default";
}

watch(() => userStore.user, fillProfile, { immediate: true });

watch(
    () => route.query.section,
    async (section) => {
        if (section === "password") {
            await nextTick();
            document.getElementById("password-card")?.scrollIntoView({
                behavior: "smooth",
                block: "start",
            });
        }
    },
    { immediate: true },
);

async function saveProfile() {
    await profileFormRef.value?.validate();
    saving.value = true;
    try {
        const user = await updateProfile(profileForm);
        userStore.setUser(user);
        profileDraft.clear();
        ElMessage.success("个人资料已保存");
    } finally {
        saving.value = false;
    }
}

async function savePassword() {
    await passwordFormRef.value?.validate();
    passwordSaving.value = true;
    try {
        await changePassword(passwordForm);
        await userStore.logout("password");
        ElMessage.success("密码已修改，请重新登录");
    } finally {
        passwordSaving.value = false;
    }
}

getMe().then(async (user) => {
    userStore.setUser(user);
    await nextTick();
    if (!profileDraft.restore()) {
        profileDraft.markClean();
    }
});
</script>

<style scoped lang="scss">
.profile-page {
    display: flex;
    align-items: flex-start;
    gap: 20px;
}

.profile-card {
    position: relative;
    width: 360px;
    padding: 32px 28px 26px;
    overflow: hidden;
    text-align: center;
}

.profile-background {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 190px;
    object-fit: cover;
}

.profile-avatar {
    position: relative;
    width: 84px;
    height: 84px;
    margin-top: 110px;
    object-fit: cover;
    border: 3px solid var(--panel-bg);
    border-radius: 50%;
}

.profile-card h2 {
    margin: 18px 0 0;
    font-size: 21px;
    font-weight: 500;
}

.profile-username {
    margin: 8px 0 0;
    color: var(--text-muted);
    font-size: 13px;
}

.profile-meta {
    width: 270px;
    max-width: 100%;
    margin: 28px auto 0;
    text-align: left;
}

.profile-meta > div {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
    margin-top: 14px;
    color: var(--text-muted);
    font-size: 13px;
}

.profile-meta span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.settings-column {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: 20px;
    min-width: 0;
}

.settings-card {
    padding: 0;
    overflow: hidden;
    scroll-margin-top: 16px;
}

.settings-card h1 {
    padding: 17px 20px;
    margin: 0;
    border-bottom: 1px solid var(--border);
    font-size: 19px;
    font-weight: 500;
}

.settings-card :deep(.el-form) {
    padding: 20px;
}

.form-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0 20px;
}

.form-grid :deep(.el-select) {
    width: 100%;
}

.avatar-options {
    display: grid;
    grid-template-columns: repeat(6, 48px);
    gap: 12px;
}

.avatar-option {
    width: 48px;
    height: 48px;
    padding: 2px;
    border: 2px solid transparent;
    border-radius: 50%;
    background: transparent;
    cursor: pointer;
}

.avatar-option:hover {
    background: var(--slider-bg);
}

.avatar-option.selected {
    border-color: var(--primary);
}

.avatar-option img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
    border-radius: 50%;
}

.form-actions {
    display: flex;
    justify-content: flex-end;
}

.form-actions :deep(.el-button) {
    min-width: 110px;
}

@media (max-width: 960px) {
    .profile-page {
        flex-direction: column;
    }

    .profile-card,
    .settings-column {
        width: 100%;
    }
}

@media (max-width: 600px) {
    .form-grid {
        grid-template-columns: 1fr;
    }

    .avatar-options {
        grid-template-columns: repeat(4, 48px);
    }
}
</style>
