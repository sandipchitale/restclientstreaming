package sandipchitale.restclientstreaming;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@SpringBootApplication
public class RestclientstreamingApplication {

	public static void main(String[] args) {
		SpringApplication.run(RestclientstreamingApplication.class, args);
	}

	@RestController
	public static class StreamingProxyController {

		private final RestClient restClient;

		public StreamingProxyController() {
			this.restClient = RestClient.create();
		}

		// Use ResponseEntity<?> to allow streaming
		@RequestMapping("/stream/**")
		public ResponseEntity<?> proxy(HttpServletRequest httpServletRequest,
									   @RequestHeader HttpHeaders httpHeaders,
									   HttpServletResponse httpServletResponse) {
			// Prepare the URI using the URL path following /stream/
			URI uri = ServletUriComponentsBuilder.fromRequest(httpServletRequest).build().toUri();
			String queryString = httpServletRequest.getQueryString();
			queryString = queryString == null ? "" : "?" + queryString;

			String proxyUriString = uri.getPath().substring(8);
			// Deal with removal of double slashes
			if (proxyUriString.startsWith("https:/") && !proxyUriString.startsWith("https://")) {
				proxyUriString = "https://" + proxyUriString.substring(7);
			} else if (proxyUriString.startsWith("http:/") && !proxyUriString.startsWith("http://")) {
				proxyUriString = "http://" + proxyUriString.substring(6);
			}
			proxyUriString += UriUtils.decode(queryString, StandardCharsets.UTF_8);

			// Prepare the headers to send. We may want to filter some headers like Cookies or Authorization
			// in case we don't want to send them to the target service.
			HttpHeaders httpHeadersToSend = new HttpHeaders();
			httpHeadersToSend.addAll(httpHeaders);
			// Filter some request headers
			httpHeadersToSend.remove(HttpHeaders.COOKIE);

			return restClient
					// Use the same HTTP method as the original request
					.method(HttpMethod.valueOf(httpServletRequest.getMethod().toUpperCase()))
					// Proxy
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
