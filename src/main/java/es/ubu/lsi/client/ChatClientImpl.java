package es.ubu.lsi.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.ChatMessage.MessageType;

/**
 * Clase ChatClientImpl
 * 
 * @author Rubén Alonso Quintana
 *
 */
public class ChatClientImpl implements ChatClient {

	private String server = "localhost";
	private String username;
	private int port;
	private boolean carryOn = true;
	private int id;

	/**
	 * El cliente en su invocación recibe una dirección IP/nombre de máquina y un nickname. 
	 * El puerto de conexión es siempre el 1500. Si no se indica el equipo servidor, se toma 
	 * como valor por defecto localhost. 
	 * @param server
	 * @param port
	 * @param username
	 */
	public ChatClientImpl(String server, int port, String username) {
		this.server = server;
		this.port = port;
		this.username = username;
	}

	/**
	 * Instancia el cliente y arranca adicionalmente (en el método start) un hilo adicional a través 
	 * de ChatClientListener.
	 */
	@Override
	public boolean start() {
		try (Socket socket = new Socket(server, port);
				ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {

			// Mensaje del usuario al servidor
			outputStream.writeObject(username);
			outputStream.flush();

			// Recibimos ID del servidor
			id = inputStream.readInt();
			System.out.println("Conectado como " + username + " con ID " + id);

			// Hilo
	        new Thread (new ChatClientListener(inputStream)).start();
			// Leer los mensajes
			Scanner scanner = new Scanner(System.in);
			while (carryOn) {
				String message = scanner.nextLine();
				if (message.equalsIgnoreCase("logout")) {
					outputStream.writeObject(new ChatMessage(id, MessageType.LOGOUT, ""));
					outputStream.flush();
					carryOn = false;
					break; // Incorporación nueva
				} else {
					outputStream.writeObject(new ChatMessage(id, MessageType.MESSAGE, message));
					outputStream.flush();
				}
			}
			// Cerramos todos los recursos
			scanner.close();
			outputStream.close();
			inputStream.close();
			socket.close();

		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return false;
	}

	/**
	 * En el hilo principal se espera a la entrada de consola por parte del usuario para el envío 
	 * del mensaje (flujo de salida, a través del método sendMessage). 
	 */
	@Override
	public void sendMessage(ChatMessage msg) {
		try (Socket socket = new Socket(server, port);
				ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {
			outputStream.writeObject(msg);
			outputStream.flush();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Cuando se sale del bucle (Ej: logout) se desconecta. 
	 */
	@Override
	public void disconnect() {
		carryOn = false;
		System.out.println("Desconectando del chat...");
	}

	/**
	 * Contiene un método main que arranca el hilo principal de ejecución del cliente.
	 * @param args
	 */
	public static void main(String[] args) {
	    if (args.length < 2) {
	        System.out.println("Error: No se proporcionaron suficientes argumentos.");
	        return;
	    }
		String server = args[0];
		String username = args[1];

		ChatClientImpl client = new ChatClientImpl(server, 1500, username);
		client.start();
	}

	/**
	 * Implementa la interfaz Runnable, por lo tanto, redefine el método run para ejecutar 
	 * el hilo de escucha de mensajes del servidor (flujo de entrada) y mostrar los mensajes entrantes. 
	 */
	private class ChatClientListener implements Runnable {
		private ObjectInputStream inputStream;

		public ChatClientListener(ObjectInputStream inputStream) {
			this.inputStream = inputStream;
		}

		@Override
		public void run() {
			while (carryOn) {
				try {
					ChatMessage msg = (ChatMessage) inputStream.readObject();
					System.out.println(msg.getMessage());
				} catch (ClassNotFoundException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}
}
