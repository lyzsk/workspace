package cn.sichu.system.auth.service;

import cn.sichu.system.auth.model.LoginAuthentication;
import cn.sichu.system.auth.model.form.LoginForm;
import cn.sichu.system.auth.model.form.PasswordForm;
import cn.sichu.system.auth.model.form.ProfileForm;
import cn.sichu.system.auth.model.vo.UserInfoVO;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public interface IAuthService {

    LoginAuthentication login(LoginForm form);

    LoginAuthentication refresh(String refreshToken);

    void logout(String accessToken, String refreshToken);

    UserInfoVO currentUser();

    UserInfoVO updateProfile(ProfileForm form);

    void changePassword(PasswordForm form);
}
