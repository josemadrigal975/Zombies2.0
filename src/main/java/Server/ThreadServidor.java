package Server;

import Modelos.Mensaje;
import Modelos.TipoMensaje;
import Personajes.Jugador; // Necesario para el caso CONTROL
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ThreadServidor extends Thread {
    public Socket socket;
    public ObjectOutputStream salida; 
    private ObjectInputStream entrada;  
    private DataInputStream entradaDatosNombre; 
    public String nombre;
    private Servidor server;
    private volatile boolean isRunning = true;

    public ThreadServidor(Socket socket, Servidor server) {
        this.socket = socket;
        this.server = server;
        try {
            this.salida = new ObjectOutputStream(socket.getOutputStream());
            this.salida.flush();
            server.pantalla.write("ThreadServidor (" + socket.getInetAddress() + "): ObjectOutputStream creado y cabecera enviada.");

            this.entradaDatosNombre = new DataInputStream(socket.getInputStream());
            server.pantalla.write("ThreadServidor (" + socket.getInetAddress() + "): DataInputStream para nombre creado.");
            
        } catch (IOException ex) {
            server.pantalla.write("ThreadServidor (" + socket.getInetAddress() + "): ERROR GRAVE al crear streams iniciales: " + ex.getMessage());
            isRunning = false;
            try {
                if (this.socket != null && !this.socket.isClosed()) this.socket.close();
            } catch (IOException e) {
                server.pantalla.write("ThreadServidor (" + socket.getInetAddress() + "): Error cerrando socket en constructor tras fallo: " + e.getMessage());
            }
        }
    }

    @Override
    public void run() {
        if (!isRunning) {
            server.pantalla.write("ThreadServidor (" + (nombre != null ? nombre : socket.getInetAddress()) + "): No se ejecuta debido a error previo en streams.");
            return;
        }

        try {
            nombre = entradaDatosNombre.readUTF();
            server.pantalla.write("ThreadServidor: Nombre recibido del cliente " + socket.getInetAddress() + ": " + nombre);

            // Ahora que el nombre se leyó, se crea el ObjectInputStream para los mensajes.
            // No se puede usar el mismo InputStream que ya fue usado parcialmente por DataInputStream
            // para crear un nuevo ObjectInputStream si el DataInputStream no ha sido cerrado
            // o si el ObjectInputStream no está diseñado para continuar desde donde dejó el DataInputStream.
            // PERO, como DataInputStream solo leyó el nombre, ObjectInputStream debería poder tomar el resto.
            // El ObjectInputStream se crea sobre el socket.getInputStream() original.
            entrada = new ObjectInputStream(socket.getInputStream());
            server.pantalla.write("ThreadServidor (" + nombre + "): ObjectInputStream para mensajes creado.");

            server.registrarJugador(nombre); // Esto ahora añade a nombresEnEspera y actualiza listas

        } catch (IOException ex) {
            server.pantalla.write("ThreadServidor (" + (nombre != null ? nombre : socket.getInetAddress()) + "): Error al leer nombre o crear ObjectInputStream: " + ex.getMessage());
            isRunning = false;
        }

        while (isRunning) {
            try {
                Mensaje mensaje = (Mensaje) entrada.readObject();

                if (mensaje.getTipo() != TipoMensaje.ACTUALIZAR_ESTADO_JUEGO) { // No loguear actualizaciones de estado frecuentes
                    if (mensaje.getTipo() == TipoMensaje.DISPARO_FRANCOTIRADOR && mensaje.getContenido() instanceof java.awt.Point) {
                        server.pantalla.write("ThreadServidor (" + nombre + ") Recibido: " + mensaje.getTipo() + " Contenido: Coordenadas de disparo");
                    } else {
                        server.pantalla.write("ThreadServidor (" + nombre + ") Recibido: " + mensaje.getTipo() + " Contenido: " + (mensaje.getContenido() != null ? mensaje.getContenido().toString() : "null"));
                    }
                }

                switch (mensaje.getTipo()) {
                    case PUBLICO:
                        server.broadcoast(mensaje);
                        break;
                    case PRIVADO:
                        server.privateMessage(mensaje);
                        break;
                    case DISPARO_FRANCOTIRADOR: // Mantenido de A
                        server.procesarDisparoFrancotirador(mensaje);
                        break;
                    case MOVER:
                        server.procesarMovimiento(mensaje);
                        break;
                    case CONTROL: // Nuevo de B
                        if (mensaje.getContenido() instanceof String comando) {
                            server.pantalla.write("ThreadServidor (" + nombre + ") Comando CONTROL recibido: " + comando);
                            if (comando.equals("SALIR_PARTIDA")) {
                                Jugador jugador = server.jugadores.get(mensaje.getEnviador());
                                if (jugador != null) {
                                    jugador.setVivo(false); // Marcar como no vivo (lógica de B para que se reinicie)
                                    jugador.setSalud(0);    // Adicionalmente poner salud a 0
                                    server.pantalla.write("Jugador " + mensaje.getEnviador() + " marcó SALIR_PARTIDA. Considerado como no vivo.");
                                    // La lógica de verificarEstadoJuego se encargará del reinicio.
                                    server.enviarActualizacionEstadoJuego(); // Para que el cambio se refleje
                                }
                            } else if (comando.equals("INICIAR_JUEGO_CLIENTE_LISTO")) {
                                // Este comando podría ser enviado por el cliente cuando presiona "Ingresar al Juego"
                                // El servidor podría usar esto para saber cuándo todos están listos, si es necesario.
                                // Por ahora, iniciarJuego del servidor se llama desde PantallaServidor.
                                server.pantalla.write("Jugador " + nombre + " envió INICIAR_JUEGO_CLIENTE_LISTO.");
                            }
                            // Otros comandos de CONTROL que el servidor deba procesar
                        }
                        break;
                    default:
                        server.pantalla.write("Tipo de mensaje no manejado directamente por ThreadServidor (" + nombre + "): " + mensaje.getTipo());
                }
            } catch (java.io.EOFException e) {
                server.pantalla.write("Cliente " + nombre + " cerró la conexión (EOF).");
                isRunning = false;
            } catch (java.net.SocketException e) {
                server.pantalla.write("Cliente " + nombre + " desconectado (SocketException): " + e.getMessage());
                isRunning = false;
            } catch (IOException | ClassNotFoundException ex) {
                if (isRunning) {
                    server.pantalla.write("Error de comunicación con cliente " + nombre + ": " + ex.getMessage());
                }
                isRunning = false;
            }
        }

        server.eliminarCliente(this);
        try {
            if (salida != null) salida.close();
            if (entrada != null) entrada.close();
            if (entradaDatosNombre != null) entradaDatosNombre.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ex) {
            server.pantalla.write("Error al cerrar recursos para " + nombre + ": " + ex.getMessage());
        }
        server.pantalla.write("Thread para " + nombre + " finalizado.");
    }
}