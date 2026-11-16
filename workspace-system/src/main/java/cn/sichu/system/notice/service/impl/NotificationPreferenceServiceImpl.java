package cn.sichu.system.notice.service.impl;

import cn.sichu.common.exception.BusinessException;
import cn.sichu.common.result.ResultCode;
import cn.sichu.common.base.BaseImpl;
import cn.sichu.system.notice.entity.SysNotificationPreference;
import cn.sichu.system.notice.mapper.SysNotificationPreferenceMapper;
import cn.sichu.system.notice.model.form.SysNotificationPreferenceForm;
import cn.sichu.system.notice.service.INotificationPreferenceService;
import cn.sichu.system.security.model.LoginUser;
import cn.sichu.system.security.util.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Objects;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Service
@RequiredArgsConstructor
public class NotificationPreferenceServiceImpl extends
    BaseImpl<SysNotificationPreferenceMapper, SysNotificationPreference>
    implements INotificationPreferenceService {

    @Override
    public SysNotificationPreference getMy() {
        LoginUser current = SecurityUtils.getLoginUser();
        SysNotificationPreference preference = find(current);
        if (preference != null) {
            return preference;
        }
        SysNotificationPreference defaults = new SysNotificationPreference();
        defaults.setUserId(current.getUserId());
        defaults.setQuietEnabled(0);
        defaults.setTimezone("Asia/Shanghai");
        return defaults;
    }

    @Override
    public SysNotificationPreference updateMy(SysNotificationPreferenceForm form) {
        if (form.getQuietEnabled() != null && form.getQuietEnabled() == 1
            && (form.getQuietStart() == null || form.getQuietEnd() == null
            || Objects.equals(form.getQuietStart(), form.getQuietEnd()))) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "免打扰起止时间不能为空或相同");
        }
        try {
            ZoneId.of(form.getTimezone());
        } catch (DateTimeException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "时区不正确");
        }
        LoginUser current = SecurityUtils.getLoginUser();
        SysNotificationPreference preference = find(current);
        if (preference == null) {
            preference = new SysNotificationPreference();
            preference.setTenantId(current.getTenantId());
            preference.setUserId(current.getUserId());
        }
        preference.setQuietEnabled(form.getQuietEnabled() == null ? 0 : form.getQuietEnabled());
        preference.setQuietStart(form.getQuietStart());
        preference.setQuietEnd(form.getQuietEnd());
        preference.setTimezone(form.getTimezone());
        if (preference.getId() == null) {
            baseMapper.insert(preference);
        } else {
            baseMapper.updateById(preference);
        }
        return preference;
    }

    private SysNotificationPreference find(LoginUser current) {
        return baseMapper.selectOne(new LambdaQueryWrapper<SysNotificationPreference>()
            .eq(SysNotificationPreference::getUserId, current.getUserId())
            .eq(SysNotificationPreference::getTenantId, current.getTenantId()));
    }
}
