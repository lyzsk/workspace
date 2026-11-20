package cn.sichu.system.apiclient.service.impl;

import cn.sichu.common.core.exception.BusinessException;
import cn.sichu.common.core.result.ResultCode;
import cn.sichu.system.apiclient.entity.SysApiClient;
import cn.sichu.system.apiclient.mapper.SysApiClientMapper;
import cn.sichu.system.apiclient.model.form.ApiClientForm;
import cn.sichu.system.apiclient.model.vo.ApiClientSecretVO;
import cn.sichu.system.apiclient.model.vo.ApiClientVO;
import cn.sichu.system.apiclient.service.IApiClientService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author sichu huang
 * @since 2026/11/20
 */
@Service
@RequiredArgsConstructor
public class ApiClientServiceImpl implements IApiClientService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SysApiClientMapper clientMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<ApiClientVO> list() {
        return clientMapper.selectList(new LambdaQueryWrapper<SysApiClient>()
                .orderByDesc(SysApiClient::getCreateTime))
            .stream().map(this::toVO).toList();
    }

    @Override
    public ApiClientSecretVO create(ApiClientForm form) {
        String clientId = "cli_" + randomToken(12);
        String secret = randomToken(32);
        SysApiClient entity = new SysApiClient();
        entity.setTenantId(0L);
        entity.setClientId(clientId);
        entity.setClientName(form.getClientName());
        entity.setSecretHash(passwordEncoder.encode(secret));
        entity.setScopes(form.getScopes());
        entity.setStatus(form.getStatus() == null ? 1 : form.getStatus());
        entity.setExpiresAt(form.getExpiresAt());
        entity.setRemark(form.getRemark());
        clientMapper.insert(entity);
        return new ApiClientSecretVO(entity.getId() == null ? null : entity.getId().toString(),
            clientId, secret, "密钥只显示这一次，请立即保存");
    }

    @Override
    public void update(Long id, ApiClientForm form) {
        SysApiClient entity = require(id);
        entity.setClientName(form.getClientName());
        entity.setScopes(form.getScopes());
        entity.setStatus(form.getStatus());
        entity.setExpiresAt(form.getExpiresAt());
        entity.setRemark(form.getRemark());
        clientMapper.updateById(entity);
    }

    @Override
    public ApiClientSecretVO resetSecret(Long id) {
        SysApiClient entity = require(id);
        String secret = randomToken(32);
        entity.setSecretHash(passwordEncoder.encode(secret));
        clientMapper.updateById(entity);
        return new ApiClientSecretVO(entity.getId().toString(), entity.getClientId(), secret,
            "密钥已重置，旧密钥立即失效");
    }

    @Override
    public void delete(Long id) {
        require(id);
        clientMapper.deleteById(id);
    }

    @Override
    public Optional<SysApiClient> verify(String clientId, String secret) {
        if (clientId == null || clientId.isBlank() || secret == null || secret.isBlank()) {
            return Optional.empty();
        }
        SysApiClient entity = clientMapper.selectOne(new LambdaQueryWrapper<SysApiClient>()
            .eq(SysApiClient::getClientId, clientId));
        if (entity == null || !Integer.valueOf(1).equals(entity.getStatus())) {
            return Optional.empty();
        }
        if (entity.getExpiresAt() != null
            && entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        if (!passwordEncoder.matches(secret, entity.getSecretHash())) {
            return Optional.empty();
        }
        entity.setLastUsedAt(LocalDateTime.now());
        clientMapper.updateById(entity);
        return Optional.of(entity);
    }

    private SysApiClient require(Long id) {
        SysApiClient entity = clientMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "调用方不存在");
        }
        return entity;
    }

    private ApiClientVO toVO(SysApiClient entity) {
        ApiClientVO vo = new ApiClientVO();
        vo.setId(entity.getId() == null ? null : entity.getId().toString());
        vo.setClientId(entity.getClientId());
        vo.setClientName(entity.getClientName());
        vo.setScopes(entity.getScopes());
        vo.setStatus(entity.getStatus());
        vo.setExpiresAt(entity.getExpiresAt());
        vo.setLastUsedAt(entity.getLastUsedAt());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private String randomToken(int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
