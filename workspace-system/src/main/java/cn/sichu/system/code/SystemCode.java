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
    USER_NOT_FOUND("A1001", "用户不存在"), USERNAME_EXISTS("A1002",
        "用户名已存在"), PASSWORD_REQUIRED("A1003", "密码不能为空"), LOGIN_FAILED("A1004",
        "用户名或密码错误"), ACCOUNT_DISABLED("A1005", "账号已停用"), ACCOUNT_LOCKED("A1006",
        "账号已锁定，请稍后再试"), ROLE_NOT_FOUND("A1007", "角色不存在"), ROLE_CODE_EXISTS(
        "A1008", "角色编码已存在"), ROLE_PROTECTED("A1009", "超级管理员角色不允许修改或删除"),
        MENU_NOT_FOUND("A1010", "菜单不存在"), MENU_CODE_EXISTS("A1011", "菜单编码已存在"),
        MENU_HAS_CHILDREN("A1012", "存在子菜单，不能删除"), DEPT_NOT_FOUND("A1013",
        "部门不存在"), DEPT_CODE_EXISTS("A1014", "部门编码已存在"), DEPT_HAS_CHILDREN("A1015",
        "存在下级部门，不能删除"), DICT_TYPE_NOT_FOUND("A1016", "字典类型不存在"),
        DICT_TYPE_EXISTS("A1017", "字典类型已存在"), DICT_DATA_NOT_FOUND("A1018",
        "字典数据不存在"), DICT_DATA_EXISTS("A1019", "字典值已存在"), DICT_TYPE_HAS_DATA(
        "A1020", "字典类型下存在数据，不能删除"), NOTICE_NOT_FOUND("A1021", "通知不存在"),
        NOTICE_ALREADY_PUBLISHED("A1022", "通知已发布，不能重复发布"), NOTICE_TARGET_INVALID(
        "A1023", "通知发布对象无效"), MESSAGE_NOT_FOUND("A1024", "消息不存在"),
        APP_NOT_FOUND("A1025", "应用不存在"), APP_CODE_EXISTS("A1026", "应用编码已存在"),
        APP_BASE_PATH_EXISTS("A1027", "应用路由前缀已存在"), APP_TYPE_INVALID("A1028",
        "应用接入方式不合法"), APP_PROTECTED("A1029", "平台内置应用不允许修改编码或删除"),
        APP_HAS_MENUS("A1030", "应用下存在菜单，不能删除"), APP_ENTRY_URL_REQUIRED("A1031",
        "微前端模块入口不能为空"), APP_CODE_IMMUTABLE("A1032", "应用下存在菜单，不能修改编码"),
        ROLE_IN_USE("A1033", "角色已分配给用户，不能删除"),
        USER_PROTECTED("A1034", "内置管理员账号不允许在用户管理中修改或删除"),
        DEPT_PROTECTED("A1035", "总公司不允许修改或删除");

    private final String code;
    private final String msg;

    SystemCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
