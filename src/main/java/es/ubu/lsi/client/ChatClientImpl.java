package es.ubu.lsi.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.ChatMessage.MessageType;

public class ChatClientImpl implements ChatClient {

	private String server;
	private String username;
	private int port;
	private boolean carryOn = true;
	private int id;

	public ChatClientImpl(String server, int port, String username) {
		this.server = server;
		this.port = port;
		this.username = username;
	}

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
	        new Thread(() -> {
	            while (carryOn) {
	                try {
	                    ChatMessage msg = (ChatMessage) inputStream.readObject();
	                    System.out.println(msg.getMessage());
	                } catch (IOException | ClassNotFoundException e) {
	                    System.out.println("[Error] Conexión cerrada.");
	                    break;
	                }
	            }
	        }).start();
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

	@Override
	public void disconnect() {
		carryOn = false;
		System.out.println("Desconectando del chat...");
	}

	public static void main(String[] args) {
		// Robustez
		String server = args[0];
		String username = args[1];

		ChatClientImpl client = new ChatClientImpl(server, 1500, username);
		client.start();
	}

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
					System.out.println("Mensaje recibido: " + msg.getMessage());
				} catch (ClassNotFoundException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}
}
