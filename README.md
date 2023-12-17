# Description

A simple Spring Boot application that implements streaming using the new RestClient.

## Optional features

`X-CONNECT-TIMEOUT-MILLIS` header - set the connect timeout in milliseconds. A value of 0 means infinite connect timeout.
`X-READ-TIMEOUT-MILLIS` header - set the read timeout in milliseconds. A value of 0 means infinite read timeout.

## How to use

- Run the application

- Set the header X-METHOD to the method to proxy (GET, POST, PUT, DELETE, etc)
    - If the header is not set, method of `HttpServletRequest` is used.
- Send the request with proxy url and arguments as path and query params and post body
    - The proxy request url may contain a pattern `{method}` that will be replaced by the lowercase value of the final HttpMethod.
    - The proxy request url may contain a pattern `{METHOD}` that will be replaced by the uppercase value of the final HttpMethod.

- For example in browser use URL: 

http://localhost:8080/https://jsonplaceholder.typicode.com/posts/1

### Request

URL: 

`http://localhost:8080/https://jsonplaceholder.typicode.com/posts/1`

Headers:

```
GET /https://jsonplaceholder.typicode.com/posts/1 HTTP/1.1
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7
Accept-Encoding: gzip, deflate, br
Accept-Language: en-US,en;q=0.9
Cache-Control: no-cache
Connection: keep-alive
Cookie: R_PCS=light; R_LOCALE=en-us; R_THEME=auto; R_USERNAME=admin; _ga=GA1.1.667135922.1702520414; _ga_9HHPS5RX9D=GS1.1.1702520413.1.0.1702520580.60.0.0
Host: localhost:8080
Pragma: no-cache
Sec-Fetch-Dest: document
Sec-Fetch-Mode: navigate
Sec-Fetch-Site: none
Sec-Fetch-User: ?1
Upgrade-Insecure-Requests: 1
User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36
sec-ch-ua: "Not_A Brand";v="8", "Chromium";v="120", "Google Chrome";v="120"
sec-ch-ua-arch: "x86"
sec-ch-ua-bitness: "64"
sec-ch-ua-full-version: "120.0.6099.71"
sec-ch-ua-full-version-list: "Not_A Brand";v="8.0.0.0", "Chromium";v="120.0.6099.71", "Google Chrome";v="120.0.6099.71"
sec-ch-ua-mobile: ?0
sec-ch-ua-model: ""
sec-ch-ua-platform: "Linux"
sec-ch-ua-platform-version: "6.6.4"
sec-ch-ua-wow64: ?0
```
### Response

Response Headers:

```
HTTP/1.1 200
:status: 200
access-control-allow-credentials: true
age: 4725
alt-svc: h3=":443"; ma=86400
cache-control: max-age=43200
cf-cache-status: HIT
cf-ray: 83530ccf9dac67f4-SJC
content-encoding: br
date: Thu, 14 Dec 2023 02:34:50 GMT
etag: W/"124-yiKdLzqO5gfBrJFrcdJ8Yq0LGnU"
expires: -1
nel: {"success_fraction":0,"report_to":"cf-nel","max_age":604800}
pragma: no-cache
report-to: {"endpoints":[{"url":"https:\/\/a.nel.cloudflare.com\/report\/v3?s=JUkNmCcfo5bvJc%2BlUqfP70LUEwm79%2BkNDrMtAXX8qp54raN4lnQc4hKzya0DyXg1Hsmld7iFgYiSXS1uMhlwVmPPo%2BqgpwhHt%2BetI3uH0CaFc1g0PcF1CxfhrKnHZAI3SNEJDQp7brgsV044LoTv"}],"group":"cf-nel","max_age":604800}
server: cloudflare
vary: Origin, Accept-Encoding
via: 1.1 vegur
x-content-type-options: nosniff
x-powered-by: Express
x-ratelimit-limit: 1000
x-ratelimit-remaining: 999
x-ratelimit-reset: 1698826305
Content-Type: application/json;charset=utf-8
Transfer-Encoding: chunked
Keep-Alive: timeout=60
Connection: keep-alive
```

Response Body:

```json
{
  "userId": 1,
  "id": 1,
  "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
  "body": "quia et suscipit\nsuscipit recusandae consequuntur expedita et cum\nreprehenderit molestiae ut ut quas totam\nnostrum rerum est autem sunt rem eveniet architecto"
}
```

You can try:

`http://localhost:8080/https://postman-echo.com/{method}?onekey=onevalue&sort=fn,desc&sort=ln,asc`

Of course, you can use any other Rest Client like Postman to use other HTTP methods like POST with body, PUT with body, DELETE, etc.

### Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.2.0/gradle-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/3.2.0/gradle-plugin/reference/html/#build-image)
* [Spring Web](https://docs.spring.io/spring-boot/docs/3.2.0/reference/htmlsingle/index.html#web)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/3.2.0/reference/htmlsingle/index.html#actuator)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)

### Additional Links
These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)

