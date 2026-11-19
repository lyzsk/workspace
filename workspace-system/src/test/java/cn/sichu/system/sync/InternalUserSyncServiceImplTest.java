package cn.sichu.system.sync;

import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.common.core.result.PageResult;
import cn.sichu.system.sync.model.vo.InternalUserSyncVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/17
 */
class InternalUserSyncServiceImplTest {

    private final InternalUserSyncMapper mapper = mock(InternalUserSyncMapper.class);
    private final InternalUserSyncServiceImpl service = new InternalUserSyncServiceImpl(mapper);

    @Test
    void pagesOnlyThroughAuthorizedUserMapper() {
        LocalDateTime updatedAfter = LocalDateTime.parse("2026-11-17T10:00:00");
        InternalUserSyncVO user = new InternalUserSyncVO();
        user.setWorkspaceUserId(1L);
        user.setPasswordHash("$2a$12$example");
        when(mapper.countAuthorizedUsers("eap", updatedAfter)).thenReturn(1L);
        when(mapper.selectAuthorizedUsers("eap", updatedAfter, 40, 20))
            .thenReturn(List.of(user));

        PageResult<InternalUserSyncVO> result =
            service.pageAuthorizedUsers("eap", updatedAfter, 3, 20);

        assertEquals(1, result.getTotal());
        assertEquals(3, result.getPageNum());
        assertEquals(1, result.getRecords().size());
        verify(mapper).selectAuthorizedUsers("eap", updatedAfter, 40, 20);
    }

    @Test
    void rejectsOversizedPageSize() {
        assertThrows(BusinessException.class,
            () -> service.pageAuthorizedUsers("eap", null, 1, 501));

        verifyNoInteractions(mapper);
    }
}
