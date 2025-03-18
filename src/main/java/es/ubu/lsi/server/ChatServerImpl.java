package es.ubu.lsi.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.ChatMessage.MessageType;

public class ChatServerImpl implements ChatServer {

	private static final int DEFAULT_PORT = 1500;
	private static int clientId;
	private static SimpleDateFormat sdf;
	private int port;
	private boolean alive;
	
	public ChatServerImpl(int port) {
		this.port = port;
		this.alive = true;
	}

	@Override
	public void startup() {
		try {
			ServerSocket serverSocket = new ServerSocket(port);
			System.out.println("[Servidor] Iniciado en el puerto " + port);
			
			while (alive) {
				Socket socket = serverSocket.accept();
				clientId++;
				System.out.println("[Servidor] Nuevo cliente conectado con ID: " + clientId);
				
				//Hilo
				new Thread(new ServerThreadForClient()).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void shutdown() {
		System.out.println("[Servidor] Apagando...");
		alive = false;
		System.out.println("[Servidor] Apagado correctamente.");
	}

	@Override
	public void broadcast(ChatMessage message) {
		System.out.println("[Broadcast] " + message.getMessage());
	}

	@Override
	public void remove(int id) {
		System.out.println("[Servidor] Cliente " + id + " desconectado.");
	}
	
	public static void main(String[] args) {
		ChatServerImpl server = new ChatServerImpl(DEFAULT_PORT);
		server.startup();
	}
	
	private class ServerThreadForClient implements Runnable{
		private int id;
		private String username;
		
		public void run() {
			try {
				Socket socket = new ServerSocket(DEFAULT_PORT).accept();
				int id = clientId++;
				
				try(ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
					ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {
					
					String username = (String) inputStream.readObject();
					System.out.println("[Servidor] Client " + id + " identificado como " + username);
					
					//Enviar el ID al client
					outputStream.writeInt(id);
					outputStream.flush();
					
					//Escuchar mensajes
					while (alive) {
						ChatMessage msg = (ChatMessage) inputStream.readObject();
						if (msg.getType() == MessageType.LOGOUT) {
							System.out.println("[Servidor] Cliente " + id + " ha cerrado sesión.");
							break;
						}
						System.out.println("[" + username + "]" + msg.getMessage());
					}
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}

