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
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

@Singleton
@Path("servicio")
public class Servicio {
	private int servidor;
	private int numProcesos = 4;
	private int numServidores = 2;
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
			String ips = ip[0] + "," + ip[1];// + "," + ip[2];

			Client cliente = ClientBuilder.newClient();
			URI uri = UriBuilder.fromUri("http://" + ip[servidor] + ":8080/prueba").build();
			WebTarget target = cliente.target(uri);
			target.path("rest").path("servicio").path("procesos").queryParam("idIp", idIp).queryParam("ips", ips)
					.request(MediaType.TEXT_PLAIN).async().get(new InvocationCallback<Response>() {
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

		return "Comenzamos";
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("procesos")
	public String procesos(@QueryParam(value = "idIp") int idIp, @QueryParam(value = "ips") String ips) {
		servidor = idIp;
		String[] ipsSplit = ips.split(",");

		int i = 0;
		while (i < 2) {
			procesos[i] = new Proceso((2 * servidor) + i, numProcesos, ipsSplit, servidor);
			procesos[i].start();
			i++;
		}

		return "Procesos creados";
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("multicast")
	public String multicast(@QueryParam(value = "Pj") int Pj, @QueryParam(value = "Tj") int Tj,
			@QueryParam(value = "destino") int idProcesoReceptor) {
		if ((idProcesoReceptor % 2) == 0) {
			procesos[0].recibirMensaje(Pj, Tj);
		} else if ((idProcesoReceptor % 2) != 0) {
			procesos[1].recibirMensaje(Pj, Tj);
		}

		return "Realizada la multidifusion";
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("respuesta")
	public String respuesta(@QueryParam(value = "Pj") int Pj, @QueryParam(value = "destino") int idProcesoReceptor) {
		if ((idProcesoReceptor % 2) == 0) {
			procesos[0].recibirRespuesta();
		} else if ((idProcesoReceptor % 2) != 0) {
			procesos[1].recibirRespuesta();
		}

		return "Respuesta enviada";
	}
}
