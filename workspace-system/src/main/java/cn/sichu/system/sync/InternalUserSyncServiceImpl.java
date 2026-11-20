package cn.sichu.system.sync;

import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.common.core.result.PageResult;
import cn.sichu.common.core.result.ResultCode;
import cn.sichu.system.sync.model.vo.InternalUserSyncVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InternalUserSyncServiceImpl implements IInternalUserSyncService {

    private final InternalUserSyncMapper syncMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<InternalUserSyncVO> pageAuthorizedUsers(String appCode,
        LocalDateTime updatedAfter, long page, int size) {
        if (page < 1 || page > 1_000_000 || size < 1 || size > 500) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分页参数不合法");
        }
        long offset = (page - 1) * (long) size;
        long total = syncMapper.countAuthorizedUsers(appCode, updatedAfter);
        List<InternalUserSyncVO> records = total == 0 ? List.of()
            : syncMapper.selectAuthorizedUsers(appCode, updatedAfter, offset, size);
        log.info("内部用户同步 appCode={} updatedAfter={} page={} size={} total={}", appCode,
            updatedAfter, page, size, total);
        return PageResult.of(records, total, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public InternalUserSyncVO findAuthorizedUser(String appCode, String username) {
        return syncMapper.selectAuthorizedUser(appCode, username);
    }
}
