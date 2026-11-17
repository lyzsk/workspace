export const MICRO_FRONTEND_EVENT = "workspace:micro-frontend";

export type MicroFrontendEventType =
    | "host.app.entered"
    | "host.app.left"
    | "host.app.error"
    | "host.theme.changed"
    | "host.locale.changed"
    | "host.session.expired"
    | "host.logout"
    | "app.ready"
    | "app.route.changed"
    | "app.theme.changed"
    | "app.logout"
    | "app.session.expired"
    | "app.auth.failed"
    | "app.error";

export interface MicroFrontendEventDetail {
    version: 1;
    type: MicroFrontendEventType;
    appCode: string;
    path: string;
    time: string;
    message?: string;
    theme?: "light" | "dark";
    locale?: string;
}

export function emitMicroFrontendEvent(
    type: MicroFrontendEventType,
    detail: Omit<MicroFrontendEventDetail, "version" | "type" | "time">,
) {
    window.dispatchEvent(
        new CustomEvent<MicroFrontendEventDetail>(MICRO_FRONTEND_EVENT, {
            detail: {
                ...detail,
                version: 1,
                type,
                time: new Date().toISOString(),
            },
        }),
    );
}

export function onMicroFrontendEvent(
    listener: (detail: MicroFrontendEventDetail) => void,
) {
    const handler = (event: Event) => {
        const detail = (event as CustomEvent<MicroFrontendEventDetail>).detail;
        if (detail?.version === 1) {
            listener(detail);
        }
    };
    window.addEventListener(MICRO_FRONTEND_EVENT, handler);
    return () => window.removeEventListener(MICRO_FRONTEND_EVENT, handler);
}
