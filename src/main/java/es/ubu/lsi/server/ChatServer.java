package es.ubu.lsi.server;

import es.ubu.lsi.common.ChatMessage;

/**
 * Interfaz que define la signatura de los métodos de arranque, multidifusión, eliminación de cliente y apagado.
 */
public interface ChatServer {
	
	public abstract void startup();
	public abstract void shutdown();
	public abstract void broadcast(ChatMessage message);
	public abstract void remove (int id);
}
