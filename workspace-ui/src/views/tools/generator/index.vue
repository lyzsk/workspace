<template>
    <div class="generator-page">
        <el-card shadow="never">
            <template #header>代码生成</template>
            <el-form :model="form" label-width="110px">
                <div class="generator-grid">
                    <el-form-item label="Schema" required>
                        <el-input v-model.trim="form.schemaName" @blur="loadTables" />
                    </el-form-item>
                    <el-form-item label="数据表" required>
                        <el-select
                            v-model="form.tableName"
                            filterable
                            style="width: 100%"
                            @change="loadColumns"
                        >
                            <el-option
                                v-for="item in tables"
                                :key="item"
                                :label="item"
                                :value="item"
                            />
                        </el-select>
                    </el-form-item>
                    <el-form-item label="模块名" required>
                        <el-input v-model.trim="form.moduleName" placeholder="如 ies" />
                    </el-form-item>
                    <el-form-item label="业务名" required>
                        <el-input v-model.trim="form.businessName" placeholder="如 device" />
                    </el-form-item>
                    <el-form-item label="后端包名" required>
                        <el-input
                            v-model.trim="form.packageName"
                            placeholder="如 cn.sichu.ies.device"
                        />
                    </el-form-item>
                    <el-form-item label="类名">
                        <el-input v-model.trim="form.className" placeholder="默认按表名转换" />
                    </el-form-item>
                    <el-form-item label="功能名称" required>
                        <el-input v-model.trim="form.functionName" placeholder="如 设备管理" />
                    </el-form-item>
                    <el-form-item label="权限前缀" required>
                        <el-input v-model.trim="form.permissionPrefix" placeholder="如 ies:device" />
                    </el-form-item>
                    <el-form-item label="继承租户实体">
                        <el-switch v-model="form.tenantScoped" />
                    </el-form-item>
                </div>
            </el-form>
            <div class="generator-actions">
                <el-button type="primary" :loading="previewLoading" @click="preview">
                    预览代码
                </el-button>
                <el-button :loading="downloadLoading" @click="downloadZip">
                    下载 ZIP
                </el-button>
            </div>
        </el-card>

        <el-card shadow="never">
            <template #header>字段信息（{{ columns.length }}）</template>
            <el-table :data="columns" border>
                <el-table-column prop="name" label="字段" min-width="160" />
                <el-table-column prop="dataType" label="数据库类型" min-width="150" />
                <el-table-column prop="comment" label="注释" min-width="160" />
                <el-table-column label="可空" width="80">
                    <template #default="{ row }">{{ row.nullable ? "是" : "否" }}</template>
                </el-table-column>
            </el-table>
        </el-card>

        <el-dialog v-model="previewVisible" title="代码预览" width="90%">
            <el-tabs v-model="activeFile" tab-position="left" class="preview-tabs">
                <el-tab-pane
                    v-for="(content, path) in files"
                    :key="path"
                    :label="path"
                    :name="path"
                >
                    <pre class="code-preview">{{ content }}</pre>
                </el-tab-pane>
            </el-tabs>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import {
    downloadGenerator,
    listGeneratorColumns,
    listGeneratorTables,
    previewGenerator,
    type GeneratorColumn,
    type GeneratorRequest,
} from "@/api/system/generator";

defineOptions({ name: "WorkspaceGenerator" });

const tables = ref<string[]>([]);
const columns = ref<GeneratorColumn[]>([]);
const files = ref<Record<string, string>>({});
const activeFile = ref("");
const previewVisible = ref(false);
const previewLoading = ref(false);
const downloadLoading = ref(false);

const form = reactive<GeneratorRequest>({
    schemaName: "system",
    tableName: "",
    moduleName: "",
    businessName: "",
    packageName: "cn.sichu",
    functionName: "",
    permissionPrefix: "",
    className: "",
    tenantScoped: true,
});

function toClassName(tableName: string) {
    return tableName
        .split("_")
        .filter(Boolean)
        .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
        .join("");
}

async function loadTables() {
    tables.value = await listGeneratorTables(form.schemaName);
}

async function loadColumns() {
    if (!form.tableName) {
        return;
    }
    columns.value = await listGeneratorColumns(form.schemaName, form.tableName);
    if (!form.className) {
        form.className = toClassName(form.tableName);
    }
    if (!form.businessName) {
        form.businessName = form.tableName.replace(/^[a-z]+_/, "");
    }
    if (!form.moduleName) {
        form.moduleName = form.schemaName;
    }
    if (form.packageName === "cn.sichu") {
        form.packageName = `cn.sichu.${form.moduleName}.${form.businessName}`;
    }
}

async function preview() {
    previewLoading.value = true;
    try {
        files.value = await previewGenerator(form);
        activeFile.value = Object.keys(files.value)[0] ?? "";
        previewVisible.value = true;
    } catch (error) {
        ElMessage.error(error instanceof Error ? error.message : "生成预览失败");
    } finally {
        previewLoading.value = false;
    }
}

async function downloadZip() {
    downloadLoading.value = true;
    try {
        await downloadGenerator(form);
    } finally {
        downloadLoading.value = false;
    }
}

onMounted(loadTables);
</script>

<style scoped>
.generator-page {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.generator-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0 24px;
}

.generator-actions {
    display: flex;
    gap: 8px;
}

.preview-tabs {
    height: 70vh;
    overflow: auto;
}

.code-preview {
    margin: 0;
    overflow: auto;
    font-family: monospace;
    font-size: 13px;
    line-height: 1.6;
    white-space: pre;
}
</style>
