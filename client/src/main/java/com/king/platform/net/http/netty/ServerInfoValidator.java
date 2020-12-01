package com.king.platform.net.http.netty;

public interface ServerInfoValidator {
	ServerInfoValidator WEB_SOCKET = ServerInfo::isWebSocket;
	ServerInfoValidator HTTP = serverInfo -> !serverInfo.isWebSocket();

	boolean isValid(ServerInfo serverInfo);
}
