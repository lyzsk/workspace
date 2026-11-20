import { ref, watch } from "vue";
import { defineStore } from "pinia";
import {
    doneMessage,
    getUnreadCount,
    pageMyMessages,
    readAllMessages,
    readMessage,
    type MessageRow,
} from "@/api/system/message";
import { useUserStore } from "@/store/modules/user";
import { getMe } from "@/api/auth";
import { notifyDataChanged } from "@/utils/dataChange";

export const useNotificationStore = defineStore("notification", () => {
    const userStore = useUserStore();
    const messages = ref<MessageRow[]>([]);
    const unreadCount = ref(0);
    const connected = ref(false);
    let socket: WebSocket | null = null;
    let reconnectTimer: ReturnType<typeof setTimeout> | undefined;
    let pollTimer: ReturnType<typeof setInterval> | undefined;
    let retries = 0;
    let intentionalClose = false;

    async function load() {
        if (!userStore.isLogin) {
            return;
        }
        try {
            const [page, count] = await Promise.all([
                pageMyMessages({ pageNum: 1, pageSize: 30 }, true),
                getUnreadCount(true),
            ]);
            messages.value = page.records;
            unreadCount.value = Number(count);
        } catch {
            // background refresh is best-effort
        }
    }

    function scheduleReconnect() {
        if (intentionalClose || !userStore.isLogin) {
            return;
        }
        const delay = Math.min(30000, 1000 * 2 ** retries);
        retries = Math.min(retries + 1, 5);
        clearTimeout(reconnectTimer);
        reconnectTimer = setTimeout(connect, delay);
    }

    function connect() {
        if (!userStore.isLogin || socket?.readyState === WebSocket.OPEN) {
            return;
        }
        intentionalClose = false;
        const scheme = window.location.protocol === "https:" ? "wss" : "ws";
        socket = new WebSocket(
            `${scheme}://${window.location.host}/ws/notifications`,
            ["workspace", userStore.token],
        );
        socket.onopen = () => {
            connected.value = true;
            retries = 0;
            load();
        };
        socket.onmessage = (event) => {
            try {
                const payload = JSON.parse(String(event.data)) as {
                    type?: string;
                    data?: { path?: string };
                };
                if (payload?.type === "data.changed") {
                    notifyDataChanged(payload.data?.path);
                    return;
                }
            } catch {
                // non-JSON payload falls through to the message refresh
            }
            load();
        };
        socket.onerror = () => socket?.close();
        socket.onclose = async () => {
            connected.value = false;
            // 被动断开时校验一次会话：被顶号或强退后请求层会 expire 并跳登录页，
            // 不再带着已经失效的 token 无限重连。
            if (!intentionalClose && userStore.isLogin) {
                try {
                    userStore.setUser(await getMe());
                } catch {
                    // 401 已由请求层处理
                }
            }
            if (!intentionalClose && userStore.isLogin) {
                scheduleReconnect();
            }
        };
    }

    function start() {
        intentionalClose = false;
        load();
        connect();
        clearInterval(pollTimer);
        pollTimer = setInterval(load, 30000);
    }

    function stop() {
        intentionalClose = true;
        clearTimeout(reconnectTimer);
        clearInterval(pollTimer);
        socket?.close();
        socket = null;
        connected.value = false;
    }

    async function markRead(id: string) {
        await readMessage(id);
        await load();
    }

    async function markAllRead() {
        await readAllMessages();
        await load();
    }

    async function markDone(id: string) {
        await doneMessage(id);
        await load();
    }

    watch(() => userStore.isLogin, (value) => {
        if (!value) {
            stop();
            messages.value = [];
            unreadCount.value = 0;
        }
    });

    window.addEventListener("workspace:session-expired", () => stop());

    return {
        messages,
        unreadCount,
        connected,
        load,
        start,
        stop,
        markRead,
        markAllRead,
        markDone,
    };
});
