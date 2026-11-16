package cn.sichu.system.online.service;

import cn.sichu.system.online.model.vo.SysOnlineUserVO;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public interface IOnlineUserService {

    List<SysOnlineUserVO> listOnline();

    void kick(Long userId);
}
