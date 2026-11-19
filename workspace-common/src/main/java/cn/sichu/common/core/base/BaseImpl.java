package cn.sichu.common.core.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public abstract class BaseImpl<M extends BaseMapper<T>, T>
    extends ServiceImpl<M, T> {
}
