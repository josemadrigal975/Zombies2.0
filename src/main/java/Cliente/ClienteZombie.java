package Cliente;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javax.swing.JOptionPane;

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
            DataOutputStream dosNombre = new DataOutputStream(socket.getOutputStream());
            dosNombre.writeUTF(nombre);
            dosNombre.flush();
            System.out.println("CLIENTE: Nombre '" + nombre + "' enviado al servidor.");

            // Paso 2: Ahora inicializar los Object Streams.
            salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush(); // Importante para enviar la cabecera del stream al servidor
            System.out.println("CLIENTE: ObjectOutputStream creado y cabecera enviada.");

            entrada = new ObjectInputStream(socket.getInputStream());
            System.out.println("CLIENTE: ObjectInputStream creado.");

            lobby = new PantallaLobby();
            // Pasar 'salida' al constructor de EscuchaServidorThread
            escucha = new EscuchaServidorThread(entrada, salida, lobby); 
            escucha.start();                                     
            System.out.println("CLIENTE: Hilo EscuchaServidor iniciado.");

            lobby.initData(nombre, salida, entrada, escucha);    
            lobby.setVisible(true);
            System.out.println("CLIENTE: PantallaLobby inicializada y visible.");

        } catch (IOException e) {
            System.err.println("CLIENTE: Error al conectar o configurar streams: " + e.getMessage());
            // e.printStackTrace(); // Para debug más detallado
            JOptionPane.showMessageDialog(null, "No se pudo conectar al servidor: " + e.getMessage(), "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            // Considerar cerrar la aplicación si la conexión falla críticamente
            System.exit(1); 
        }
    }
}