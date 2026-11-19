package cn.sichu.system.sync;

import cn.sichu.common.core.result.PageResult;
import cn.sichu.system.sync.model.vo.InternalUserSyncVO;

import java.time.LocalDateTime;

/**
 *
 * @author sichu huang
 * @since 2026/11/17
 */
public interface IInternalUserSyncService {

    PageResult<InternalUserSyncVO> pageAuthorizedUsers(String appCode, LocalDateTime updatedAfter,
        long page, int size);
}
