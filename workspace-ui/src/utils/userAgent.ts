export function formatUserAgent(ua?: string | null): string {
    if (!ua) return "-";
    const summary = [parseOs(ua), parseBrowser(ua)].filter(Boolean).join(" · ");
    return summary || ua;
}

function parseOs(ua: string): string {
    if (/Windows NT 10\.0/.test(ua)) return "Windows 10/11";
    if (/Windows NT 6\.3/.test(ua)) return "Windows 8.1";
    if (/Windows NT 6\.2/.test(ua)) return "Windows 8";
    if (/Windows NT 6\.1/.test(ua)) return "Windows 7";
    if (/Windows/.test(ua)) return "Windows";
    if (/iPhone|iPad|iPod/.test(ua)) return "iOS";
    if (/Mac OS X/.test(ua)) return "macOS";
    const android = ua.match(/Android (\d+)/);
    if (android) return `Android ${android[1]}`;
    if (/Linux/.test(ua)) return "Linux";
    return "";
}

function parseBrowser(ua: string): string {
    const rules: Array<[RegExp, string]> = [
        [/Edg(?:e|A|iOS)?\/(\d+)/, "Edge"],
        [/OPR\/(\d+)/, "Opera"],
        [/Firefox\/(\d+)/, "Firefox"],
        [/Chrome\/(\d+)/, "Chrome"],
        [/Version\/(\d+).*Safari/, "Safari"],
        [/(?:MSIE |rv:)(\d+)/, "IE"],
    ];
    for (const [re, name] of rules) {
        const m = ua.match(re);
        if (m) return `${name} ${m[1]}`;
    }
    return "";
}
