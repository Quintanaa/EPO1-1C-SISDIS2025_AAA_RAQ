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

				// Hilo
				new Thread(new ServerThreadForClient(socket, clientId)).start();
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

	    for (Thread t : Thread.getAllStackTraces().keySet()) {
	        if (t instanceof ServerThreadForClient) {
	            ServerThreadForClient clientThread = (ServerThreadForClient) t;
	            try {
	                // Usar el mismo ObjectOutputStream de cada cliente
	                clientThread.outputStream.writeObject(message);
	                clientThread.outputStream.flush();
	            } catch (IOException e) {
	                System.out.println("[Error] No se pudo enviar mensaje a un cliente.");
	            }
	        }
	    }
	}


	@Override
	public void remove(int id) {
		System.out.println("[Servidor] Cliente " + id + " desconectado.");
	}

	public static void main(String[] args) {
		ChatServerImpl server = new ChatServerImpl(DEFAULT_PORT);
		server.startup();
	}

	private class ServerThreadForClient extends Thread {
		private int id;
		private String username;
		private Socket socket;

		public ServerThreadForClient(Socket socket, int id) {
			this.socket = socket;
			this.id = id;
		}
		public void run() {
		    try {
		        ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
		        outputStream = new ObjectOutputStream(socket.getOutputStream()); // Guardar el stream en el hilo del cliente

		        username = (String) inputStream.readObject();
		        System.out.println("[Servidor] Cliente " + id + " identificado como " + username);

		        outputStream.writeInt(id);
		        outputStream.flush();

		        while (true) {
		            ChatMessage msg = (ChatMessage) inputStream.readObject();
		            if (msg.getType() == MessageType.LOGOUT) {
		                System.out.println("[Servidor] Cliente " + id + " ha cerrado sesión.");
		                break;
		            }

		            System.out.println("[" + username + "] " + msg.getMessage());
		            broadcast(msg);
		        }
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
