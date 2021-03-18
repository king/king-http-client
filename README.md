# king-http-client
## 'com.king.king-http-client:king-http-client:3.0.21'
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.king.king-http-client/king-http-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.king.king-http-client/king-http-client)

## New in version 3.0.21
* Fixed a bug that could lead to a memory leak 
* Added new api for websocket where sending frames and sending messages are separated

## New in version 3.0.20
 * Made it possible to specify a custom dns resolver using ConfKeys.
 
## New in version 3.0.19
* Minor bugfixes

## New in version 3.0.18
* Fixed a bug related to returned content-type containing quoted charset

## New in version 3.0.17
* Fixed bug related to hostnames that contained "-" and "_".

## New in Version 3.0.16
* Adding support for sending manual pong frames in websocket connections

## New in Version 3.0.15
* Made HttpClient.create(HttpMethod httpMethod, String uri) return HttpClientRequestWithBodyBuilder

## New in Version 3.0.14
* Fixed bug in ChannelManager which caused compression failures
* Improved WebSocket client, added support for automatically splitting too large frames
* Improved WebSocket client, added more configuration options to both builder as well as default values. 

## New in Version 3.0.13
* Extracted interfaces for Header values

## New in Version 3.0.12
* Added support for disabling / enabling automatic decompression of gziped response bodies. This can either be configured globaly through `ConfKeys.AUTOMATICALLY_DECOMPRESS_RESPONSE` or per request through `.automaticallyDecompressResponse(boolean)`.

## New in Version 3.0.11
* Added checks for bad servers sending invalid http responses
* Fixed issue where HEAD requests completed twice

## New in Version 3.0.10
* Made it possible to close an `WebSocketConnection`

## New in Version 3.0.9
* Bug fix for an race condition in WebSocket connection. Now onConnected callback is called before the CompletableFuture is completed. Both the onConnected and CompletableFuture is called on the netto io threads, so avoid blocking!

## New in Version 3.0.8
* Added support for fetching http version as well as status reason from `HttpResponse`
* Added support for supplying what http verb should be used in HttpClient

## New in Version 3.0.7
* Bumped netty version to 4.1.29.Final

## New in Version 3.0.6
* Improved shutdown by waiting for all channels to close
* Fixed potential NPE
* Removed duplicate AttributeKey
* Added `HttpClient.isStarted()` method

## New in Version 3.0.5
* Fixed port parsing issue for WSS

## New in Version 3.0.4
* Correctly release retained frames in WS

## New in Version 3.0.2
* Added support for handling http redirects for websocket connections.

## New in Version 3.0.1
* Fixed a critical memmory leak in 3.0.0

## New in Version 3.0.0
* Redesign of builder APIs.
  * The `ResponseBodyConsumer` have been moved from the  `execute` method to the `build` method.
  * The `HttpCallback` have been moved from the  `execute` method to the `withHttpCallback` method.
  * The `NioCallback` have been moved from the  `execute` method to the `withNioCallback` method.
  * A few methods have been renamed to align them between the classes.
* Added support for websocket.
* Added support for building multipart uploads.
* Added support for upload progression callbacks through the `withUploadCallback` method.
* Fixed a bug in idle timeout handling.

## New in Version 2.3.1
* Client is now waiting for ssl/tls handshake to complete before trying to push data

## New in Version 2.3.0 
* Changed to Java 8
* `Future<FutureResult<T>>` has been replaced with `CompletableFuture<HttpResponse<T>>` in `BuiltClientRequest`.
* The `CompletableFuture.get()` can now throw `ExecutionException` which contains the underlaying Exception (Before this exception could be fetched from the `FutureResult`).
* `HttpClient.setConf` has been deprecated and replaced with `NettyHttpClientBuilder.setOption`
* `UriQueryBuilder.addParameter` can now be chained.
* Netty 4.1 has been bumped to 4.1.15.Final.

## New in Version 2.2.0
* Improved SseClient

## New in Version 2.1.0
 * Upgraded to use Netty-4.1
 
## New in Version 2.0.0
 * Supports Server Side Events.
 * Support for enabling epoll (set ConfKeys.EPOLL to true)


## Api

First off the HttpClient needs to be created and started.
To make it easier, use the NettyHttpClientBuilder.
Use the different set methods on the NettyHttpClientBuilder to tweak how HttpClient works.

