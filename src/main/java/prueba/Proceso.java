package prueba;

import java.net.URI;
import java.util.PriorityQueue;
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

	private PriorityQueue<Mensaje> cola; // Cola de peticiones de entrada en sección crítica
	private int numRespuestas; // Número de respuestas recibidas por el proceso
	private int nProcesos; // Número total de procesos en el sistema
	private String ip[] = new String[3];

	private Comparador comparador;
	private Logger logger;

	private Semaphore semSC;

	public Proceso(int id, int numProcesos, String ip1, String ip2, String ip3) {
		this.id = id;
		this.Ci = 0;
		this.Ti = 0;
		this.estado = Estado.LIBERADA;
		this.cola = new PriorityQueue<>();
		this.numRespuestas = 0;
		this.nProcesos = numProcesos;
		this.semSC = new Semaphore(0);
		this.ip[0] = ip1;
		this.ip[1] = ip2;
		this.ip[2] = ip3;
		this.logger = new Logger("C:\\Users\\Luis\\Desktop\\proceso_" + id + ".log");
		this.comparador = new Comparador();
	}

	private void entradaSC() {
		estado = Estado.BUSCADA;
		Ti = Ci;
		peticionMulticast();
		for (numRespuestas = 0; numRespuestas < (nProcesos - 1); numRespuestas++) {
			try {
				semSC.acquire();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		estado = Estado.TOMADA;
		
		String texto = "P" + (id + 1) + " E " + System.currentTimeMillis() + "\n";
		logger.write(texto);

		Ci = Ci + 1;
	}

	private void peticionMulticast() {
		int servidor;
		for (int i = 0; i < nProcesos; i++) {
			if (i != id) {
				if (i == 0 || i == 1)
					servidor = 0;
				else if (i == 3 || i == 4)
					servidor = 1;
				else
					servidor = 2;

				Client cliente = ClientBuilder.newClient();
				URI uri = UriBuilder.fromUri("http://" + ip[servidor] + ":8080/prueba").build();
				WebTarget target = cliente.target(uri);
				target.path("rest").path("servicio").path("multicast").queryParam("Pj", id).queryParam("Tj", Ti)
						.queryParam("destino", i).request(MediaType.TEXT_PLAIN).get(String.class);
			}
		}
	}

	public void recibirMensaje(int Pj, int Tj) {
		Ci = Math.max(Ci, Tj) + 1;
		if (estado == Estado.TOMADA || (estado == Estado.BUSCADA && (comparador.compara(Ti, Tj, id, Pj)))) {
			Mensaje peticion = new Mensaje(Pj, Tj, TipoMensaje.SOLICITUD_ACCESO);
			cola.offer(peticion);
		} else {
			int servidor;
			if (Pj == 0 || Pj == 1)
				servidor = 0;
			else if (Pj == 3 || Pj == 4)
				servidor = 1;
			else
				servidor = 2;

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

	private void salidaSC() {
		estado = Estado.LIBERADA;
		
		String texto = "P" + (id + 1) + " S " + System.currentTimeMillis() + "\n";
		logger.write(texto);
		
		responderPeticiones();
	}

	private void responderPeticiones() {
		while (!cola.isEmpty()) {
			Mensaje mensaje = cola.poll();

			int servidor;
			if (mensaje.getIdProceso() == 0 || mensaje.getIdProceso() == 1)
				servidor = 0;
			else if (mensaje.getIdProceso() == 3 || mensaje.getIdProceso() == 4)
				servidor = 1;
			else
				servidor = 2;

			Client cliente = ClientBuilder.newClient();
			URI uri = UriBuilder.fromUri("http://" + ip[servidor] + ":8080/prueba").build();
			WebTarget target = cliente.target(uri);
			target.path("rest").path("servicio").path("respuesta").queryParam("destino", mensaje.getIdProceso())
					.request(MediaType.TEXT_PLAIN).get(String.class);
		}

	}

	public void run() {
		float tiempo1;
		float tiempo2;

		for (int i = 0; i < 100; i++) {
			tiempo1 = (float) (Math.random() * 500.0 + 300.0);
			try {
				Thread.sleep((long) tiempo1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			entradaSC();
			tiempo2 = (float) (Math.random() * 300.0 + 100.0);
			try {
				Thread.sleep((long) tiempo2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			salidaSC();
		}
	}

}
