import type { PageResult } from "@/types/api";
import type { Equipment, EquipmentQuery } from "@/api/equipment";

/**
 * 本地模拟数据。
 *
 * 用途只有一个：后端接口没就绪时，模板仍能打开、能翻页、能搜索，
 * 让接入者先看到页面形状，而不是一进来就一片报错。
 *
 * 接入真实后端后，把 .env 里的 VITE_USE_MOCK 改为 false 即可，不需要删代码。
 */

const STATUS_POOL: Equipment["status"][] = ["运行", "待机", "维修", "停用"];

const MOCK_EQUIPMENT: Equipment[] = Array.from({ length: 46 }, (_, index) => {
    const seq = index + 1;
    const code = `EQ-${String(seq).padStart(4, "0")}`;
    return {
        id: seq,
        code,
        name: `示例设备 ${seq}`,
        model: `M-${1000 + seq}`,
        status: STATUS_POOL[index % STATUS_POOL.length],
        owner: index % 2 === 0 ? "张工" : "李工",
        updateTime: `2026-11-${String((index % 28) + 1).padStart(2, "0")} 09:30:00`,
    };
});

/** 模拟带搜索与分页的列表查询 */
export function mockListEquipment(query: EquipmentQuery): Promise<PageResult<Equipment>> {
    const keyword = (query.keyword || "").trim().toLowerCase();

    const filtered = keyword
        ? MOCK_EQUIPMENT.filter(
              (item) =>
                  item.code.toLowerCase().includes(keyword) ||
                  item.name.toLowerCase().includes(keyword),
          )
        : MOCK_EQUIPMENT;

    const start = (query.pageNum - 1) * query.pageSize;
    const rows = filtered.slice(start, start + query.pageSize);

    // 模拟网络延迟，便于观察表格的加载状态
    return new Promise((resolve) => {
        window.setTimeout(() => {
            resolve({ rows, total: filtered.length });
        }, 200);
    });
}
