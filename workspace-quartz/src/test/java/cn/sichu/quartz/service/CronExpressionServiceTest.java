package cn.sichu.quartz.service;

import cn.sichu.quartz.model.vo.CronValidationVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
class CronExpressionServiceTest {

    private final CronExpressionService service = new CronExpressionService();

    @Test
    void validExpressionReturnsFiveExecutionTimes() {
        CronValidationVO result = service.validate("0 0/5 * * * ?");

        assertTrue(result.valid());
        assertEquals(5, result.nextTimes().size());
        for (int i = 1; i < result.nextTimes().size(); i++) {
            assertTrue(result.nextTimes().get(i).isAfter(result.nextTimes().get(i - 1)));
        }
    }

    @Test
    void invalidExpressionReturnsFailureWithoutTimes() {
        CronValidationVO result = service.validate("not-a-cron");

        assertFalse(result.valid());
        assertTrue(result.nextTimes().isEmpty());
    }
}
