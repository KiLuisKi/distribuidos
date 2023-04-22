package prueba;

public class Comparador {

	public boolean compara(int ti, int tj, int pi, int pj) {
		if (ti < tj)
			return true;
		else if (ti > tj)
			return false;
		else {
			if (pi < pj) {
				return true;
			} else {
				return false;
			}
		}
	}

}
