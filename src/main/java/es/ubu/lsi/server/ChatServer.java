package es.ubu.lsi.server;

import es.ubu.lsi.common.ChatMessage;

/**
 * Define la signatura de los métodos de arranque, multidifusión, eliminación de cliente y apagado. 
 * 
 * @author Rubén Alonso Quintana
 *
 */
public interface ChatServer {
	
	public abstract void startup();
	public abstract void shutdown();
	public abstract void broadcast(ChatMessage message);
	public abstract void remove (int id);
}
