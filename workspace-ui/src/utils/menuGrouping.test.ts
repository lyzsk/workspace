import { describe, expect, it } from "vitest";
import type { MenuNode } from "@/types/system";
import {
    groupMenusByApp,
    isVirtualMenuId,
} from "./menuGrouping";

function menu(values: Partial<MenuNode> & Pick<MenuNode, "id" | "menuCode" | "menuName">): MenuNode {
    return {
        parentId: "0",
        menuType: "MENU",
        sortNo: 0,
        hidden: 1,
        status: 1,
        ...values,
    };
}

describe("menuGrouping", () => {
    it("groups menus under virtual application nodes including empty apps", () => {
        const result = groupMenusByApp(
            [menu({ id: "1", menuCode: "users", menuName: "用户" }),
                menu({ id: "2", menuCode: "orders", menuName: "订单", appCode: "app-a" })],
            [{ code: "app-a", name: "应用 A" }, { code: "app-b", name: "应用 B" }],
            "workspace",
        );

        expect(result.map((node) => node.id)).toEqual(["1", "app-app-a", "app-app-b"]);
        expect(result[1].children?.map((node) => node.id)).toEqual(["2"]);
        expect(result[2].children).toEqual([]);
        expect(isVirtualMenuId(result[1].id)).toBe(true);
    });

    it("promotes nodes whose parent belongs to another application", () => {
        const result = groupMenusByApp(
            [menu({ id: "1", menuCode: "root", menuName: "根", menuType: "DIR" }),
                menu({ id: "2", parentId: "1", menuCode: "child", menuName: "子页面", appCode: "app-a" })],
            [{ code: "app-a", name: "应用 A" }],
            "workspace",
        );

        expect(result[0].children).toEqual([]);
        expect(result[1].children?.map((node) => node.id)).toEqual(["2"]);
    });
});
