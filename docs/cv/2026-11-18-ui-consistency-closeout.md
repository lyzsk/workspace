# 2026-11-18 前端 UI 一致性收口实施记录

依据：`D:\dev\shhxinyuan\20261116\workspace-UI收口清单-2026-11-18.md`。已直接修改 `workspace-ui` 源码，以该清单的 S1–S7 和规范勘误为准。

## S1：Element Plus 主色

- 新增 `src/assets/styles/element-theme.scss`，主色为 `#5d87ff`。
- `:root` 使用与白色混合的浅色算法；`html.dark` 使用与 `#141414` 混合的 Element 深色算法，避免默认深色主题覆盖失效。
- `main.ts` 在 `element-plus/dist/index.css` 和 `element-plus/theme-chalk/dark/css-vars.css` 之后引入主题文件。

## S2–S4：尺寸与分页

- 13 处弹框宽度按清单归并为 `420 / 620 / 720 / 1000px`。
- 10 处表单标签统一为 `100px`。
- 字典数据、调度日志分页补齐 `sizes` 与 `@size-change`。

## S5：操作列

- 9 处宽度按清单调整；双图标列在该清单中也明确使用 90px。
- 字典、任务、缓存、通知、共享成员等操作列补齐 `fixed="right"`、`align="center"`、`class-name="action-cell"`。
- 日志列表中 `prop="operation"` 的“操作”字段是数据列，未误改为操作按钮列。

## S6：硬编码色值

- 通知成功/警告色、错误页成功/错误色、仪表盘成功色改用对应 CSS 变量。
- 角色删除菜单、`.text-btn.danger`、`.table-action.danger` 使用 `var(--art-error)`。
- 清单列出的登录页、API iframe、微前端兜底等例外均未修改。

## S7：表格文字按钮

- 通知列表“附件/发布”改为 `ButtonTable` 的 view/add 图标。
- 共享成员“移除”改为 `ButtonTable` 的 delete 图标。
- 字典类型文本链接保留。

## 按清单勘误恢复的内容

- 两处卡片内只读小表保留裸 `el-table`，未套 `TableCard`。
- 顶栏侧栏折叠与刷新继续使用框架层 `el-icon`。
- 通知中心非表格按钮保持原有 link 形式。
- 成功/失败标签继续使用 danger，不改成停用状态的 info。

## 验证

已执行：

```bash
npm --prefix "D:\dev\workspace\workspace-ui" run build
```

结果：`vue-tsc -b` 与 `vite build` 通过，最终输出 `✓ built in 3.46s`。

同时完成：

- `git diff --check` 通过。
- 构建后的 CSS 包含 `--el-color-primary:#5d87ff`。
- S1–S7 逐项反向扫描无遗漏。

构建仍有项目原有的 Lightning CSS `:deep` 伪类提示，不影响构建成功，本次未扩大范围处理。未启动服务，深浅色视觉效果及 1280 以下窄屏布局仍需浏览器人工验收。
