package es.ubu.lsi.client;

/**
 * Interfaz del Cliente.
 * Define la signatura de los métodos de envío de mensaje, desconexión y arranque.
 * 
 * @author Rubén Alonso Quintana
 *
 */
import es.ubu.lsi.common.ChatMessage;

public interface ChatClient {
	
	public abstract boolean start();
	public abstract void sendMessage(ChatMessage msg);
	public abstract void disconnect();

}
