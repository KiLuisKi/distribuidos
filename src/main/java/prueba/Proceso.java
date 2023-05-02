package prueba;

import java.net.URI;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

public class Proceso extends Thread {
	private int id; // Identificador del proceso
	private int Ci; // Tiempo lógico del proceso(Lamport)
	private int Ti; // Tiempo de solicitud de entrada en sección crítica(Lamport)
	private Estado estado; // Estado del proceso (LIBERADA, BUSCADA, TOMADA)

	private Queue<Mensaje> cola; // Cola de peticiones de entrada en sección crítica
	private int numRespuestas; // Número de respuestas recibidas por el proceso
	private int nProcesos; // Número total de procesos en el sistema
	private String ip[] = new String[3];

	private Logger logger;
	private int servidor;
	private Semaphore semSC;
	private Semaphore semStart;

	public Proceso(int id, int numProcesos, String[] ips, int serverId) {
		this.id = id;
		this.Ci = 0;
		this.Ti = 0;
		this.estado = Estado.LIBERADA;
		this.cola = new LinkedList<>();
		this.numRespuestas = 0;
		this.nProcesos = numProcesos;
		this.semSC = new Semaphore(0);
		this.semStart = new Semaphore(0);
		this.ip[0] = ips[0];
		this.ip[1] = ips[1];
		// this.ip[2] = ips[2];
		this.logger = new Logger("C:\\Users\\Luis\\Desktop\\proceso_" + id + ".log");
	}

	private void entradaSC() {
		estado = Estado.BUSCADA;
		Ti = Ci;

		for (int i = 0; i < nProcesos; i++) {
			if (i != id) {
				servidor = i / 2;

				Client cliente = ClientBuilder.newClient();
				URI uri = UriBuilder.fromUri("http://" + ip[servidor] + ":8080/prueba").build();
				WebTarget target = cliente.target(uri);
				target.path("rest").path("servicio").path("multicast").queryParam("Pj", id).queryParam("Tj", Ti)
						.queryParam("destino", i).request(MediaType.TEXT_PLAIN).get(String.class);
			}
		}

		for (numRespuestas = 0; numRespuestas < (nProcesos - 1); numRespuestas++) {
			try {
				semSC.acquire();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		estado = Estado.TOMADA;
		System.out.println("Soy " + id + " y acabo de tomar la SC");
		String texto = "P" + (id + 1) + " E " + System.currentTimeMillis() + "\n";
		logger.write(texto);

		Ci = Ci + 1;
	}

	public void recibirMensaje(int Pj, int Tj) {
		Ci = Math.max(Ci, Tj) + 1;
		if (estado == Estado.TOMADA || (estado == Estado.BUSCADA && (Ti < Tj || (Ti == Tj && id < Pj)))) {
			Mensaje peticion = new Mensaje(Pj, Tj, TipoMensaje.SOLICITUD_ACCESO);
			cola.offer(peticion);
		} else {
			servidor = Pj / 2;

			Client cliente = ClientBuilder.newClient();
			URI uri = UriBuilder.fromUri("http://" + ip[servidor] + ":8080/prueba").build();
			WebTarget target = cliente.target(uri);
			target.path("rest").path("servicio").path("respuesta").queryParam("destino", Pj)
					.request(MediaType.TEXT_PLAIN).get(String.class);
		}
	}

	public void recibirRespuesta() {
		semSC.release();
	}

	public void startProc() {
		semStart.release();
	}

	private void salidaSC() {
		estado = Estado.LIBERADA;
		System.out.println("Soy " + id + " y acabo de salir");
		String texto = "P" + (id + 1) + " S " + System.currentTimeMillis() + "\n";
		logger.write(texto);

		while (!cola.isEmpty()) {
			Mensaje mensaje = cola.poll();
			servidor = mensaje.getIdProceso() / 2;

			Client cliente = ClientBuilder.newClient();
			URI uri = UriBuilder.fromUri("http://" + ip[servidor] + ":8080/prueba").build();
			WebTarget target = cliente.target(uri);

			target.path("rest").path("servicio").path("respuesta").queryParam("destino", mensaje.getIdProceso())
					.request(MediaType.TEXT_PLAIN).get(String.class);
		}
	}

	public void run() {
		for (int i = 0; i < nProcesos; i++) {
			if (i != id) {
				servidor = i / 2;

				Client cliente = ClientBuilder.newClient();
				URI uri = UriBuilder.fromUri("http://" + ip[servidor] + ":8080/prueba").build();
				WebTarget target = cliente.target(uri);

				target.path("rest").path("servicio").path("espera").request(MediaType.TEXT_PLAIN).get(String.class);
			}
		}

		try {
			semStart.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (int i = 0; i < 100; i++) {
			try {
				Thread.sleep((long) (Math.random() * 200.0 + 300.0));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			entradaSC();

			try {
				Thread.sleep((long) (Math.random() * 200.0 + 100.0));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			salidaSC();
		}
	}

}
