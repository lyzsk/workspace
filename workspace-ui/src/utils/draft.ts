import { SessionConfig } from "@/config/session";

const DRAFT_VERSION = 1;
let sessionEnding = false;

export interface DraftPayload {
    version: number;
    savedAt: number;
    form: Record<string, unknown>;
    context?: Record<string, unknown>;
    visible?: boolean;
}

export interface DraftRegistration {
    key: string;
    snapshot: () => { payload: DraftPayload; dirty: boolean } | null;
}

const registry = new Map<string, DraftRegistration>();

export function markSessionEnding() {
    sessionEnding = true;
}

export function isSessionEnding() {
    return sessionEnding;
}

export function resetSessionEnding() {
    sessionEnding = false;
}

export function getTabId() {
    const storageKey = "workspace:tabId";
    let tabId = sessionStorage.getItem(storageKey);
    if (!tabId) {
        tabId = Math.random().toString(36).slice(2, 10);
        sessionStorage.setItem(storageKey, tabId);
    }
    return tabId;
}

export function buildDraftKey(
    userId: string | undefined,
    routePath: string,
    formKey: string,
) {
    return `workspace:draft:${userId || "anon"}:${routePath}:${formKey}:${getTabId()}`;
}

export function saveDraft(key: string, payload: DraftPayload) {
    try {
        localStorage.setItem(key, JSON.stringify(payload));
    } catch {
        // storage unavailable, draft is best-effort
    }
}

export function readDraft(key: string): DraftPayload | null {
    try {
        const raw = localStorage.getItem(key);
        if (!raw) {
            return null;
        }
        const parsed = JSON.parse(raw) as DraftPayload;
        if (parsed.version !== DRAFT_VERSION) {
            localStorage.removeItem(key);
            return null;
        }
        if (Date.now() - parsed.savedAt > SessionConfig.draftTtlMs) {
            localStorage.removeItem(key);
            return null;
        }
        return parsed;
    } catch {
        return null;
    }
}

export function clearDraft(key: string) {
    try {
        localStorage.removeItem(key);
    } catch {
        // ignore storage errors
    }
}

export function clearDraftsForUser(userId: string | undefined) {
    const prefix = `workspace:draft:${userId || "anon"}:`;
    const keys: string[] = [];
    try {
        for (let index = 0; index < localStorage.length; index += 1) {
            const key = localStorage.key(index);
            if (key?.startsWith(prefix)) {
                keys.push(key);
            }
        }
        for (const key of keys) {
            localStorage.removeItem(key);
        }
    } catch {
        // ignore storage errors
    }
    for (const key of registry.keys()) {
        if (key.startsWith(prefix)) {
            registry.delete(key);
        }
    }
}

export function registerDraft(registration: DraftRegistration) {
    registry.set(registration.key, registration);
    return () => {
        registry.delete(registration.key);
    };
}

export function saveAllDrafts() {
    for (const registration of registry.values()) {
        const snapshot = registration.snapshot();
        if (snapshot?.dirty) {
            saveDraft(registration.key, snapshot.payload);
        }
    }
}

export function hasDirtyDrafts() {
    for (const registration of registry.values()) {
        const snapshot = registration.snapshot();
        if (snapshot && (snapshot.dirty || snapshot.payload.visible === true)) {
            return true;
        }
    }
    return false;
}

export function notifyDraftSettled() {
    window.dispatchEvent(new Event("workspace:draft-settled"));
}
