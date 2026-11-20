import { AppConfig } from "@/config";
import { get } from "@/api/http";
import { mockListEquipment } from "@/api/mock/equipment";
import type { PageQuery, PageResult } from "@/types/api";

/**
 * 设备清单接口。
 *
 * 这是模板自带的示例业务，用来演示三件事：
 * 1. 接口定义怎么写（类型、参数、返回值）
 * 2. mock 与真实接口怎么一键切换
 * 3. 页面里怎么调用（见 views/equipment/index.vue）
 *
 * 接入自己的业务时，照着这个文件复制一份改名即可。
 */

/** 设备实体 */
export interface Equipment {
    id: number;
    /** 设备编号 */
    code: string;
    /** 设备名称 */
    name: string;
    /** 设备型号 */
    model: string;
    /** 设备状态 */
    status: "运行" | "待机" | "维修" | "停用";
    /** 责任人 */
    owner: string;
    /** 最后更新时间 */
    updateTime: string;
}

/** 设备查询参数 */
export interface EquipmentQuery extends PageQuery {
    /** 关键字，匹配设备编号或名称 */
    keyword?: string;
}

/** 查询设备清单 */
export function listEquipment(query: EquipmentQuery): Promise<PageResult<Equipment>> {
    if (AppConfig.micro.useMock) {
        return mockListEquipment(query);
    }
    return get<PageResult<Equipment>>("/equipment/list", query);
}
