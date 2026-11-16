import type { DocumentOperation } from "@/api/system/document";

interface DocumentSocketOptions {
    documentId: string;
    token: string;
    onOperation: (operation: DocumentOperation & { revision: string; operatorId: string }) => void;
    onPresence?: (payload: unknown) => void;
    onStatus?: (connected: boolean) => void;
}

export class DocumentSocket {
    private socket?: WebSocket;
    private reconnectTimer?: ReturnType<typeof setTimeout>;
    private retries = 0;
    private intentionalClose = false;
    private readonly options: DocumentSocketOptions;

    constructor(options: DocumentSocketOptions) {
        this.options = options;
    }

    connect() {
        this.intentionalClose = false;
        const scheme = window.location.protocol === "https:" ? "wss" : "ws";
        this.socket = new WebSocket(
            `${scheme}://${window.location.host}/ws/documents/${this.options.documentId}`,
            ["workspace", this.options.token],
        );
        this.socket.onopen = () => {
            this.retries = 0;
            this.options.onStatus?.(true);
        };
        this.socket.onclose = () => {
            this.options.onStatus?.(false);
            if (!this.intentionalClose) {
                const delay = Math.min(10000, 1000 * 2 ** this.retries);
                this.retries = Math.min(this.retries + 1, 4);
                clearTimeout(this.reconnectTimer);
                this.reconnectTimer = setTimeout(() => this.connect(), delay);
            }
        };
        this.socket.onerror = () => this.options.onStatus?.(false);
        this.socket.onmessage = (event) => {
            const message = JSON.parse(event.data);
            if (message.type === "document.operation") {
                this.options.onOperation(message.data);
            } else if (message.type === "PRESENCE" || message.opType === "PRESENCE") {
                this.options.onPresence?.(message);
            }
        };
    }

    send(operation: DocumentOperation) {
        if (this.socket?.readyState === WebSocket.OPEN) {
            this.socket.send(JSON.stringify(operation));
        }
    }

    close() {
        this.intentionalClose = true;
        clearTimeout(this.reconnectTimer);
        this.socket?.close();
        this.socket = undefined;
    }
}
