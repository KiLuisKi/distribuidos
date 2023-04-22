package prueba;

import java.net.URI;
import java.util.Scanner;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

@Singleton
@Path("servicio")
public class Servicio {
	private int servidor;
	private int numProcesos = 2;
	private int numServidores = 1;
	private Proceso procesos[] = new Proceso[2];

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("start")
	public String start() {
		String ip[] = new String[numServidores];
		Scanner scanner = new Scanner(System.in);

		for (int i = 0; i < numServidores; i++) {
			System.out.print("Introduce la IP " + (i + 1) + ": ");
			ip[i] = scanner.nextLine();
		}
		scanner.close();

		for (int i = 0; i < numServidores; i++) {
			int idIp = i;
			String ips = ip[0];// + "," + ip[1] + "," + ip[2];

			Client cliente = ClientBuilder.newClient();
			URI uri = UriBuilder.fromUri("http://" + ip[servidor] + ":8080/prueba").build();
			WebTarget target = cliente.target(uri);
			target.path("rest").path("servicio").path("procesos").queryParam("idIp", idIp).queryParam("ips", ips)
					.request(MediaType.TEXT_PLAIN).get(String.class);
		}

		return "Comenzamos";
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("procesos")
	public String procesos(@QueryParam(value = "idIp") int idIp, @QueryParam(value = "ips") String ips) {
		servidor=idIp;
		String ipsSplit[]= ips.split(",");
		
		if (servidor == 0) {
			procesos[0] = new Proceso(0, numProcesos, ipsSplit[0], "1", "2" /*ipsSplit[1], ipsSplit[2]*/);
			procesos[1] = new Proceso(1, numProcesos, ipsSplit[0], "1", "2" /*ipsSplit[1], ipsSplit[2]*/);
			procesos[0].start();
			procesos[1].start();
		} else if (servidor == 1) {
			procesos[0] = new Proceso(2, numProcesos, ipsSplit[0], ipsSplit[1], ipsSplit[2]);
			procesos[1] = new Proceso(3, numProcesos, ipsSplit[0], ipsSplit[1], ipsSplit[2]);
			procesos[0].start();
			procesos[1].start();
		} else {
			procesos[0] = new Proceso(4, numProcesos, ipsSplit[0], ipsSplit[1], ipsSplit[2]);
			procesos[1] = new Proceso(5, numProcesos, ipsSplit[0], ipsSplit[1], ipsSplit[2]);
			procesos[0].start();
			procesos[1].start();
		}
		
		return "Procesos creados";
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("multicast")
	public String multicast(@QueryParam(value = "Pj") int Pj, @QueryParam(value = "Tj") int Tj,
			@QueryParam(value = "destino") int idProcesoReceptor) {
		if (servidor == 0) {
			if (idProcesoReceptor == 0)
				procesos[0].recibirMensaje(Pj, Tj);
			else if (idProcesoReceptor == 1)
				procesos[1].recibirMensaje(Pj, Tj);
		} else if (servidor == 1) {
			if (idProcesoReceptor == 2)
				procesos[0].recibirMensaje(Pj, Tj);
			else if (idProcesoReceptor == 3)
				procesos[1].recibirMensaje(Pj, Tj);
		} else {
			if (idProcesoReceptor == 4)
				procesos[0].recibirMensaje(Pj, Tj);
			else if (idProcesoReceptor == 5)
				procesos[1].recibirMensaje(Pj, Tj);
		}

		return "Realizada la multidifusion";
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("respuesta")
	public String respuesta(@QueryParam(value = "Pj") int Pj, @QueryParam(value = "destino") int idProcesoReceptor) {
		if (servidor == 0) {
			if (idProcesoReceptor == 0)
				procesos[0].recibirRespuesta();
			else if (idProcesoReceptor == 1)
				procesos[1].recibirRespuesta();
		} else if (servidor == 1) {
			if (idProcesoReceptor == 2)
				procesos[0].recibirRespuesta();
			else if (idProcesoReceptor == 3)aaa
				procesos[1].recibirRespuesta();
		} else {
			if (idProcesoReceptor == 4)
				procesos[0].recibirRespuesta();
			else if (idProcesoReceptor == 5)
				procesos[1].recibirRespuesta();
		}

		return "Respuesta enviada";
	}
}
