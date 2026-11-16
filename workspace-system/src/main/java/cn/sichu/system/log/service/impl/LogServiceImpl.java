package cn.sichu.system.log.service.impl;

import cn.sichu.common.result.PageResult;
import cn.sichu.common.utils.QuerySortUtils;
import cn.sichu.system.auth.entity.SysLoginLog;
import cn.sichu.system.auth.mapper.SysLoginLogMapper;
import cn.sichu.system.log.entity.SysOperLog;
import cn.sichu.system.log.mapper.SysOperLogMapper;
import cn.sichu.system.log.model.query.SysLoginLogQuery;
import cn.sichu.system.log.model.query.SysOperLogQuery;
import cn.sichu.system.log.model.vo.SysLoginLogVO;
import cn.sichu.system.log.model.vo.SysOperLogVO;
import cn.sichu.system.log.service.ILogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.sichu.common.base.BaseImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl extends BaseImpl<SysOperLogMapper, SysOperLog> implements ILogService {

    private final SysLoginLogMapper loginLogMapper;
    private final SysOperLogMapper operLogMapper;

    @Override
    public PageResult<SysLoginLogVO> pageLoginLogs(SysLoginLogQuery query) {
        Page<SysLoginLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<SysLoginLog> result = loginLogMapper.selectPage(page, loginLogWrapper(query));
        List<SysLoginLogVO> records = result.getRecords().stream().map(this::toLoginVO).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<SysOperLogVO> pageOperLogs(SysOperLogQuery query) {
        Page<SysOperLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<SysOperLog> result = operLogMapper.selectPage(page, operLogWrapper(query));
        List<SysOperLogVO> records = result.getRecords().stream().map(this::toOperVO).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public List<SysLoginLogVO> listLoginLogs(SysLoginLogQuery query) {
        return loginLogMapper.selectList(loginLogWrapper(query)).stream().map(this::toLoginVO)
            .toList();
    }

    @Override
    public List<SysOperLogVO> listOperLogs(SysOperLogQuery query) {
        return operLogMapper.selectList(operLogWrapper(query)).stream().map(this::toOperVO)
            .toList();
    }

    @Override
    public void record(SysOperLog operLog) {
        try {
            operLogMapper.insert(operLog);
        } catch (Exception e) {
            log.warn("写操作日志失败: {}", e.getMessage());
        }
    }

    private LambdaQueryWrapper<SysLoginLog> loginLogWrapper(SysLoginLogQuery query) {
        LambdaQueryWrapper<SysLoginLog> wrapper =
            new LambdaQueryWrapper<SysLoginLog>().like(StringUtils.hasText(query.getUsername()),
                    SysLoginLog::getUsername, query.getUsername())
                .eq(query.getStatus() != null, SysLoginLog::getStatus, query.getStatus())
                .ge(query.getBeginTime() != null, SysLoginLog::getLoginTime, query.getBeginTime())
                .le(query.getEndTime() != null, SysLoginLog::getLoginTime, query.getEndTime());
        boolean sorted = QuerySortUtils.apply(wrapper, query.getSortBy(), query.getOrder(),
            Map.of("username", SysLoginLog::getUsername, "status", SysLoginLog::getStatus, "loginTime",
                SysLoginLog::getLoginTime));
        if (!sorted) {
            wrapper.orderByDesc(SysLoginLog::getLoginTime);
        }
        return wrapper;
    }

    private LambdaQueryWrapper<SysOperLog> operLogWrapper(SysOperLogQuery query) {
        LambdaQueryWrapper<SysOperLog> wrapper =
            new LambdaQueryWrapper<SysOperLog>().like(StringUtils.hasText(query.getUsername()),
                    SysOperLog::getUsername, query.getUsername())
                .like(StringUtils.hasText(query.getModule()), SysOperLog::getModule, query.getModule())
                .eq(query.getStatus() != null, SysOperLog::getStatus, query.getStatus())
                .ge(query.getBeginTime() != null, SysOperLog::getOperTime, query.getBeginTime())
                .le(query.getEndTime() != null, SysOperLog::getOperTime, query.getEndTime());
        boolean sorted = QuerySortUtils.apply(wrapper, query.getSortBy(), query.getOrder(),
            Map.of("username", SysOperLog::getUsername, "module", SysOperLog::getModule, "status",
                SysOperLog::getStatus, "costTime", SysOperLog::getCostTime, "operTime",
                SysOperLog::getOperTime));
        if (!sorted) {
            wrapper.orderByDesc(SysOperLog::getOperTime);
        }
        return wrapper;
    }

    private SysLoginLogVO toLoginVO(SysLoginLog entry) {
        SysLoginLogVO vo = new SysLoginLogVO();
        vo.setId(entry.getId() == null ? null : entry.getId().toString());
        vo.setUsername(entry.getUsername());
        vo.setStatus(entry.getStatus());
        vo.setMessage(entry.getMessage());
        vo.setIp(entry.getIp());
        vo.setUserAgent(entry.getUserAgent());
        vo.setLoginTime(entry.getLoginTime());
        return vo;
    }

    private SysOperLogVO toOperVO(SysOperLog entry) {
        SysOperLogVO vo = new SysOperLogVO();
        vo.setId(entry.getId() == null ? null : entry.getId().toString());
        vo.setUsername(entry.getUsername());
        vo.setModule(entry.getModule());
        vo.setOperation(entry.getOperation());
        vo.setRequestMethod(entry.getRequestMethod());
        vo.setRequestUri(entry.getRequestUri());
        vo.setRequestParams(entry.getRequestParams());
        vo.setResponseResult(entry.getResponseResult());
        vo.setStatus(entry.getStatus());
        vo.setErrorMsg(entry.getErrorMsg());
        vo.setCostTime(entry.getCostTime());
        vo.setIp(entry.getIp());
        vo.setOperTime(entry.getOperTime());
        return vo;
    }
}
