#!/usr/bin/env node
import {
    createCipheriv,
    createDecipheriv,
    createPrivateKey,
    generateKeyPairSync,
    randomBytes,
    scryptSync,
    sign,
} from "node:crypto";
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";

function parseArgs(argv) {
    const result = {};
    for (let index = 0; index < argv.length; index += 1) {
        const item = argv[index];
        if (!item.startsWith("--")) {
            continue;
        }
        const key = item.slice(2);
        const value = argv[index + 1];
        if (value === undefined || value.startsWith("--")) {
            result[key] = true;
        } else {
            result[key] = value;
            index += 1;
        }
    }
    return result;
}

function passphrase() {
    return process.env.LICENSE_KEY_PASSPHRASE || null;
}

function encryptPrivateKey(der, password) {
    const salt = randomBytes(16);
    const iv = randomBytes(12);
    const key = scryptSync(password, salt, 32);
    const cipher = createCipheriv("aes-256-gcm", key, iv);
    const data = Buffer.concat([cipher.update(der), cipher.final()]);
    return {
        v: 1,
        alg: "aes-256-gcm",
        salt: salt.toString("base64"),
        iv: iv.toString("base64"),
        tag: cipher.getAuthTag().toString("base64"),
        data: data.toString("base64"),
    };
}

function decryptPrivateKey(record, password) {
    const key = scryptSync(password, Buffer.from(record.salt, "base64"), 32);
    const decipher = createDecipheriv("aes-256-gcm", key, Buffer.from(record.iv, "base64"));
    decipher.setAuthTag(Buffer.from(record.tag, "base64"));
    return Buffer.concat([
        decipher.update(Buffer.from(record.data, "base64")),
        decipher.final(),
    ]);
}

function loadPrivateKey(path) {
    const text = readFileSync(path, "utf8").trim();
    let der;
    if (text.startsWith("{")) {
        const password = passphrase();
        if (!password) {
            throw new Error("私钥已加密，请设置 LICENSE_KEY_PASSPHRASE");
        }
        der = decryptPrivateKey(JSON.parse(text), password);
    } else {
        der = Buffer.from(text, "base64");
    }
    return createPrivateKey({ key: der, format: "der", type: "pkcs8" });
}

function keygen(args) {
    const outDir = args.out || ".";
    mkdirSync(outDir, { recursive: true });
    const { privateKey, publicKey } = generateKeyPairSync("ed25519");
    const privateDer = privateKey.export({ type: "pkcs8", format: "der" });
    const publicDer = publicKey.export({ type: "spki", format: "der" });
    const rawPublic = publicDer.subarray(publicDer.length - 32);
    const password = passphrase();
    const privateText = password
        ? JSON.stringify(encryptPrivateKey(privateDer, password), null, 2)
        : privateDer.toString("base64");
    writeFileSync(join(outDir, "license-private.key"), privateText, { mode: 0o600 });
    writeFileSync(join(outDir, "license-public.key"), rawPublic.toString("base64"));
    console.log(`private key: ${join(outDir, "license-private.key")}`);
    console.log(`public key : ${join(outDir, "license-public.key")}`);
    console.log("公钥写入 workspace-system 的 LICENSE_PUBLIC_KEY；私钥只留在本机。");
}

function buildEnvelope(args, status) {
    const keyPath = args.key;
    const machine = args.machine;
    const licenseId = args["license-id"];
    const serial = Number(args.serial);
    if (!keyPath || !machine || !licenseId || !Number.isFinite(serial) || serial <= 0) {
        throw new Error("缺少 --key / --machine / --license-id / --serial");
    }
    const hours = Number(args.hours || (status === "REVOKED" ? 24 : 168));
    const expiresAt = args["expires-at"]
        || new Date(Date.now() + hours * 3600 * 1000).toISOString();
    const payload = {
        formatVersion: 1,
        licenseId,
        serial,
        status,
        machineCode: machine,
        issuedAt: new Date().toISOString(),
        expiresAt,
        features: args.features ? String(args.features).split(",").filter(Boolean) : [],
        licensee: args.licensee || "",
    };
    const bytes = Buffer.from(JSON.stringify(payload), "utf8");
    const privateKey = loadPrivateKey(keyPath);
    const signature = sign(null, bytes, privateKey);
    return JSON.stringify({
        payload: bytes.toString("base64"),
        signature: signature.toString("base64"),
    }, null, 2);
}

function writeEnvelope(args, status) {
    const envelope = buildEnvelope(args, status);
    const out = args.out || (status === "REVOKED" ? "revoked.json" : "license.json");
    mkdirSync(dirname(out) || ".", { recursive: true });
    writeFileSync(out, envelope);
    console.log(`${status} envelope -> ${out}`);
    if (status === "REVOKED") {
        console.log("scp revoked.json <server>:/opt/workspace/license/license.json 后 60 秒内全平台拒绝");
    }
}

function main() {
    const [command, ...rest] = process.argv.slice(2);
    const args = parseArgs(rest);
    if (command === "keygen") {
        keygen(args);
        return;
    }
    if (command === "issue") {
        writeEnvelope(args, "ACTIVE");
        return;
    }
    if (command === "revoke") {
        writeEnvelope(args, "REVOKED");
        return;
    }
    console.log(`用法：
  node license-cli.mjs keygen --out keys
  node license-cli.mjs issue --key keys/license-private.key --machine <code> \\
      --license-id <id> --serial 1 --hours 168 --features workflow,todo --out license.json
  node license-cli.mjs revoke --key keys/license-private.key --machine <code> \\
      --license-id <id> --serial 2 --out revoked.json

私钥加密：set LICENSE_KEY_PASSPHRASE=...（Windows PowerShell: $env:LICENSE_KEY_PASSPHRASE="..."）`);
}

main();
