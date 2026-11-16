package cn.sichu.system.monitor.service;

import cn.sichu.system.monitor.model.vo.CacheEntryVO;
import cn.sichu.system.monitor.model.vo.CacheStatsVO;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public interface ICacheService {

    CacheEntryVO get(String key);

    void put(String key, String value);

    void delete(String key);

    void clear();

    CacheStatsVO stats();

    List<CacheEntryVO> list();
}
