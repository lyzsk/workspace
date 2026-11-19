package cn.sichu.system.security.service;

import cn.sichu.system.security.model.DataScopeFilter;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public interface IDataScopeService {

    DataScopeFilter current();

    DataScopeFilter current(String appCode);
}
