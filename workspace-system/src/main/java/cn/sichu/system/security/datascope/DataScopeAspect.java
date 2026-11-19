package cn.sichu.system.security.datascope;

import cn.sichu.system.security.model.DataScopeContext;
import cn.sichu.system.security.service.IDataScopeService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Aspect
@Component
@RequiredArgsConstructor
public class DataScopeAspect {

    private final IDataScopeService dataScopeService;

    @Around("@annotation(dataScope)")
    public Object around(ProceedingJoinPoint point, DataScope dataScope) throws Throwable {
        DataScopeContext.set(dataScopeService.current(dataScope.appCode()));
        try {
            return point.proceed();
        } finally {
            DataScopeContext.clear();
        }
    }
}
