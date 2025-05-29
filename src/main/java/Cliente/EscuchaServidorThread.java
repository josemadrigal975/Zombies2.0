package Cliente;

import Personajes.Jugador;
import Modelos.Mensaje;
import Modelos.TipoMensaje;
import Modelos.ActualizacionEstadoJuegoDTO;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class EscuchaServidorThread extends Thread {
    private ObjectInputStream entrada;
    private ObjectOutputStream salida; // Añadido para que pueda pasarse a PantallaLobby
    private ReceptorMensajes receptor;
    private volatile boolean isRunning = true;
    private String nombreMapaActual = "mapa1.txt"; // Default, se actualizará
    private List<String> jugadoresConectadosLobby; // Para mantener la lista de jugadores del lobby

    public EscuchaServidorThread(ObjectInputStream entrada, ObjectOutputStream salida, ReceptorMensajes receptor) {
        this.entrada = entrada;
        this.salida = salida; // Almacenar salida
        this.receptor = receptor;
    }

    public void setReceptor(ReceptorMensajes nuevoReceptor) {
        this.receptor = nuevoReceptor;
        System.out.println("CLIENTE EscuchaServidorThread: 🔄 Receptor cambiado a: " + nuevoReceptor.getClass().getSimpleName());
        if (nuevoReceptor instanceof ZonaJuego && jugadoresConectadosLobby != null) {
            ((ZonaJuego) nuevoReceptor).actualizarListaJugadores(jugadoresConectadosLobby);
        }
    }
    
    public String getNombreMapaActual() {
        return nombreMapaActual;
    }

    public void detener() {
        isRunning = false;
        try {
            if (entrada != null) {
                // No cerrar 'entrada' aquí directamente, el socket lo hará
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
                                jugadoresConectadosLobby = nombresJugadores; // Guardar para ZonaJuego
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
                                    // System.out.println("CLIENTE EscuchaServidorThread: Recibido ACTUALIZAR_ESTADO_JUEGO.");
                                    zona.actualizarEstadoJuego(dto.getJugadores(), dto.getZombies());
                                }
                            } else {
                                 System.err.println("CLIENTE EscuchaServidorThread: Error: Contenido de ACTUALIZAR_ESTADO_JUEGO no es ActualizacionEstadoJuegoDTO.");
                            }
                            break;
                        
                        case PUBLICO:
                        case PRIVADO:
                            receptor.recibirMensaje(finalMensaje);
                            break;
                        
                        case FINALIZAR_JUEGO: 
                            System.out.println("CLIENTE EscuchaServidorThread: Mensaje FINALIZAR_JUEGO recibido.");
                            receptor.recibirMensaje(finalMensaje); // El receptor (ZonaJuego o Lobby) decidirá qué hacer
                            JOptionPane.showMessageDialog(null, "El servidor ha cerrado la partida o se ha detenido.", "Desconectado", JOptionPane.INFORMATION_MESSAGE);
                            // Podríamos cerrar la ventana actual o llevar a una pantalla inicial.
                            // Por ahora, solo se notifica. El botón de salir del servidor en PantallaServidor hace System.exit(0) eventualmente.
                            if (receptor instanceof ZonaJuego zona) zona.dispose();
                            else if (receptor instanceof PantallaLobby lobby) lobby.dispose();
                            // Considerar System.exit(0) si es el fin definitivo.
                            break;
                        
                        case LLEGO_META: // Nuevo de B
                            if (receptor instanceof ZonaJuego zona) {
                                zona.desactivarControles(); 
                                String contenidoMeta = finalMensaje.getContenido() != null ? finalMensaje.getContenido().toString() : "¡Has llegado a la salida!";
                                JOptionPane.showMessageDialog(zona, contenidoMeta, "Nivel Completado", JOptionPane.INFORMATION_MESSAGE);
                            }
                            break;

                        case ACTIVAR_CONTROLES: // Nuevo de B
                             if (receptor instanceof ZonaJuego zona) {
                                zona.activarControles();
                            }
                            break;

                        case ACTUALIZAR_TIEMPO: // Nuevo de B
                            if (receptor instanceof ZonaJuego zona && finalMensaje.getContenido() instanceof Long segundos) {
                                zona.actualizarLabelTiempoConServidor(segundos);
                            }
                            break;
                        
                        case REINICIAR_JUEGO: // Nuevo de B
                            if (finalMensaje.getContenido() instanceof String nombreMapaNuevo) {
                                this.nombreMapaActual = nombreMapaNuevo;
                                if (receptor instanceof ZonaJuego zona) {
                                    zona.cargarPanelMapa(nombreMapaActual);  
                                    zona.iniciarJuego(); // Esto activará controles e iniciará cronómetro local
                                    zona.activarControles(); // Asegurar activación
                                } else if (receptor instanceof PantallaLobby lobby) {
                                     lobby.setNombreMapaActual(nombreMapaActual);
                                     lobby.habilitarIngreso(); // Permitir ingresar al nuevo nivel
                                }
                            }
                            break;

                        case CONTROL: // Nuevo de B
                            if (finalMensaje.getContenido() instanceof String comando) {
                                if (comando.equals("INICIAR_JUEGO") && receptor instanceof ZonaJuego zona) {
                                    zona.iniciarJuego(); // Inicia cronómetro y activa controles
                                }
                                // Otros comandos de control si son necesarios del servidor al cliente
                            }
                            break;
                        
                        case VOLVER_LOBBY: // Nuevo de B
                             if (receptor instanceof ZonaJuego zona) {
                                String nombreJugadorActual = zona.getNombreJugador();
                                zona.dispose();

                                PantallaLobby lobby = new PantallaLobby();
                                // El 'this' es el EscuchaServidorThread actual.
                                // Se necesita 'salida' y 'entrada' para el initData de PantallaLobby.
                                lobby.initData(nombreJugadorActual, this.salida, this.entrada, this);
                                setReceptor(lobby); // Cambiar el receptor de mensajes a la nueva pantalla de lobby
                                lobby.setVisible(true);
                                lobby.habilitarIngreso(); // O bloquear según la lógica del servidor
                                lobby.areaChat.append("[Sistema] Has vuelto al lobby. Espera para unirte a una nueva partida o al reinicio.\n");
                            }
                            break;

                        default:
                            receptor.recibirMensaje(finalMensaje); 
                            break;
                    }
                });
            }
        } catch (EOFException e) {
            System.out.println("CLIENTE EscuchaServidorThread: INFO: El servidor cerró la conexión (EOFException). " + (receptor != null ? receptor.getClass().getSimpleName() : ""));
            if(receptor != null && isRunning) receptor.recibirMensaje(new Mensaje("Servidor", "Desconectado del servidor (EOF).", "LOCAL", TipoMensaje.PRIVADO));
        }
        catch (java.net.SocketException e) {
             System.out.println("CLIENTE EscuchaServidorThread: INFO: Conexión cerrada o reseteada (SocketException): " + e.getMessage() + ". " + (receptor != null ? receptor.getClass().getSimpleName() : ""));
             if(receptor != null && isRunning) receptor.recibirMensaje(new Mensaje("Servidor", "Conexión perdida con el servidor.", "LOCAL", TipoMensaje.PRIVADO));
        }
        catch (IOException | ClassNotFoundException e) {
            if (isRunning) { 
                System.err.println("CLIENTE EscuchaServidorThread: Error en EscuchaServidorThread para " + (receptor != null ? receptor.getClass().getSimpleName() : "receptor desconocido") + ": " + e.getMessage());
                if (receptor != null) {
                     final String errorMsg = e.getMessage();
                     SwingUtilities.invokeLater(() -> receptor.recibirMensaje(new Mensaje("Error", "Error de comunicación: " + errorMsg, "LOCAL", TipoMensaje.PRIVADO)));
                }
            }
        } finally {
            System.out.println("CLIENTE EscuchaServidorThread: Finalizado para " + (receptor != null ? receptor.getClass().getSimpleName() : "receptor desconocido"));
            isRunning = false; // Asegurar que se detenga
            // No cerrar streams aquí, se maneja en ClienteZombie o al cerrar la app.
        }
    }
}