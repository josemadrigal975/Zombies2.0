package Cliente;

import Personajes.Jugador;
// import Personajes.Zombie; // No es necesario importar Zombie aquí si solo se pasa el DTO
import Modelos.Mensaje;
import Modelos.TipoMensaje;
import Modelos.ActualizacionEstadoJuegoDTO; // << NUEVO
import java.io.*;
import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities; // Para asegurar actualizaciones de UI en el EDT

public class EscuchaServidorThread extends Thread {
    private ObjectInputStream entrada;
    private ReceptorMensajes receptor; // Puede ser PantallaLobby o ZonaJuego
    private volatile boolean isRunning = true; // Para detener el hilo limpiamente

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
                // No cerrar 'entrada' aquí directamente
            }
        } catch (Exception e) {
            System.err.println("Error al intentar gestionar ObjectInputStream en EscuchaServidorThread al detener: " + e.getMessage());
        }
        this.interrupt(); 
    }


    @Override
    public void run() {
        try {
            while (isRunning) {
                Mensaje mensaje = (Mensaje) entrada.readObject();
                // Descomentar para log general de mensajes
                // System.out.println("CLIENTE EscuchaServidorThread: 📥 Mensaje recibido del servidor: " + mensaje.getTipo() + " con contenido de tipo " + (mensaje.getContenido() != null ? mensaje.getContenido().getClass().getSimpleName() : "null"));

                final Mensaje finalMensaje = mensaje; 
                SwingUtilities.invokeLater(() -> {
                    if (receptor == null) {
                        System.err.println("CLIENTE EscuchaServidorThread: ADVERTENCIA: Receptor es nulo. Mensaje no procesado: " + finalMensaje);
                        return;
                    }

                    switch (finalMensaje.getTipo()) {
                        case ACTUALIZAR_JUGADORES: 
                            if (finalMensaje.getContenido() instanceof String listaNombresStr) {
                                List<String> nombresJugadores = Arrays.asList(listaNombresStr.split(","));
                                if (receptor instanceof PantallaLobby lobby) {
                                    lobby.actualizarListaJugadores(nombresJugadores);
                                } else if (receptor instanceof ZonaJuego zona) {
                                    zona.actualizarListaJugadores(nombresJugadores);
                                }
                            } else {
                                System.err.println("CLIENTE EscuchaServidorThread: Error: Contenido de ACTUALIZAR_JUGADORES no es String.");
                            }
                            break;

                        case ACTUALIZAR_ESTADO_JUEGO: 
                            if (finalMensaje.getContenido() instanceof ActualizacionEstadoJuegoDTO dto) {
                                if (receptor instanceof ZonaJuego zona) {
                                    System.out.println("CLIENTE EscuchaServidorThread: Recibido ACTUALIZAR_ESTADO_JUEGO. Jugadores: " + 
                                                       (dto.getJugadores() != null ? dto.getJugadores().size() : "null") + 
                                                       ", Zombies: " + (dto.getZombies() != null ? dto.getZombies().size() : "null"));
                                    if (dto.getJugadores() != null && !dto.getJugadores().isEmpty()) {
                                        Jugador primerJugador = dto.getJugadores().get(0);
                                        System.out.println("CLIENTE EscuchaServidorThread: Primer jugador en DTO: " + primerJugador.getNombre() + 
                                                           " en (" + primerJugador.getX() + "," + primerJugador.getY() + "), Salud: " + primerJugador.getSalud());
                                    }
                                    zona.actualizarEstadoJuego(dto.getJugadores(), dto.getZombies());
                                }
                            } else {
                                 System.err.println("CLIENTE EscuchaServidorThread: Error: Contenido de ACTUALIZAR_ESTADO_JUEGO no es ActualizacionEstadoJuegoDTO. Es: " + (finalMensaje.getContenido() != null ? finalMensaje.getContenido().getClass().getName() : "null"));
                            }
                            break;
                        
                        case PUBLICO:
                        case PRIVADO:
                            receptor.recibirMensaje(finalMensaje);
                            break;
                        
                        case FINALIZAR_JUEGO: 
                            System.out.println("CLIENTE EscuchaServidorThread: Mensaje FINALIZAR_JUEGO recibido.");
                            receptor.recibirMensaje(finalMensaje);
                            break;

                        default:
                            // System.out.println("CLIENTE EscuchaServidorThread: Mensaje de tipo " + finalMensaje.getTipo() + " recibido, pasando al receptor general.");
                            receptor.recibirMensaje(finalMensaje); 
                            break;
                    }
                });

            }
        } catch (EOFException e) {
            System.out.println("CLIENTE EscuchaServidorThread: INFO: El servidor cerró la conexión (EOFException). " + (receptor != null ? receptor.getClass().getSimpleName() : ""));
            if(receptor != null && isRunning) receptor.recibirMensaje(new Mensaje("Servidor", "Desconectado del servidor (EOF).", "", null));
        }
        catch (java.net.SocketException e) {
             System.out.println("CLIENTE EscuchaServidorThread: INFO: Conexión cerrada o reseteada (SocketException): " + e.getMessage() + ". " + (receptor != null ? receptor.getClass().getSimpleName() : ""));
             if(receptor != null && isRunning) receptor.recibirMensaje(new Mensaje("Servidor", "Conexión perdida con el servidor.", "", null));
        }
        catch (IOException | ClassNotFoundException e) {
            if (isRunning) { 
                System.err.println("CLIENTE EscuchaServidorThread: Error en EscuchaServidorThread para " + (receptor != null ? receptor.getClass().getSimpleName() : "receptor desconocido") + ": " + e.getMessage());
                // e.printStackTrace(); 
                if (receptor != null) {
                     final String errorMsg = e.getMessage();
                     SwingUtilities.invokeLater(() -> receptor.recibirMensaje(new Mensaje("Error", "Error de comunicación: " + errorMsg, "", null)));
                }
            }
        } finally {
            System.out.println("CLIENTE EscuchaServidorThread: Finalizado para " + (receptor != null ? receptor.getClass().getSimpleName() : "receptor desconocido"));
        }
    }
}