/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Cliente;

import java.io.DataOutputStream;
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
            salida = new ObjectOutputStream(socket.getOutputStream());
            entrada = new ObjectInputStream(socket.getInputStream());

            DataOutputStream outUTF = new DataOutputStream(socket.getOutputStream());
            outUTF.writeUTF(nombre);

            lobby = new PantallaLobby();
            escucha = new EscuchaServidorThread(entrada, lobby); // ✅ crear primero
            escucha.start();                                     // ✅ iniciar

            lobby.initData(nombre, salida, entrada, escucha);    // ✅ pasar ya inicializado
            lobby.setVisible(true);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "No se pudo conectar al servidor: " + e.getMessage());
        }
    }

}
