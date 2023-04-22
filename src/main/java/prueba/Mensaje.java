package prueba;

public class Mensaje {
	private int idProceso; //ID del proceso emisor del mensaje
	private int marcaTemporalLamport; //Marca de tiempo del proceso emisor en el momento de enviar el mensaje
	private TipoMensaje tipoMensaje; //Puede ser: solicitud de acceso a zona crítica, confirmación, liberación...

	public Mensaje(int idProceso, int marcaTemporalLamport, TipoMensaje tipoMensaje) {
		this.setIdProceso(idProceso);
		this.setMarcaTemporalLamport(marcaTemporalLamport);
		this.setTipoMensaje(tipoMensaje);
	}

	public int getIdProceso() {
		return idProceso;
	}

	public void setIdProceso(int idProceso) {
		this.idProceso = idProceso;
	}

	public int getMarcaTemporalLamport() {
		return marcaTemporalLamport;
	}

	public void setMarcaTemporalLamport(int marcaTemporalLamport) {
		this.marcaTemporalLamport = marcaTemporalLamport;
	}

	public TipoMensaje getTipoMensaje() {
        return tipoMensaje;
    }

    public void setTipoMensaje(TipoMensaje tipoMensaje) {
        this.tipoMensaje = tipoMensaje;
    }
}
