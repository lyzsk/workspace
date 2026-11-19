package cn.sichu.tool.document.service;

import java.util.Collection;
import java.util.Map;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public interface IDocumentUserService {

    Map<Long, DocumentUser> findUsers(Collection<Long> userIds);

    DocumentUser findUser(Long userId);

    record DocumentUser(Long id, String username, String realName, Integer status, Long tenantId) {
    }
}
