package cn.sichu.system.code;

import cn.sichu.common.result.IResultCode;
import lombok.Getter;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Getter
public enum SystemCode implements IResultCode {
    USER_NOT_FOUND(404, "用户不存在"), USERNAME_EXISTS(409, "用户名已存在"), PASSWORD_REQUIRED(400,
        "密码不能为空"), LOGIN_FAILED(401, "用户名或密码错误"), ACCOUNT_DISABLED(403,
        "账号已停用"), ACCOUNT_LOCKED(403, "账号已锁定，请稍后再试"), ROLE_NOT_FOUND(404,
        "角色不存在"), ROLE_CODE_EXISTS(409, "角色编码已存在"), ROLE_PROTECTED(403,
        "超级管理员角色不允许修改或删除"), MENU_NOT_FOUND(404, "菜单不存在"), MENU_CODE_EXISTS(409,
        "菜单编码已存在"), MENU_HAS_CHILDREN(409, "存在子菜单，不能删除"), DEPT_NOT_FOUND(404,
        "部门不存在"), DEPT_CODE_EXISTS(409, "部门编码已存在"), DEPT_HAS_CHILDREN(409,
        "存在下级部门，不能删除"), DICT_TYPE_NOT_FOUND(404, "字典类型不存在"), DICT_TYPE_EXISTS(409,
        "字典类型已存在"), DICT_DATA_NOT_FOUND(404, "字典数据不存在"), DICT_DATA_EXISTS(409,
        "字典值已存在"), DICT_TYPE_HAS_DATA(409, "字典类型下存在数据，不能删除"), NOTICE_NOT_FOUND(404,
        "通知不存在"), NOTICE_ALREADY_PUBLISHED(409, "通知已发布，不能重复发布"),
    NOTICE_TARGET_INVALID(422, "通知发布对象无效"), MESSAGE_NOT_FOUND(404, "消息不存在"),
    APP_NOT_FOUND(404, "应用不存在"), APP_CODE_EXISTS(409, "应用编码已存在"),
    APP_BASE_PATH_EXISTS(409, "应用路由前缀已存在"), APP_PROTECTED(403,
        "平台内置应用不允许修改编码或删除"), APP_HAS_MENUS(409, "应用下存在菜单，不能删除"),
    APP_ENTRY_URL_REQUIRED(422, "微前端模块入口不能为空"), APP_CODE_IMMUTABLE(409,
        "应用下存在菜单，不能修改编码"), ROLE_IN_USE(409, "角色已分配给用户，不能删除"),
    USER_PROTECTED(403, "内置管理员账号不允许在用户管理中修改或删除"), DEPT_PROTECTED(403,
        "总公司不允许修改或删除");

    private final int code;
    private final String msg;

    SystemCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
