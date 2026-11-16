package cn.sichu.system.app.service;

import cn.sichu.system.app.model.vo.EapToolServiceTicketVO;
import cn.sichu.system.security.model.LoginUser;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
public interface IEapToolServiceTicketService {

    EapToolServiceTicketVO issue(LoginUser user);
}
