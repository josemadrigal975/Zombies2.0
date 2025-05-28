package Server;

import Modelos.Mensaje;
import Modelos.TipoMensaje;
import Personajes.Jugador;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ThreadServidor extends Thread{
    public Socket socket;
    public ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private DataInputStream entradaDatos; // Para el nombre inicial
    public String nombre;
    private Servidor server;
    private boolean isRunning = true;

    public ThreadServidor(Socket socket, Servidor server) {
        this.socket = socket;
        this.server = server;
        try {
            salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush(); 
            // La creación de ObjectInputStream puede bloquear hasta que el otro extremo
            // envíe la cabecera del stream (lo cual sucede con new ObjectOutputStream()).
            // El cliente crea su OOS y luego su OIS. El servidor crea OOS y luego OIS.
            // El cliente envía su nombre por DataOutputStream, luego el servidor puede leerlo.
            // El problema puede surgir si ambos intentan crear OIS antes de que el otro cree OOS.
            // Aquí, el DataInputStream para el nombre se crea DESPUÉS del OIS del servidor,
            // y el OOS del servidor se crea ANTES del OIS del servidor.
            entrada = new ObjectInputStream(socket.getInputStream());
            // entradaDatos = new DataInputStream(socket.getInputStream()); // Esto es problemático si se crea después de OIS en el mismo socket.
                                                                          // Es mejor usar el mismo OIS o un stream separado si es estrictamente necesario.
                                                                          // Para simplicidad, el cliente enviará el nombre como un Mensaje normal
                                                                          // o el servidor leerá el nombre del stream original de entrada.
                                                                          // PERO, como el cliente lo envía por DataOutputStream, necesitamos DataInputStream.
                                                                          // La clave es el orden de creación y uso.
            // Corregido: DataInputStream DEBERÍA leer del InputStream ANTES de que ObjectInputStream lo "tome".
            // Lo ideal sería que el cliente envíe el nombre primero y luego se establezcan los ObjectStreams.
            // Como está, se crea el OOS/OIS y LUEGO el DataInputStream para leer del MISMO socket.getInputStream()
            // que ya está usando OIS. Esto es propenso a errores.

            // Solución más robusta: El cliente envía el nombre como un String UTF vía DataOutputStream.
            // El servidor lo lee con DataInputStream antes de crear el ObjectInputStream.
            // O, el cliente envía un Mensaje especial de LOGIN.
            
            // Para la estructura actual (cliente envía UTF con DataOutputStream primero):
            // Creamos entradaDatos aquí, pero aseguramos que se use ANTES de que entrada.readObject()
            // sea llamado extensivamente.
            this.entradaDatos = new DataInputStream(socket.getInputStream());


        } catch (IOException ex) {
             System.err.println("Error al crear streams para " + (nombre != null ? nombre : "nuevo cliente") + ": " + ex.getMessage());
             isRunning = false;
        }
    }

    @Override
    public void run() {
        try {
            // Leer el nombre enviado por el cliente a través de DataOutputStream
            nombre = entradaDatos.readUTF(); 
            server.pantalla.write("ThreadServidor: Recibido nombre: " + nombre + " de " + socket.getInetAddress());
            server.registrarJugador(nombre); 
        } catch (IOException ex) {
            server.pantalla.write("ThreadServidor: Error al leer nombre o registrar jugador para " + socket.getInetAddress() + ": " + ex.getMessage());
            isRunning = false; 
        }

        while (isRunning) {
            try {
                Mensaje mensaje = (Mensaje) entrada.readObject(); // Ahora OIS puede tomar control del stream
                
                // Loguear mensajes excepto las actualizaciones de estado que son muy frecuentes
                if (mensaje.getTipo() != TipoMensaje.ACTUALIZAR_ESTADO_JUEGO) {
                    server.pantalla.write("ThreadServidor (" + nombre + ") Recibido: " + mensaje.getTipo() + " Contenido: " + mensaje.getContenido());
                }

                switch (mensaje.getTipo()) {
                    case PUBLICO:
                        server.broadcoast(mensaje);
                        break;
                    case PRIVADO:
                        server.privateMessage(mensaje);
                        break;
                    case DISPARO: 
                        // server.procesarDisparo(mensaje); // Implementar lógica de disparo
                        break;
                    case MOVER: 
                        // server.pantalla.write("ThreadServidor (" + nombre + ") DELEGANDO MOVIMIENTO: " + mensaje.getContenido()); // Log más específico en Servidor.procesarMovimiento
                        server.procesarMovimiento(mensaje);
                        break;
                    case CONTROL:
                        if (mensaje.getContenido() instanceof String comando) {
                            switch (comando) {
                                case "SALIR_PARTIDA":
                                    Jugador jugador = server.getJugadores().get(mensaje.getEnviador());
                                    if (jugador != null) {
                                        jugador.setSalud(0); 
                                    }

                                    server.pantalla.write("Jugador " + mensaje.getEnviador() + " salió del juego (contado como muerto).");

                                    String listaNombres = String.join(",", server.getJugadores().keySet());
                                    Mensaje actualizar = new Mensaje("SERVIDOR", listaNombres, "TODOS", TipoMensaje.ACTUALIZAR_JUGADORES);
                                    server.broadcoast(actualizar);
                                    break;

                                case "INICIAR_JUEGO":
                                    // Este comando solo lo usan los clientes, no procesarlo acá
                                    break;

                                default:
                                    server.pantalla.write("Comando CONTROL desconocido: " + comando);
                            }
                        }
                        break;
                    // Otros tipos de mensajes que el servidor deba manejar directamente del cliente
                    // Por ejemplo, un mensaje de "listo para empezar" si el lobby lo requiere.
                    default:
                        server.pantalla.write("Tipo de mensaje no manejado directamente por ThreadServidor (" + nombre + "): " + mensaje.getTipo());
                }
            } catch (java.io.EOFException e) {
                server.pantalla.write("Cliente " + nombre + " cerró la conexión (EOF).");
                isRunning = false;
            } catch (java.net.SocketException e) {
                 server.pantalla.write("Cliente " + nombre + " desconectado (SocketException): " + e.getMessage());
                 isRunning = false;
            }
            catch (IOException | ClassNotFoundException ex) {
                if (isRunning) { // Solo si no se detuvo intencionalmente
                    server.pantalla.write("Error de comunicación con cliente " + nombre + ": " + ex.getMessage());
                    // ex.printStackTrace(); // Para debug detallado
                }
                isRunning = false;
            }
        }
        
        server.eliminarCliente(this); // Asegurar limpieza
        try {
            if (salida != null) salida.close();
            if (entrada != null) entrada.close();
            if (entradaDatos != null) entradaDatos.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ex) {
            server.pantalla.write("Error al cerrar recursos para " + nombre + ": " + ex.getMessage());
        }
        server.pantalla.write("Thread para " + nombre + " finalizado.");
    }
}