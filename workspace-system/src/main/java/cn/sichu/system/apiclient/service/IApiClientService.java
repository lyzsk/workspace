package cn.sichu.system.apiclient.service;

import cn.sichu.system.apiclient.entity.SysApiClient;
import cn.sichu.system.apiclient.model.form.ApiClientForm;
import cn.sichu.system.apiclient.model.vo.ApiClientSecretVO;
import cn.sichu.system.apiclient.model.vo.ApiClientVO;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
public interface IApiClientService {

    List<ApiClientVO> list();

    ApiClientSecretVO create(ApiClientForm form);

    void update(Long id, ApiClientForm form);

    ApiClientSecretVO resetSecret(Long id);

    void delete(Long id);

    Optional<SysApiClient> verify(String clientId, String secret);
}
