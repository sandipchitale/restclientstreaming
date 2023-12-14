package sandipchitale.restclientstreaming;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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

		private final RestClient restClient;
		private final Set<HttpMethod> httpMethods;

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

			return restClient
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
	}

}
