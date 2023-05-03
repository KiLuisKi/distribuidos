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
import javax.ws.rs.core.Response;
import javax.ws.rs.client.InvocationCallback;

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
		//this.ip[1] = ips[1];
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

	private double determinarDelay(long t0, long t1, long t2, long t3) {
	    double d1 = (t1 - t0) / 2.0;
	    double d2 = (t3 - t2) / 2.0;
	    double d = d2 - d1;
	    return d;
	}

	private double determinarOffset(long t0, long t1, long t2, long t3) {
	    double o1 = (t1 - t0) / 2.0;
	    double o2 = (t3 - t2) / 2.0;
	    double o = (o1 + o2) / 2.0;
	    return o;
	}

	public void run() {
		//NTP
		if (id == 0) {
			for (int i = 0; i < (nProcesos / 2); i++) {
				double mejorDelay = Double.MAX_VALUE;
			    double mejorOffset = Double.MAX_VALUE;
			    for (int j = 0; j < 10; j++) {
			    	Client cliente = ClientBuilder.newClient();
					URI uri = UriBuilder.fromUri("http://" + ip[i] + ":8080/prueba").build();
					WebTarget target = cliente.target(uri);
					
			        long t0 = System.currentTimeMillis();
			        String tiempo = target.path("rest").path("servicio").path("pedirTiempo").request(MediaType.TEXT_PLAIN).get(String.class);
			        String[] tiempos = tiempo.split(",");
			        long t1 = Long.parseLong(tiempos[0]);
			        long t2 = Long.parseLong(tiempos[1]);
			        long t3 = System.currentTimeMillis();

			        double delay = determinarDelay(t0, t1, t2, t3);
			        double offset = determinarOffset(t0, t1, t2, t3);

			        if (delay < mejorDelay) {
			            mejorDelay = delay;
			            mejorOffset = offset;
			        }
			    }
				System.out.println("El delay maximo es: " + mejorDelay);
				System.out.println("El offset maximo es: " + mejorOffset);
			}
		}
		

		for (int i = 0; i < nProcesos; i++) {
			if (i != id) {
				servidor = i / 2;

				Client cliente = ClientBuilder.newClient();
				URI uri = UriBuilder.fromUri("http://" + ip[servidor] + ":8080/prueba").build();
				WebTarget target = cliente.target(uri);

				target.path("rest").path("servicio").path("espera").request(MediaType.TEXT_PLAIN).async()
						.get(new InvocationCallback<Response>() {
							@Override
							public void completed(Response response) {
							}

							@Override
							public void failed(Throwable throwable) {
								System.out.println("Invocation failed.");
								throwable.printStackTrace();
							}
						});
			}
		}

		if (id == 0) {
			for (int i = 0; i < (nProcesos / 2); i++) {
				Client cliente = ClientBuilder.newClient();
				URI uri = UriBuilder.fromUri("http://" + ip[i] + ":8080/prueba").build();
				WebTarget target = cliente.target(uri);

				target.path("rest").path("servicio").path("liberar").request(MediaType.TEXT_PLAIN).async()
						.get(new InvocationCallback<Response>() {
							@Override
							public void completed(Response response) {
							}

							@Override
							public void failed(Throwable throwable) {
								System.out.println("Invocation failed.");
								throwable.printStackTrace();
							}
						});
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
