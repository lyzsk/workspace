package cn.sichu.system.notice.service;

import cn.sichu.system.notice.entity.SysNotificationPreference;
import cn.sichu.system.notice.model.form.SysNotificationPreferenceForm;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
public interface INotificationPreferenceService {

    SysNotificationPreference getMy();

    SysNotificationPreference updateMy(SysNotificationPreferenceForm form);
}
