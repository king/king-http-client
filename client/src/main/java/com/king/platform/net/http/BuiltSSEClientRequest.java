package com.king.platform.net.http;


import java.util.concurrent.Future;

public interface BuiltSSEClientRequest {

	Future<FutureResult<Void>> execute(HttpSSECallback httpSSECallback);

}
