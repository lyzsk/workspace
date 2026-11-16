package cn.sichu.quartz.service;

import cn.sichu.quartz.model.vo.CronValidationVO;
import org.quartz.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
@Service
public class CronExpressionService {

    private static final String RULE =
        "六字段：秒(0-59) 分(0-59) 时(0-23) 日(1-31) 月(1-12) 周(1-7)";

    public CronValidationVO validate(String expression) {
        String value = expression == null ? "" : expression.trim();
        if (!CronExpression.isValidExpression(value)) {
            return new CronValidationVO(false, "cron 表达式无效", RULE, List.of());
        }
        try {
            CronExpression cron = new CronExpression(value);
            List<LocalDateTime> nextTimes = new ArrayList<>();
            Date cursor = new Date();
            for (int i = 0; i < 5; i++) {
                cursor = cron.getNextValidTimeAfter(cursor);
                if (cursor == null) {
                    break;
                }
                nextTimes.add(LocalDateTime.ofInstant(cursor.toInstant(), ZoneId.systemDefault()));
            }
            return new CronValidationVO(true, "表达式合法", RULE, nextTimes);
        } catch (java.text.ParseException | IllegalArgumentException e) {
            return new CronValidationVO(false, e.getMessage(), RULE, List.of());
        }
    }
}
