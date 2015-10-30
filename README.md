# king-http-client


## Api

First off the HttpClient needs to be created and started.
To make it easier, use the NettyHttpClientBuilder.
Use the different set methods on the NettyHttpClientBuilder to tweak how HttpClient works.

```java
NettyHttpClientBuilder nettyHttpClientBuilder = new NettyHttpClientBuilder();
NettyHttpClint httpClient = nettyHttpClientBuilder.createHttpClient();
httpClient.start();
```

Before start is called, httpClient.setConf can be used to tweak internal settings of the client.

Then to use it, use the fluent method builder on the HttpClient:

```java
Future<FutureResult<String>> resultFuture = httpClient.createGet("http://some.url").build().execute();
httpClient.createPost("http://someUrl").content("someContentToBePosted".getBytes()).withQueryParameter("param1", "value1").withHeader("header1", "headerValue1").build().execute();
```

Or using callback objects:

```java
httpClient.createGet("http://some.url").build().execute(new StringHttpCallback() {
            @Override
            public void onCompleted(HttpResponse<String> httpResponse) {

            }

            @Override
            public void onError(Throwable throwable) {

            }
        });

```


For more complex cases or when the result is unfit to handle as a string, a ResponseBodyConsumer can be defined in the HttpCallback object:

```java
httpClient.createGet("http://some.url").build().execute(new HttpCallback<Void>() {
            @Override
            public void onCompleted(HttpResponse<Void> httpResponse) {

            }

            @Override
            public ResponseBodyConsumer<Void> newResponseBodyConsumer() {
                return new ResponseBodyConsumer<Void>() {
                    @Override
                    public void onBodyStart(String contentType, String charset, long contentLength) throws Exception {

                    }

                    @Override
                    public void onReceivedContentPart(ByteBuffer buffer) throws Exception {

                    }

                    @Override
                    public void onCompletedBody() throws Exception {

                    }

                    @Override
                    public Void getBody() {
                        return null;
                    }
                };
            }

            @Override
            public void onError(Throwable throwable) {

            }
        });


```


## Thread model

The methods on HttpCallback are called by HttpCallbackExecutor, there can therefore be long running jobs in the callbacks.
The methods on NioCallback are called by the internal nio threads, they should therefore not be blocking or long running. The same goes for the ResponseBodyConsumer and MetricCallback interfaces.


## Limitations
Does not support complex authentication (basic should work using the headers). Does not support proxys. Does not support http2 (yet).
