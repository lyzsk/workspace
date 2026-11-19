package cn.sichu.system.support;

import cn.sichu.system.user.entity.SysUser;
import cn.sichu.system.user.mapper.SysUserMapper;
import cn.sichu.tool.document.service.IDocumentUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Service
@RequiredArgsConstructor
public class DocumentUserServiceImpl implements IDocumentUserService {

    private final SysUserMapper userMapper;

    @Override
    public Map<Long, DocumentUser> findUsers(Collection<Long> userIds) {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getId, userIds)).stream()
            .map(this::toUser).collect(Collectors.toMap(DocumentUser::id, user -> user));
    }

    @Override
    public DocumentUser findUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        return user == null ? null : toUser(user);
    }

    private DocumentUser toUser(SysUser user) {
        return new DocumentUser(user.getId(), user.getUsername(), user.getRealName(),
            user.getStatus(), user.getTenantId());
    }
}
