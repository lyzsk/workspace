package cn.sichu.auth.service;

import cn.sichu.auth.model.form.LoginForm;
import cn.sichu.auth.model.form.PasswordForm;
import cn.sichu.auth.model.form.ProfileForm;
import cn.sichu.auth.model.vo.LoginAuthentication;
import cn.sichu.auth.model.vo.UserInfoVO;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
public interface IAuthService {

    LoginAuthentication login(LoginForm form);

    LoginAuthentication refresh(String accessToken);

    void logout(String accessToken);

    void touch(String accessToken);

    UserInfoVO currentUser();

    UserInfoVO updateProfile(ProfileForm form);

    void changePassword(PasswordForm form);
}
