import type { MenuNode } from "@/types/system";

export interface MenuGroupNode extends MenuNode {
    virtual?: boolean;
}

export interface MenuGroupApp {
    code: string;
    name: string;
    icon?: string;
    basePath?: string;
    sortNo?: number;
}

export const VIRTUAL_APP_ID_PREFIX = "app-";

export function isVirtualMenuId(id: string) {
    return id.startsWith(VIRTUAL_APP_ID_PREFIX);
}

export function groupMenusByApp(
    menus: MenuNode[],
    apps: MenuGroupApp[],
    mainAppCode: string,
): MenuGroupNode[] {
    const flat: MenuGroupNode[] = [];
    const flatten = (items: MenuNode[]) => {
        for (const item of items) {
            flat.push({ ...item, children: [] });
            flatten(item.children ?? []);
        }
    };
    flatten(menus);

    const byId = new Map(flat.map((node) => [node.id, node]));
    const groupOf = (node: MenuGroupNode) => node.appCode || mainAppCode;
    const rootsByGroup = new Map<string, MenuGroupNode[]>();
    for (const node of flat) {
        const parent = byId.get(node.parentId);
        if (parent && parent.id !== node.id && groupOf(parent) === groupOf(node)) {
            parent.children!.push(node);
            continue;
        }
        const group = groupOf(node);
        rootsByGroup.set(group, [...(rootsByGroup.get(group) ?? []), node]);
    }

    const result: MenuGroupNode[] = [...(rootsByGroup.get(mainAppCode) ?? [])];
    const knownCodes = new Set<string>();
    const appendGroup = (code: string, name: string, app?: MenuGroupApp) => {
        result.push({
            id: `${VIRTUAL_APP_ID_PREFIX}${code}`,
            appCode: code,
            parentId: "0",
            menuCode: `app:${code}`,
            menuName: name,
            menuType: "DIR",
            path: app?.basePath ?? "",
            component: "",
            permission: "",
            icon: app?.icon,
            sortNo: app?.sortNo ?? 0,
            hidden: 1,
            status: 1,
            children: rootsByGroup.get(code) ?? [],
            virtual: true,
        });
    };

    for (const app of apps) {
        knownCodes.add(app.code);
        if (app.code !== mainAppCode) {
            appendGroup(app.code, app.name, app);
        }
    }
    for (const code of rootsByGroup.keys()) {
        if (code !== mainAppCode && !knownCodes.has(code)) {
            appendGroup(code, code);
        }
    }
    return result;
}
