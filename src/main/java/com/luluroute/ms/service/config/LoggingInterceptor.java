package com.luluroute.ms.service.config;

import com.luluroute.ms.service.util.ShipmentConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class LoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String reqEntityCode = request.getHeader("X-Entity-Code");
        if (StringUtils.isNotBlank(reqEntityCode)) {
            MDC.put(ShipmentConstants.CODE_PATH, reqEntityCode);
        } else {
            MDC.put(ShipmentConstants.CODE_PATH, "user");
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
        MDC.remove(ShipmentConstants.CODE_PATH);
    }
}