package Server;

import Modelos.Mensaje;
import Modelos.TipoMensaje;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ThreadServidor extends Thread {
    public Socket socket;
    public ObjectOutputStream salida; // Stream para enviar objetos al cliente
    private ObjectInputStream entrada;  // Stream para recibir objetos del cliente
    private DataInputStream entradaDatosNombre; // Stream específico para leer el nombre inicial (String UTF)
    public String nombre;
    private Servidor server;
    private volatile boolean isRunning = true;

    public ThreadServidor(Socket socket, Servidor server) {
        this.socket = socket;
        this.server = server;
        try {
            // Paso 1 (Servidor): Crear ObjectOutputStream para enviar mensajes al cliente.
            // El flush() envía la cabecera del stream, que el cliente necesita para crear su ObjectInputStream.
            this.salida = new ObjectOutputStream(socket.getOutputStream());
            this.salida.flush();
            server.pantalla.write("ThreadServidor (" + socket.getInetAddress() + "): ObjectOutputStream creado y cabecera enviada al cliente.");

            // Paso 2 (Servidor): Crear DataInputStream para leer el nombre (String UTF)
            // que el cliente envía primero, ANTES de cualquier objeto Mensaje.
            this.entradaDatosNombre = new DataInputStream(socket.getInputStream());
            server.pantalla.write("ThreadServidor (" + socket.getInetAddress() + "): DataInputStream para nombre creado.");
            
            // El ObjectInputStream (this.entrada) para leer objetos Mensaje se creará en run(),
            // DESPUÉS de que el nombre haya sido leído usando entradaDatosNombre.

        } catch (IOException ex) {
            server.pantalla.write("ThreadServidor (" + socket.getInetAddress() + "): ERROR GRAVE al crear streams iniciales: " + ex.getMessage());
            ex.printStackTrace();
            isRunning = false;
            // Intentar cerrar el socket si falla la inicialización de streams
            try {
                if (this.socket != null && !this.socket.isClosed()) {
                    this.socket.close();
                }
            } catch (IOException e) {
                server.pantalla.write("ThreadServidor (" + socket.getInetAddress() + "): Error cerrando socket en constructor tras fallo: " + e.getMessage());
            }
        }
    }

    @Override
    public void run() {
        if (!isRunning) { // Si los streams fallaron en el constructor, no continuar.
            server.pantalla.write("ThreadServidor (" + (nombre != null ? nombre : socket.getInetAddress()) + "): No se ejecuta debido a error previo en streams.");
            // No es necesario llamar a server.eliminarCliente aquí si el thread nunca se añadió correctamente
            // o si el servidor tiene otra forma de limpiar threads fallidos.
            return;
        }

        try {
            // Paso 3 (Servidor): Leer el nombre del cliente.
            // Esto bloqueará hasta que el cliente envíe el nombre vía DataOutputStream.writeUTF().
            nombre = entradaDatosNombre.readUTF();
            server.pantalla.write("ThreadServidor: Nombre recibido del cliente " + socket.getInetAddress() + ": " + nombre);

            // Paso 4 (Servidor): Ahora que el nombre se ha leído (y DataInputStream ha consumido esa parte del stream),
            // se puede crear el ObjectInputStream para leer los objetos Mensaje.
            // Esto esperará la cabecera del ObjectOutputStream del cliente (que el cliente envía con su salida.flush()).
            entrada = new ObjectInputStream(socket.getInputStream());
            server.pantalla.write("ThreadServidor (" + nombre + "): ObjectInputStream para mensajes creado.");

            // Registrar al jugador en el servidor.
            server.registrarJugador(nombre);
            // El servidor, dentro de registrarJugador, llamará a enviarActualizacionListaJugadores,
            // que a su vez utiliza la lista `clientesAceptados`.
            // ConexionThreads ya añade este `ts` a `clientesAceptados` después de `ts.start()`.

        } catch (IOException ex) {
            server.pantalla.write("ThreadServidor (" + (nombre != null ? nombre : socket.getInetAddress()) + "): Error al leer nombre o crear ObjectInputStream: " + ex.getMessage());
            // ex.printStackTrace();
            isRunning = false; // Detener el hilo si falla esta parte crucial.
        }

        while (isRunning) {
            try {
                Mensaje mensaje = (Mensaje) entrada.readObject();

                if (mensaje.getTipo() != TipoMensaje.ACTUALIZAR_ESTADO_JUEGO) {
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
                    case DISPARO_FRANCOTIRADOR:
                        server.procesarDisparoFrancotirador(mensaje);
                        break;
                    case MOVER:
                        server.procesarMovimiento(mensaje);
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
                    // ex.printStackTrace(); 
                }
                isRunning = false;
            }
        }

        server.eliminarCliente(this);
        try {
            if (salida != null) salida.close();
            if (entrada != null) entrada.close();
            if (entradaDatosNombre != null) entradaDatosNombre.close(); // Cerrar también este stream
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ex) {
            server.pantalla.write("Error al cerrar recursos para " + nombre + ": " + ex.getMessage());
        }
        server.pantalla.write("Thread para " + nombre + " finalizado.");
    }
}