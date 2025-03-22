package es.ubu.lsi.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.ChatMessage.MessageType;

/**
 * Clase ChatServerImpl.
 * 
 * @author Rubén Alonso Quintana
 *
 */
public class ChatServerImpl implements ChatServer {

	private static final int DEFAULT_PORT = 1500;
	private static int clientId;
	private static SimpleDateFormat sdf;
	private int port;
	private boolean alive;

	private Map<Integer, Socket> clients = new HashMap<>();
	private Map<String, List<String>> blockList = new HashMap<>();
	private Map<Integer, ObjectOutputStream> clientOutputs = new HashMap<>();
	private Map<Integer, String> clientUsernames = new HashMap<>(); //Relacionar id con nombre de usuario

	/**
	 * Por defecto el servidor se ejecuta en el puerto 1500. en su invocación no recibe argumentos. 
	 * @param port
	 */
	public ChatServerImpl(int port) {
		this.port = port;
		this.alive = true;
		clientId = 0;
	}

	/**
	 * En el método startup se implementa el bucle con el servidor de sockets (Server
	 * Socket), esperando y aceptado peticiones. Ante cada petición entrante y aceptada, se 
	 * instancia un nuevo ServerThreadForClient y se arranca el hilo correspondiente 
	 * para que cada cliente tenga su hilo independiente asociado en el servidor (con su socket, 
	 * flujo de entrada y flujo de salida). Es importante ir guardando un registro de los hilos 
	 * creados para poder posteriormente realizar el push de los mensajes y un apagado correcto. 
	 */
	@Override
	public void startup() {
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			System.out.println("[Servidor] Iniciado en el puerto " + port);

			while (alive) {
				Socket socket = serverSocket.accept();
				clientId++;
				clients.put(clientId, socket);
				System.out.println("[Servidor] Nuevo cliente conectado con ID: " + clientId);
				
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                clientOutputs.put(clientId, outputStream);

				// Hilo
				new ServerThreadForClient(socket, clientId, outputStream).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * El método shutdown cierra los flujos de entrada/salida y el socket correspondiente de 
	 * cada cliente. 
	 */
	@Override
	public void shutdown() {
		System.out.println("[Servidor] Apagando...");
		alive = false;
		System.out.println("[Servidor] Apagado correctamente.");
	}

	/**
	 * El método broadcast envía el mensaje recepcionado a todos los clientes (flujo de salida). 
	 */
	@Override
	public void broadcast(ChatMessage message) {
		String origen = clientUsernames.get(message.getId()); //Obtiene el destino.
		for (Map.Entry<Integer, ObjectOutputStream> entry : clientOutputs.entrySet()) {
			//Verificación de bloqueo
			int id = entry.getKey();
			String nombre = clientUsernames.get(id);
			
			if (nombre != null && blockList.containsKey(nombre) && blockList.get(nombre).contains(origen)) {
				System.out.println("[Bloqueo] " + origen + " está bloqueado por " + nombre + ". Mensahe no enviado.");
				continue; //Salto al siguiente usuario.
			}
			
			ChatMessage mensajeOrigen = new ChatMessage(message.getId(), message.getType(), origen + ": " + message.getMessage());
			ObjectOutputStream outputStream;
			try {
				outputStream = entry.getValue();
				outputStream.writeObject(mensajeOrigen);
				outputStream.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("[Broadcast] " + message.getMessage());
	}

	/**
	 * El método remove elimina un cliente de la lista. 
	 */
	@Override
	public void remove(int id) {
		String usuario = clientUsernames.get(id);
		if (usuario != null) {
			blockList.remove(usuario); //Elimino el bloqueo
			clientUsernames.remove(id);
		}
		clients.remove(id);
		clientOutputs.remove(id);
		System.out.println("[Servidor] Cliente " + id + " desconectado.");
	}
	
	/**
	 * Contiene un método main que arranca el hilo principal de ejecución del servidor: instancia 
	 * el servidor y arranca (en el método startup). 
	 * @param args
	 */
	public static void main(String[] args) {
		ChatServerImpl server = new ChatServerImpl(DEFAULT_PORT);
		server.startup();
	}

	/**
	 * Inner class ServerThreadForClient
	 * La clase extiende de Thread.
	 */
	private class ServerThreadForClient extends Thread {
		private int id;
		private String username;
		private Socket socket;
		private ObjectOutputStream outputStream;

		public ServerThreadForClient(Socket socket, int id, ObjectOutputStream outputStream) {
			this.socket = socket;
			this.id = id;
			this.outputStream = outputStream;
		}

		/**
		 * En el método run se espera en un bucle a los mensajes recibidos de cada cliente (flujo 
         * de entrada), realizándose la operación correspondiente (a través de los métodos de la 
         * clase externa, ChatServer). A la finalización de su ejecución se debe eliminar al propio 
         * cliente de la lista de clientes activos. 
		 */
		public void run() {
			try {
				ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

				// Leemos nombre de usuario del cliente
				username = (String) inputStream.readObject();
				blockList.put(username, new ArrayList<>()); // Bloqueos
				clientUsernames.put(id, username);
				System.out.println("[Servidor] Cliente " + id + " identificado como " + username);

				// Enviamos ID al cliente
				outputStream.writeInt(id);
				outputStream.flush();

				while (alive) {
					ChatMessage msg = (ChatMessage) inputStream.readObject();
					if (msg.getType() == MessageType.LOGOUT) {
						remove(id);
						System.out.println("[Servidor] Cliente " + id + " ha cerrado sesión.");
						break;
					} else if (msg.getMessage().startsWith("ban ")) {
						String block = msg.getMessage().split(" ")[1];
						blockList.get(username).add(block);
						System.out.println(username + " ha baneado a " + block);
					} else if (msg.getMessage().startsWith("unban ")) {
						String unblock = msg.getMessage().split(" ")[1];
						blockList.get(username).remove(unblock);
						System.out.println(username + " ha desbaneado a " + unblock);
					}
					System.out.println("[" + username + "]" + msg.getMessage());
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