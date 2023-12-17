package sandipchitale.restclientstreaming;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@SpringBootApplication
public class RestclientstreamingApplication {

	public static void main(String[] args) {
		SpringApplication.run(RestclientstreamingApplication.class, args);
	}

	@RestController
	public static class StreamingProxyController {

		private static final String X_METHOD = "X-METHOD";

		private static final String X_CONNECT_TIMEOUT_MILLIS = "X-CONNECT-TIMEOUT-MILLIS";
		private static final String X_READ_TIMEOUT_MILLIS = "X-READ-TIMEOUT-MILLIS";

		private final RestClient restClient;

		public StreamingProxyController() {
			this.restClient = RestClient.create();
			this.httpMethods = Set.of(HttpMethod.GET,
					HttpMethod.HEAD,
					HttpMethod.POST,
					HttpMethod.PUT,
					HttpMethod.PATCH,
					HttpMethod.DELETE,
					HttpMethod.OPTIONS);
		}

		private final Set<HttpMethod> httpMethods;

		// Use ResponseEntity<?> to allow streaming
		@RequestMapping("/**")
		public ResponseEntity<?> proxy(HttpServletRequest httpServletRequest,
									   @RequestHeader(value = X_METHOD, required = false) String method,
									   @RequestHeader HttpHeaders httpHeaders,
									   HttpServletResponse httpServletResponse) {

			HttpMethod nonFinalHttpMethod = null;
			if (method == null) {
				method = httpServletRequest.getMethod();
			}

			final String finalMethod = method;

			nonFinalHttpMethod = HttpMethod.valueOf(method.toUpperCase());
			if (!httpMethods.contains(nonFinalHttpMethod)) {
				throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid method value: " + method + " in " + X_METHOD + " header.");
			}

			HttpMethod httpMethod = nonFinalHttpMethod;

			// Prepare the URI using the URL path following /stream/
			URI uri = ServletUriComponentsBuilder.fromRequest(httpServletRequest).build().toUri();
			String queryString = httpServletRequest.getQueryString();
			queryString = queryString == null ? "" : "?" + queryString;

			String proxyUriString = uri.getPath();
			// Remove leading slash
			if (proxyUriString.startsWith("/")) {
				proxyUriString = proxyUriString.substring(1);
			}
			// Deal with removal of double slashes
			if (proxyUriString.startsWith("https:/") && !proxyUriString.startsWith("https://")) {
				proxyUriString = "https://" + proxyUriString.substring(7);
			} else if (proxyUriString.startsWith("http:/") && !proxyUriString.startsWith("http://")) {
				proxyUriString = "http://" + proxyUriString.substring(6);
			}

			// Replace {method} with the HTTP method in lowercase
			proxyUriString = proxyUriString.replaceAll(Pattern.quote("%7Bmethod%7D"), finalMethod);
			// Replace {METHOD} with the HTTP method in uppercase
			proxyUriString = proxyUriString.replaceAll(Pattern.quote("%7BMETHOD%7D"), finalMethod);

			proxyUriString += UriUtils.decode(queryString, StandardCharsets.UTF_8);

			// Prepare the headers to send. We may want to filter some headers like Cookies or Authorization
			// in case we don't want to send them to the target service.
			HttpHeaders httpHeadersToSend = new HttpHeaders();
			httpHeadersToSend.addAll(httpHeaders);
			// Filter some request headers
			httpHeadersToSend.remove(HttpHeaders.COOKIE);

			return getRestClient(httpServletRequest)
					// Use the same HTTP method as the original request if X-METHOD header was not specified
					.method(httpMethod)
					// Proxy uri
					.uri(proxyUriString)
					// Send the prepared headers
					.headers((HttpHeaders headers) -> headers.addAll(httpHeadersToSend))
					// Stream the request body
					.body((OutputStream outputStream) -> StreamUtils.copy(httpServletRequest.getInputStream(), outputStream))
					// Use exchange to get control over the response, inferred return type is ResponseEntity<?>
					.exchange((HttpRequest clientRequest, RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse clientResponse) -> {
						// Copy headers from the upstream service response to httpServletResponse to send to client
						clientResponse.getHeaders().forEach((String name, List<String> valueList) -> {
							// Filter some response headers if needed
							valueList.forEach((String value) -> {
								httpServletResponse.addHeader(name, value);
							});
						});

						// Stream upstream service response inoutStream to httpServletResponse outputStream
						StreamUtils.copy(clientResponse.getBody(), httpServletResponse.getOutputStream());

						// Return a ResponseEntity with the status code from the upstream service response
						return ResponseEntity.status(clientResponse.getStatusCode()).build();
					});
		}

		private RestClient getRestClient(HttpServletRequest httpServletRequest) {
			String connectTimeoutMillisString = httpServletRequest.getHeader(X_CONNECT_TIMEOUT_MILLIS);
			String readTimeoutMillisString = httpServletRequest.getHeader(X_READ_TIMEOUT_MILLIS);
			if (connectTimeoutMillisString == null && readTimeoutMillisString == null) {
				// Return default one
				return restClient;
			} else {
				Duration connectionTimeout = connectTimeoutMillisString == null ? null : Duration.ofMillis(Long.parseLong(connectTimeoutMillisString));
				Duration readTimeout = readTimeoutMillisString == null ? null : Duration.ofMillis(Long.parseLong(readTimeoutMillisString));

				RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
				if (connectionTimeout != null) {
					if (connectionTimeout.equals(Duration.ZERO)) {
						// 0 indicates infinite connect	 timeout
						restTemplateBuilder = restTemplateBuilder.setConnectTimeout(Duration.ofMillis(Long.MAX_VALUE));
					} else {
						restTemplateBuilder = restTemplateBuilder.setConnectTimeout(connectionTimeout);
					}
				}
				if (readTimeout != null) {
					if (readTimeout.equals(Duration.ZERO)) {
						// 0 indicates infinite read timeout
						restTemplateBuilder = restTemplateBuilder.setReadTimeout(Duration.ofMillis(Long.MAX_VALUE));
					} else {
						restTemplateBuilder = restTemplateBuilder.setReadTimeout(readTimeout);
					}
				}

				return RestClient.create(restTemplateBuilder.build());
			}
		}

		@ExceptionHandler
		ResponseEntity<String> handleException(SocketTimeoutException socketTimeoutException) {
			return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(HttpStatus.GATEWAY_TIMEOUT.getReasonPhrase() + ": " + socketTimeoutException.getMessage());
		}
	}
}
