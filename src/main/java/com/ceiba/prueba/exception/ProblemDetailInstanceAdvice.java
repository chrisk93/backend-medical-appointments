package com.ceiba.prueba.exception;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Removes {@code instance} from error responses so they never echo the requested path back.
 * <p>
 * Spring fills {@code instance} with the request path right before writing a {@link ProblemDetail}, so it has to
 * be cleared at this last step; a null value is then omitted from the JSON.
 */
@RestControllerAdvice
public class ProblemDetailInstanceAdvice implements ResponseBodyAdvice<Object> {

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return true;
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
			ServerHttpResponse response) {
		if (body instanceof ProblemDetail problemDetail) {
			problemDetail.setInstance(null);
		}
		return body;
	}

}
