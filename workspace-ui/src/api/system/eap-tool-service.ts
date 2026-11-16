import { request } from "@/utils/http";

export interface EapToolServiceTicket {
    ticket: string;
    eapUrl: string;
    expiresIn: number;
}

export function fetchEapToolServiceTicket() {
    return request<EapToolServiceTicket>({
        url: "/eap-tool-service/ticket",
        method: "post",
    });
}
