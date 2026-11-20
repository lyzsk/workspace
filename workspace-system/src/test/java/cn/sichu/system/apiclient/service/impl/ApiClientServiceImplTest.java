package cn.sichu.system.apiclient.service.impl;

import cn.sichu.system.apiclient.entity.SysApiClient;
import cn.sichu.system.apiclient.mapper.SysApiClientMapper;
import cn.sichu.system.apiclient.model.form.ApiClientForm;
import cn.sichu.system.apiclient.model.vo.ApiClientSecretVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
class ApiClientServiceImplTest {

    private final SysApiClientMapper clientMapper = mock(SysApiClientMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final ApiClientServiceImpl service =
        new ApiClientServiceImpl(clientMapper, passwordEncoder);

    @Test
    void createReturnsSecretOnlyOnce() {
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        ApiClientForm form = new ApiClientForm();
        form.setClientName("DSH");

        ApiClientSecretVO result = service.create(form);

        assertTrue(result.getClientId().startsWith("cli_"));
        assertNotNull(result.getClientSecret());
        assertNotEquals("hash", result.getClientSecret());
        verify(clientMapper).insert(any(SysApiClient.class));
    }

    @Test
    void verifyRejectsWrongSecret() {
        when(clientMapper.selectOne(any())).thenReturn(client());
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        assertTrue(service.verify("cli_x", "bad").isEmpty());
    }

    @Test
    void verifyAcceptsValidSecretAndRecordsUsage() {
        when(clientMapper.selectOne(any())).thenReturn(client());
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);

        assertTrue(service.verify("cli_x", "secret").isPresent());
        verify(clientMapper).updateById(any(SysApiClient.class));
    }

    private SysApiClient client() {
        SysApiClient entity = new SysApiClient();
        entity.setId(1L);
        entity.setClientId("cli_x");
        entity.setSecretHash("hash");
        entity.setStatus(1);
        return entity;
    }
}
