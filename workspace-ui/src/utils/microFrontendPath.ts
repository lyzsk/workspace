function normalizeBasePath(basePath: string) {
    const withLeadingSlash = basePath.startsWith("/") ? basePath : `/${basePath}`;
    return withLeadingSlash.length > 1
        ? withLeadingSlash.replace(/\/+$/, "")
        : withLeadingSlash;
}

export function resolveMicroInternalPath(path: string, basePath: string) {
    const base = normalizeBasePath(basePath);
    const normalizedPath = path.startsWith("/") ? path : `/${path}`;
    const suffixIndex = normalizedPath.search(/[?#]/);
    const pathname = suffixIndex >= 0
        ? normalizedPath.slice(0, suffixIndex)
        : normalizedPath;
    const suffix = suffixIndex >= 0 ? normalizedPath.slice(suffixIndex) : "";
    if (base === "/") {
        return `${pathname}${suffix}` || "/";
    }
    if (pathname === base || pathname === `${base}/`) {
        return `/${suffix}`;
    }
    if (pathname.startsWith(`${base}/`)) {
        return `${pathname.slice(base.length)}${suffix}`;
    }
    return `${pathname}${suffix}` || "/";
}

export function resolveMicroHostPath(basePath: string, internalPath: string) {
    const base = normalizeBasePath(basePath);
    const internal = resolveMicroInternalPath(internalPath, base);
    return base === "/"
        ? internal
        : internal === "/"
          ? base
          : `${base}${internal}`;
}

export function resolveMicroInitialPath(fullPath: string, basePath: string) {
    return resolveMicroInternalPath(fullPath, basePath);
}
