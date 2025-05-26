/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Cliente;

/**
 *
 * @author jos_m
 */
import Personajes.Jugador;
import Modelos.Mensaje;
import Modelos.TipoMensaje;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities; 

public class EscuchaServidorThread extends Thread {
    private ObjectInputStream entrada;
    private ReceptorMensajes receptor; 
    private volatile boolean isRunning = true; 

    public EscuchaServidorThread(ObjectInputStream entrada, ReceptorMensajes receptor) {
        this.entrada = entrada;
        this.receptor = receptor;
    }

    public void setReceptor(ReceptorMensajes nuevoReceptor) {
        this.receptor = nuevoReceptor;
        System.out.println("🔄 Receptor cambiado a: " + nuevoReceptor.getClass().getSimpleName());
    }

    public void detener() {
        isRunning = false;
        
        try {
            if (entrada != null) {
                
            }
        } catch (Exception e) {
            System.err.println("Error al intentar cerrar ObjectInputStream en EscuchaServidorThread: " + e.getMessage());
        }
        this.interrupt(); 
    }


    @Override
    public void run() {
        try {
            while (isRunning) {
                Mensaje mensaje = (Mensaje) entrada.readObject();
                System.out.println("📥 Mensaje recibido del servidor: " + mensaje.getTipo() + " con contenido de tipo " + (mensaje.getContenido() != null ? mensaje.getContenido().getClass().getSimpleName() : "null"));

              
                final Mensaje finalMensaje = mensaje; 
                SwingUtilities.invokeLater(() -> {
                    if (receptor == null) {
                        System.err.println("ADVERTENCIA: Receptor es nulo en EscuchaServidorThread. Mensaje no procesado: " + finalMensaje);
                        return;
                    }

                    switch (finalMensaje.getTipo()) {
                        case ACTUALIZAR_JUGADORES:
                            if (finalMensaje.getContenido() instanceof String) {
                                String listaNombresStr = (String) finalMensaje.getContenido();
                                List<String> nombresJugadores = Arrays.asList(listaNombresStr.split(","));
                                if (receptor instanceof PantallaLobby lobby) {
                                    lobby.actualizarListaJugadores(nombresJugadores);
                                } else if (receptor instanceof ZonaJuego zona) {
                                    zona.actualizarListaJugadores(nombresJugadores);
                                }
                            } else {
                                System.err.println("Error: Contenido de ACTUALIZAR_JUGADORES no es String.");
                            }
                            break;

                        case INICIALIZAR:
                            if (finalMensaje.getContenido() instanceof List) {
                                try {
                                    @SuppressWarnings("unchecked") 
                                    List<Jugador> listaJugadores = (List<Jugador>) finalMensaje.getContenido();
                                    if (receptor instanceof ZonaJuego zona) {
                                        zona.setJugadores(listaJugadores); 
                                    }
                                } catch (ClassCastException e) {
                                    System.err.println("Error: Contenido de INICIALIZAR no es List<Jugador>. " + e.getMessage());
                                }
                            } else {
                                 System.err.println("Error: Contenido de INICIALIZAR no es List.");
                            }
                            break;
                        
                        case ACTUALIZAR_POSICIONES: 
                            if (finalMensaje.getContenido() instanceof List) {
                                 try {
                                    @SuppressWarnings("unchecked")
                                    List<Jugador> jugadoresActualizados = (List<Jugador>) finalMensaje.getContenido();
                                    if (receptor instanceof ZonaJuego zona) {
                                        zona.actualizarPosicionesJugadores(jugadoresActualizados);
                                    }
                                 } catch (ClassCastException e) {
                                    System.err.println("Error: Contenido de ACTUALIZAR_POSICIONES no es List<Jugador>. " + e.getMessage());
                                 }
                            } else {
                                 System.err.println("Error: Contenido de ACTUALIZAR_POSICIONES no es List.");
                            }
                            break;

                        case PUBLICO:
                        case PRIVADO:
                            receptor.recibirMensaje(finalMensaje); 
                            break;
                        
      
                        default:
                            System.out.println("Mensaje de tipo " + finalMensaje.getTipo() + " recibido, pasando al receptor general.");
                            receptor.recibirMensaje(finalMensaje); 
                            break;
                    }
                });

            }
        } catch (EOFException e) {
            System.out.println("INFO: El servidor cerró la conexión (EOFException). " + (receptor != null ? receptor.getClass().getSimpleName() : ""));
            if(receptor != null) receptor.recibirMensaje(new Mensaje("Servidor", "Desconectado del servidor (EOF).", "", null));
        }
        catch (java.net.SocketException e) {
             System.out.println("INFO: Conexión cerrada o reseteada (SocketException): " + e.getMessage() + ". " + (receptor != null ? receptor.getClass().getSimpleName() : ""));
             if(receptor != null) receptor.recibirMensaje(new Mensaje("Servidor", "Conexión perdida con el servidor.", "", null));
        }
        catch (IOException | ClassNotFoundException e) {
            if (isRunning) { 
                System.err.println("Error en EscuchaServidorThread para " + (receptor != null ? receptor.getClass().getSimpleName() : "receptor desconocido") + ": " + e.getMessage());
                e.printStackTrace();
                if (receptor != null) {
                     final String errorMsg = e.getMessage();
                     SwingUtilities.invokeLater(() -> receptor.recibirMensaje(new Mensaje("Error", "Error de comunicación: " + errorMsg, "", null)));
                }
            }
        } finally {
            System.out.println("EscuchaServidorThread finalizado para " + (receptor != null ? receptor.getClass().getSimpleName() : "receptor desconocido"));
          
        }
    }
}