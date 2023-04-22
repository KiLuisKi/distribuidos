package prueba;

public enum Estado {
    LIBERADA,    // El proceso no está en la sección crítica
    BUSCADA,     // El proceso ha enviado una petición para entrar en la sección crítica
    TOMADA       // El proceso ha entrado en la sección crítica
}