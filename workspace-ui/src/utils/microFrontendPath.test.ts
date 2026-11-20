import { describe, expect, it } from "vitest";
import {
    resolveMicroHostPath,
    resolveMicroInitialPath,
    resolveMicroInternalPath,
} from "./microFrontendPath";

describe("microFrontendPath", () => {
    it("resolves internal paths from host deep links", () => {
        expect(resolveMicroInitialPath("/apps/demo", "/apps/demo")).toBe("/");
        expect(resolveMicroInitialPath("/apps/demo/orders/12?tab=items#top", "/apps/demo"))
            .toBe("/orders/12?tab=items#top");
    });

    it("accepts internal paths without a leading slash", () => {
        expect(resolveMicroInternalPath("orders?tab=items", "/apps/demo"))
            .toBe("/orders?tab=items");
    });

    it("does not duplicate the base path when a child sends a full host path", () => {
        expect(resolveMicroHostPath("/apps/demo", "/apps/demo/orders?tab=items"))
            .toBe("/apps/demo/orders?tab=items");
    });

    it("joins internal paths and preserves query and hash", () => {
        expect(resolveMicroHostPath("/apps/demo/", "/orders?tab=items#top"))
            .toBe("/apps/demo/orders?tab=items#top");
        expect(resolveMicroHostPath("/apps/demo", "/")).toBe("/apps/demo");
    });
});
