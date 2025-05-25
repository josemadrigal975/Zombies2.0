/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Cliente;

/**
 *
 * @author jos_m
 */
import Modelos.Jugador;
import Modelos.Mensaje;
import Modelos.TipoMensaje;
import java.io.*;
import java.util.Arrays;
import java.util.List;

public class EscuchaServidorThread extends Thread {
    private ObjectInputStream entrada;
    private ReceptorMensajes receptor;

    public EscuchaServidorThread(ObjectInputStream entrada, ReceptorMensajes receptor) {
        this.entrada = entrada;
        this.receptor = receptor;
    }

    public void setReceptor(ReceptorMensajes nuevo) {
        this.receptor = nuevo;
        System.out.println("🔄 Receptor cambiado a: " + nuevo.getClass().getSimpleName());
    }

    @Override
    public void run() {
        try {
            while (true) {
                Mensaje mensaje = (Mensaje) entrada.readObject();
                System.out.println("📥 Mensaje recibido: " + mensaje);

                if (mensaje.getTipo() == null && mensaje.getContenido().toString().startsWith("UPDATE_JUGADORES")) {
                    String lista = mensaje.getContenido().toString().substring("UPDATE_JUGADORES".length()).trim();
                    String[] jugadores = lista.split(",");

                    if (receptor instanceof ZonaJuego zona) {
                        zona.actualizarListaJugadores(List.of(jugadores));
                    } else if (receptor instanceof PantallaLobby lobby) {
                        lobby.actualizarListaJugadores(List.of(jugadores));
                    }
                } else if (mensaje.getTipo() == TipoMensaje.INICIALIZAR) {
                    if (receptor instanceof ZonaJuego zona) {
                        List<Jugador> listaJugadores = (List<Jugador>) mensaje.getContenido();
                        zona.setJugadores(listaJugadores);
                        zona.repaintMapa();
                    }
                } else {
                    receptor.recibirMensaje(mensaje);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            receptor.recibirMensaje(new Mensaje("Servidor", "Desconectado del servidor", "", null));
        }
    }
}
