package com.luluroute.ms.service.validator;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

@Service
public class FactoryUtil implements ApplicationContextAware {

  private static ApplicationContext context;

  public FactoryUtil() {
  }

  public static <T> T getBean(Class<T> beanClass) {
    return context.getBean(beanClass);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    context = applicationContext;
  }
}