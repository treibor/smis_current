package com.smis.security;

import java.util.Map;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

/** Generic Spring error responses with a server-side reference, independent of debug options. */
@Component
public final class SafeErrorAttributes extends DefaultErrorAttributes {
    @Override
    public Map<String, Object> getErrorAttributes(WebRequest request, ErrorAttributeOptions options) {
        Map<String, Object> attributes = super.getErrorAttributes(request, ErrorAttributeOptions.defaults());
        Throwable failure = getError(request);
        if (failure != null) {
            String key = SafeErrorAttributes.class.getName() + ".reference";
            String reference = (String) request.getAttribute(key, WebRequest.SCOPE_REQUEST);
            if (reference == null) {
                reference = ErrorReferences.record(failure);
                request.setAttribute(key, reference, WebRequest.SCOPE_REQUEST);
            }
            attributes.put("message", "Unable to complete the request");
            attributes.put("reference", reference);
        }
        return attributes;
    }
}