```java
NettyHttpClientBuilder nettyHttpClientBuilder = new NettyHttpClientBuilder();
HttpClient httpClient = nettyHttpClientBuilder.createHttpClient();
httpClient.start();
```

Before start is called, httpClient.setConf can be used to tweak internal settings of the client.

Then to use it, use the fluent method builder on the HttpClient:

```java
CompletableFuture<HttpResponse<String>> future = httpClient.createGet("http://some.url").build().execute();

CompletableFuture<HttpResponse<byte[]>> byteResponseFuture = httpClient.createPost("http://someUrl")
			.content("someContentToBePosted".getBytes())
			.addQueryParameter("param1", "value1")
			.addHeader("header1", "headerValue1")
			.build(ByteArrayResponseBodyConsumer::new)
			.execute();
```

Or using callback objects:

```java
httpClient.createGet("http://some.url").build().withHttpCallback(new HttpCallback<String>() {
			@Override
			public void onCompleted(HttpResponse<String> httpResponse) {

			}

			@Override
			public void onError(Throwable throwable) {

			}
		}).execute();
```


For more complex cases or when the result is unfit to handle as a string, a ResponseBodyConsumer can be defined on the first build method:

```java
httpClient.createGet("http://some.url").build(() -> new ResponseBodyConsumer<SomeObject>() {
	@Override
	public void onBodyStart(String contentType, String charset, long contentLength) throws Exception {

	}

	@Override
	public void onReceivedContentPart(ByteBuffer buffer) throws Exception {
		//aggregate the content from the server
	}

	@Override
	public void onCompletedBody() throws Exception {
		//build the SomeObject from the aggregated data
	}

	@Override
	public SomeObject getBody() {
		return null;
	}
}).withHttpCallback(new HttpCallback<SomeObject>() {
	@Override
	public void onCompleted(HttpResponse<SomeObject> httpResponse) {

	}

	@Override
	public void onError(Throwable throwable) {

	}
}).execute();
```
This can also be used to stream the returned body to a file instead of buffer all bytes in memory.


## Server Side Events Api
A server side event connection can be made by callign the createSSE method on the httpClient.
```java
SseClient sseClient = httpClient.createSSE("http://someUrl").build().execute(new SseClientCallback() {
		@Override
		public void onConnect() {

		}

		@Override
		public void onDisconnect() {

		}

		@Override
		public void onError(Throwable throwable) {

		}

		@Override
		public void onEvent(String lastSentId, String event, String data) {

		}
	});
		
sseClient.awaitClose();

```
The SseClient also supports reconnecting after a connection has been lost
```java
sseClient.connect();
```
It is also possible to subscribe specific events
```java
sseClient.onEvent("stocks", new EventCallback() {
	@Override
	public void onEvent(String lastSentId, String event, String data) {

	}
});

sseClient.onConnect(new SseClient.ConnectCallback() {
	@Override
	public void onConnect() {

	}
});


sseClient.onDisconnect(new SseClient.DisconnectCallback() {
	@Override
	public void onDisconnect() {

	}
});
```

## Web-sockets
A websocket connection can be made by calling the createWebSocket on the httpClient.
```java
CompletableFuture<WebSocketClient> future = httpClient.createWebSocket("ws://some.server").build().execute(new WebSocketListener() {
	@Override
	public void onConnect(WebSocketConnection connection) {

	}

	@Override
	public void onError(Throwable throwable) {

	}

	@Override
	public void onDisconnect() {

	}

	@Override
	public void onCloseFrame(int code, String reason) {

	}

	@Override
	public void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {

	}

	@Override
	public void onTextFrame(String payload, boolean finalFragment, int rsv) {

	}
});
```
If no extra executor has been supplied into the websocket builder all callbacks will be executed on the NIO thread!


## Thread model

The methods in HttpCallback are only invoked from the threads in HttpCallbackExecutor.
This means that you can have long running jobs without blocking IO operations for other requests. The executor will of course
need to be resized accordingly to prevent excessive queues.  

The methods in NioCallback are always invoked from the internal nio threads. They should therefore not be blocking nor long running. 
The same goes for implementations of the ResponseBodyConsumer and MetricCallback interfaces.


## Limitations
Does not support complex authentication (basic should work using the headers). Does not support proxys. Does not support http2.
