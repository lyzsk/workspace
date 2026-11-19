import { request } from "@/utils/http";
import type {
    PasswordPayload,
    ProfilePayload,
    UserInfo,
} from "@/types/api";

const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";

const MOCK_USER: UserInfo = {
    id: "1",
    username: "admin",
    realName: "管理员",
    gender: 1,
    tenantId: "0",
    tenantName: "默认公司",
};

function wait(ms: number) {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

export async function getMe(): Promise<UserInfo> {
    if (USE_MOCK) {
        await wait(200);
        return MOCK_USER;
    }

    return request<UserInfo>({
        url: "/auth/me",
        method: "get",
    });
}

export async function updateProfile(
    payload: ProfilePayload,
): Promise<UserInfo> {
    return request<UserInfo>({
        url: "/auth/profile",
        method: "put",
        data: payload,
    });
}

export async function changePassword(payload: PasswordPayload): Promise<void> {
    return request<void>({
        url: "/auth/password",
        method: "put",
        data: payload,
    });
}

export async function touch(options?: { silent?: boolean }): Promise<void> {
    if (USE_MOCK) {
        return;
    }

    await request<void>(
        {
            url: "/auth/touch",
            method: "post",
        },
        { silent: options?.silent },
    );
}
