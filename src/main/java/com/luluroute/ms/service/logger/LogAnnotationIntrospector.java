package com.luluroute.ms.service.logger;

import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.logistics.luluroute.domain.logger.Mask;

public class LogAnnotationIntrospector extends NopAnnotationIntrospector {

    private static final long serialVersionUID = 6609936151559688015L;

    @Override
    public Object findSerializer(Annotated annotated) {
        Mask annotation = annotated.getAnnotation(Mask.class);
        return annotation != null ? new MaskingSerializer() : null;
    }
}
