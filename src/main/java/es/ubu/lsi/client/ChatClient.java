package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;

public interface ChatClient {
	
	public abstract boolean start();
	public abstract void sendMessage(ChatMessage msg);
	public abstract void disconnect();

}
