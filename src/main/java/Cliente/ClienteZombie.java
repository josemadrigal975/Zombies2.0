package Cliente;

import java.io.DataOutputStream; // Asegúrate que esté importado
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javax.swing.JOptionPane;

/**
 *
 * @author jos_m
 */
public class ClienteZombie {
    private String nombre;
    private Socket socket;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private PantallaLobby lobby;
    private EscuchaServidorThread escucha;

    public ClienteZombie(String nombre) {
        this.nombre = nombre;
        conectarAlServidor();
    }

    private void conectarAlServidor() {
        try {
            socket = new Socket("localhost", 8084);
            System.out.println("CLIENTE: Conectado al servidor.");

            // Paso 1: Enviar el nombre usando DataOutputStream ANTES de inicializar ObjectOutputStream
            // Esto es porque el servidor (ThreadServidor) espera leer el nombre con DataInputStream
            // antes de intentar crear su propio ObjectInputStream.
            DataOutputStream dosNombre = new DataOutputStream(socket.getOutputStream());
            dosNombre.writeUTF(nombre);
            dosNombre.flush(); // Asegura que el nombre se envíe inmediatamente
            System.out.println("CLIENTE: Nombre '" + nombre + "' enviado al servidor.");

            // Paso 2: Ahora inicializar los Object Streams.
            // El cliente crea su ObjectOutputStream. El flush() es importante porque envía
            // la cabecera del stream, que el servidor necesita para crear su ObjectInputStream.
            salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush();
            System.out.println("CLIENTE: ObjectOutputStream creado y cabecera enviada.");

            // El cliente crea su ObjectInputStream. Esto esperará la cabecera del OOS del servidor.
            entrada = new ObjectInputStream(socket.getInputStream());
            System.out.println("CLIENTE: ObjectInputStream creado.");

            lobby = new PantallaLobby();
            escucha = new EscuchaServidorThread(entrada, lobby);
            escucha.start();
            System.out.println("CLIENTE: Hilo EscuchaServidor iniciado.");

            lobby.initData(nombre, salida, entrada, escucha);
            lobby.setVisible(true);
            System.out.println("CLIENTE: PantallaLobby inicializada y visible.");

        } catch (IOException e) {
            System.err.println("CLIENTE: Error al conectar o configurar streams: " + e.getMessage());
            e.printStackTrace(); // Muestra el stack trace completo del error.
            JOptionPane.showMessageDialog(null, "No se pudo conectar al servidor: " + e.getMessage(), "Error de Conexión", JOptionPane.ERROR_MESSAGE);
        }
    }
}