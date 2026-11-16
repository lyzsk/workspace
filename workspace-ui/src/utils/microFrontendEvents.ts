export const MICRO_FRONTEND_EVENT = "workspace:micro-frontend";

export type MicroFrontendEventType =
    | "host.app.entered"
    | "host.app.left"
    | "host.app.error";

export interface MicroFrontendEventDetail {
    type: MicroFrontendEventType;
    appCode: string;
    path: string;
    time: string;
    message?: string;
}

export function emitMicroFrontendEvent(
    type: MicroFrontendEventType,
    detail: Omit<MicroFrontendEventDetail, "type" | "time">,
) {
    window.dispatchEvent(
        new CustomEvent<MicroFrontendEventDetail>(MICRO_FRONTEND_EVENT, {
            detail: { ...detail, type, time: new Date().toISOString() },
        }),
    );
}

export function onMicroFrontendEvent(
    listener: (detail: MicroFrontendEventDetail) => void,
) {
    const handler = (event: Event) => {
        listener((event as CustomEvent<MicroFrontendEventDetail>).detail);
    };
    window.addEventListener(MICRO_FRONTEND_EVENT, handler);
    return () => window.removeEventListener(MICRO_FRONTEND_EVENT, handler);
}
