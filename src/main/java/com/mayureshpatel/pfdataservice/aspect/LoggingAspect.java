package com.mayureshpatel.pfdataservice.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Arrays;

/**
 * Aspect for logging execution of service and repository Spring components.
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("within(@org.springframework.stereotype.Repository *)" +
            " || (within(@org.springframework.stereotype.Service *) && !within(com.mayureshpatel.pfdataservice.security..*))" +
            " || (within(@org.springframework.web.bind.annotation.RestController *) && !within(com.mayureshpatel.pfdataservice.controller.AuthenticationController))")
    public void springBeanPointcut() {
        // empty as this is just a pointcut; the implementations are in the advices
    }

    @Pointcut("within(com.mayureshpatel.pfdataservice..*)")
    public void applicationPackagePointcut() {
        // method is empty as this is just a pointcut; the implementations are in the advices.
    }

    @Around("applicationPackagePointcut() && springBeanPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        if (log.isTraceEnabled()) {
            log.trace("Enter: {}.{}() with argument[s] = {}", joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(), Arrays.toString(joinPoint.getArgs()));
        } else if (log.isDebugEnabled()) {
            log.debug("Enter: {}.{}()", joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName());
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();

            if (log.isTraceEnabled()) {
                log.trace("Exit: {}.{}() with result = {} (Execution time: {} ms)",
                        joinPoint.getSignature().getDeclaringTypeName(),
                        joinPoint.getSignature().getName(), result, stopWatch.getTotalTimeMillis());
            } else if (log.isDebugEnabled()) {
                log.debug("Exit: {}.{}() (Execution time: {} ms)",
                        joinPoint.getSignature().getDeclaringTypeName(),
                        joinPoint.getSignature().getName(), stopWatch.getTotalTimeMillis());
            }
            return result;
        } catch (IllegalArgumentException e) {
            log.error("Illegal argument in {}.{}() - Message: {}",
                    joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName(), e.getMessage());
            throw e;
        }
    }
}
