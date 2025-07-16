package com.github.novicezk.midjourney.wss;


public interface WebSocketStarter {

	void start() throws Exception;
	void tryReconnect() throws Exception;

}
