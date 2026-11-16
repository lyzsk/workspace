export function formatTime(value?: string | null): string {
    if (!value) {
        return "-";
    }
    return value.replace("T", " ").slice(0, 19);
}

export function formatFileStamp(date = new Date()): string {
    const pad = (value: number) => String(value).padStart(2, "0");
    return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}_${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`;
}
