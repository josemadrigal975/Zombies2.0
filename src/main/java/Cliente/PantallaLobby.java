/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Cliente;

import Fondos.FondoPanel;
import java.io.ObjectOutputStream;
import Modelos.Mensaje;
import Modelos.TipoMensaje;
import Sonidos.ReproductorAudio;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.List;
import javax.swing.JOptionPane; // Añadido para JOptionPane

/**
 *
 * @author jos_m
 */
public class PantallaLobby extends javax.swing.JFrame implements ReceptorMensajes {
    private String nombreJugador;
    private ReproductorAudio reproductor = new ReproductorAudio();
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private EscuchaServidorThread escucha;
    private String nombreMapaActual = "mapa1.txt"; // Nuevo de B, con default
    private boolean puedeIngresar = true; // Nuevo de B

    
    /**
     * Creates new form PantallaLobby
     */
    public PantallaLobby() {
        initComponents();
        this.setSize(1000, 520);
        this.setLocationRelativeTo(null);
        
        URL ruta = getClass().getResource("/Imagenes/fondoLobby.jpg");
        FondoPanel fondo = new FondoPanel(ruta);
        fondo.setOpaque(false);

        panelLobby.setLayout(new java.awt.BorderLayout());
        panelLobby.add(fondo, java.awt.BorderLayout.CENTER);
        panelLobby.setComponentZOrder(fondo, panelLobby.getComponentCount() - 1);
        
        reproductor.reproducir("/Sonidos/musica.wav");
        areaChat.setEditable(false);
        btnPartida.setEnabled(true); // Por defecto puede ingresar
    }
    
    @Override
    public void dispose() {
        reproductor.detener(); 
        super.dispose();       
    }
    
    public void initData(String nombreJugador, ObjectOutputStream salida, ObjectInputStream entrada, EscuchaServidorThread escucha) {
        this.nombreJugador = nombreJugador;
        this.salida = salida;
        this.entrada = entrada;
        this.escucha = escucha; // El EscuchaServidorThread ya debería estar iniciado por ClienteZombie
        if (this.escucha != null) {
            this.escucha.setReceptor(this); // Asegurarse que este Lobby sea el receptor actual
            this.nombreMapaActual = this.escucha.getNombreMapaActual(); // Obtener el mapa actual del listener
        }
        setTitle("Lobby de " + nombreJugador);
        // No es necesario enviar un mensaje de INICIALIZAR_LOBBY aquí, el servidor ya envía ACTUALIZAR_JUGADORES al conectar.
    }
    
    // Métodos nuevos/modificados de la versión B
    public void bloquearIngreso() {
        puedeIngresar = false;
        btnPartida.setEnabled(false);
        areaChat.append("[Sistema] Ya has salido de la partida o el juego ha terminado. Espera el reinicio o una nueva partida.\n");
        areaChat.setCaretPosition(areaChat.getDocument().getLength());
    }

    public void habilitarIngreso() {
        puedeIngresar = true;
        btnPartida.setEnabled(true);
        areaChat.append("[Sistema] Puedes unirte a la partida.\n");
        areaChat.setCaretPosition(areaChat.getDocument().getLength());
    }
    
    public void setNombreMapaActual(String nombreMapa) {
        this.nombreMapaActual = nombreMapa;
    }
    
    @Override
    public void recibirMensaje(Mensaje mensaje) {
        // Este método ahora solo recibe mensajes de chat o del sistema para el lobby.
        // Las actualizaciones de estado del juego y jugadores las maneja EscuchaServidorThread
        // y llama a los métodos específicos de esta clase si es necesario.
        if (mensaje.getTipo() == TipoMensaje.PUBLICO) {
            areaChat.append(mensaje.getEnviador() + ": " + mensaje.getContenido().toString() + "\n");
        } else if (mensaje.getTipo() == TipoMensaje.PRIVADO) {
            if (mensaje.getEnviador().equals("Servidor") || mensaje.getReceptor().equals("LOCAL")) { // Mensajes del sistema/feedback
                 areaChat.append("[Sistema] " + mensaje.getContenido().toString() + "\n");
             } else if (!mensaje.getEnviador().equals(nombreJugador)) { 
                 areaChat.append("[Privado de " + mensaje.getEnviador() + "] " + mensaje.getContenido().toString() + "\n");
             }
        } else if (mensaje.getTipo() == TipoMensaje.FINALIZAR_JUEGO) {
             areaChat.append("[Servidor] " + mensaje.getContenido().toString() + "\n");
             // Podríamos deshabilitar más cosas o mostrar un diálogo
             btnPartida.setEnabled(false);
             Desconectarse.setEnabled(false); // Asumiendo que este es el botón de salir del juego
        }
        areaChat.setCaretPosition(areaChat.getDocument().getLength());
    }
    
    public void actualizarListaJugadores(List<String> jugadores) {
        comboReceptores.removeAllItems();
        comboReceptores.addItem("ALL"); 

        for (String jugador : jugadores) {
            if (!jugador.equals(nombreJugador)) {
                comboReceptores.addItem(jugador);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelLobby = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        areaChat = new javax.swing.JTextArea();
        Desconectarse = new javax.swing.JButton();
        btnPublico = new javax.swing.JButton();
        txtPublico = new javax.swing.JTextField();
        btnSendPrivate = new javax.swing.JButton();
        lblPersona = new javax.swing.JLabel();
        lblDetalle = new javax.swing.JLabel();
        txtDetalle = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        btnPartida = new javax.swing.JButton();
        comboReceptores = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        areaChat.setColumns(20);
        areaChat.setRows(5);
        jScrollPane1.setViewportView(areaChat);

        Desconectarse.setFont(new java.awt.Font("SimSun-ExtG", 0, 12)); // NOI18N
        Desconectarse.setText("Desconectarse del Servidor");
        Desconectarse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DesconectarseActionPerformed(evt);
            }
        });

        btnPublico.setFont(new java.awt.Font("SimSun-ExtB", 0, 12)); // NOI18N
        btnPublico.setText("Enviar Mensaje Público");
        btnPublico.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPublicoActionPerformed(evt);
            }
        });

        txtPublico.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtPublicoKeyPressed(evt);
            }
        });

        btnSendPrivate.setFont(new java.awt.Font("SimSun-ExtB", 0, 12)); // NOI18N
        btnSendPrivate.setText("Enviar Mensaje Privado");
        btnSendPrivate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSendPrivateActionPerformed(evt);
            }
        });

        lblPersona.setFont(new java.awt.Font("SimSun-ExtB", 0, 12)); // NOI18N
        lblPersona.setForeground(new java.awt.Color(255, 255, 255));
        lblPersona.setText("Persona");

        lblDetalle.setFont(new java.awt.Font("SimSun-ExtB", 0, 12)); // NOI18N
        lblDetalle.setForeground(new java.awt.Color(255, 255, 255));
        lblDetalle.setText("Detalle:");

        txtDetalle.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtDetalleKeyPressed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Brush Script MT", 2, 36)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Bienvenido al Lobby");

        btnPartida.setFont(new java.awt.Font("SimSun-ExtG", 0, 12)); // NOI18N
        btnPartida.setText("Ingresar al juego");
        btnPartida.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPartidaActionPerformed(evt);
            }
        });

        comboReceptores.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "ALL" }));

        javax.swing.GroupLayout panelLobbyLayout = new javax.swing.GroupLayout(panelLobby);
        panelLobby.setLayout(panelLobbyLayout);
        panelLobbyLayout.setHorizontalGroup(
            panelLobbyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelLobbyLayout.createSequentialGroup()
                .addGroup(panelLobbyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelLobbyLayout.createSequentialGroup()
                        .addContainerGap(394, Short.MAX_VALUE)
                        .addComponent(jLabel1)
                        .addGap(40, 40, 40))
                    .addGroup(panelLobbyLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(panelLobbyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelLobbyLayout.createSequentialGroup()
                                .addComponent(btnPublico)
                                .addGap(62, 62, 62)
                                .addComponent(txtPublico, javax.swing.GroupLayout.PREFERRED_SIZE, 179, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(panelLobbyLayout.createSequentialGroup()
                                .addGroup(panelLobbyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblDetalle)
                                    .addComponent(lblPersona))
                                .addGap(48, 48, 48)
                                .addGroup(panelLobbyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(comboReceptores, 0, 195, Short.MAX_VALUE)
                                    .addComponent(txtDetalle)))
                            .addComponent(btnSendPrivate)
                            .addComponent(Desconectarse, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGroup(panelLobbyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 234, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnPartida))
                .addGap(32, 32, 32))
        );
        panelLobbyLayout.setVerticalGroup(
            panelLobbyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelLobbyLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(panelLobbyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(panelLobbyLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(58, 58, 58)
                        .addGroup(panelLobbyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnPublico)
                            .addComponent(txtPublico, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(50, 50, 50)
                        .addGroup(panelLobbyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(comboReceptores, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblPersona))
                        .addGap(18, 18, 18)
                        .addGroup(panelLobbyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblDetalle)
                            .addComponent(txtDetalle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(28, 28, 28)
                        .addComponent(btnSendPrivate)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(Desconectarse))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 333, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(btnPartida)
                .addContainerGap(32, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelLobby, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelLobby, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnPublicoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPublicoActionPerformed
        enviarMensajePublico();
    }//GEN-LAST:event_btnPublicoActionPerformed

    private void btnSendPrivateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSendPrivateActionPerformed
        enviarMensajePrivado();
    }//GEN-LAST:event_btnSendPrivateActionPerformed

    private void btnPartidaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPartidaActionPerformed
        if (!puedeIngresar) {
            JOptionPane.showMessageDialog(this, "No puedes ingresar a la partida en este momento. Espera a que se reinicie o comience una nueva.", "Ingreso no permitido", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Notificar al servidor que el jugador quiere unirse/está listo
        try {
            // El servidor manejará la transición y enviará la actualización de estado o un mensaje de inicio.
            // El cliente ya está en "espera" en el servidor desde que se conectó.
            // Si el servidor inicia el juego, enviará un mensaje que EscuchaServidorThread capturará.
            // Aquí, solo cambiamos la pantalla.
            // Mensaje msgListo = new Mensaje(nombreJugador, "LISTO_PARA_JUGAR", "SERVER", TipoMensaje.CONTROL);
            // salida.writeObject(msgListo);
            // salida.flush();
            
            areaChat.append("[Sistema] Esperando inicio de partida del servidor...\n");
            areaChat.setCaretPosition(areaChat.getDocument().getLength());

            // El cambio a ZonaJuego ahora se maneja centralmente cuando el servidor envía el mapa/inicio
            // a través de EscuchaServidorThread, que entonces creará ZonaJuego.
            // Aquí solo indicamos que estamos listos, o el servidor lo asume.
            // Por ahora, mantenemos el flujo de la versión B:
            ZonaJuego zona = new ZonaJuego();
            // nombreMapaActual es importante
            zona.initData(nombreJugador, salida, entrada, nombreMapaActual, escucha); 
            escucha.setReceptor(zona); // Cambiar receptor
            // escucha.reenviarJugadores(); // El EscuchaServidorThread ahora hace esto en setReceptor

            zona.setVisible(true);
            this.dispose();

        } catch (Exception e) { // Cambio de IOException a Exception más general
            areaChat.append("[Error] No se pudo notificar al servidor o cambiar de pantalla: " + e.getMessage() + "\n");
            areaChat.setCaretPosition(areaChat.getDocument().getLength());
        }
    }//GEN-LAST:event_btnPartidaActionPerformed

    private void DesconectarseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DesconectarseActionPerformed
        try {
            if (salida != null) {
                // Opcional: Enviar mensaje de desconexión voluntaria al servidor.
                // Mensaje msgDesconexion = new Mensaje(nombreJugador, "DESCONEXION_LOBBY", "SERVER", TipoMensaje.CONTROL);
                // salida.writeObject(msgDesconexion);
                // salida.flush();
                salida.close();
            }
            if (entrada != null) entrada.close();
            if (escucha != null) escucha.detener();
             if (reproductor != null) reproductor.detener();

        } catch (IOException e) {
            System.err.println("Error al cerrar streams al desconectar: " + e.getMessage());
        } finally {
            this.dispose(); // Cierra la ventana del lobby
            System.exit(0); // Termina la aplicación cliente
        }
    }//GEN-LAST:event_DesconectarseActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        DesconectarseActionPerformed(null); // Reutilizar la lógica del botón
    }//GEN-LAST:event_formWindowClosing

    private void txtPublicoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtPublicoKeyPressed
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            enviarMensajePublico();
        }
    }//GEN-LAST:event_txtPublicoKeyPressed

    private void txtDetalleKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtDetalleKeyPressed
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            enviarMensajePrivado();
        }
    }//GEN-LAST:event_txtDetalleKeyPressed

    private void enviarMensajePublico() {
        String texto = txtPublico.getText().trim();
        if (!texto.isBlank()) {
            try {
                Mensaje mensaje = new Mensaje(nombreJugador, texto, "ALL", TipoMensaje.PUBLICO);
                salida.writeObject(mensaje);
                salida.flush();
                txtPublico.setText("");
            } catch (IOException e) {
                areaChat.append("[Error] No se pudo enviar el mensaje público: " + e.getMessage() + "\n");
                areaChat.setCaretPosition(areaChat.getDocument().getLength());
            }
        }
    }

    private void enviarMensajePrivado() {
        String texto = txtDetalle.getText().trim();
        if (comboReceptores.getSelectedItem() == null) {
             areaChat.append("[Error] Ningún destinatario seleccionado.\n");
             areaChat.setCaretPosition(areaChat.getDocument().getLength());
             return;
        }
        String receptor = comboReceptores.getSelectedItem().toString();
        
        if (!texto.isBlank()){
            if (receptor.equals("ALL")) { // Si se selecciona "ALL", enviar como público
                txtPublico.setText(texto);
                enviarMensajePublico();
                txtDetalle.setText("");
                return;
            }
            try {
                Mensaje mensaje = new Mensaje(nombreJugador, texto, receptor, TipoMensaje.PRIVADO);
                salida.writeObject(mensaje);
                salida.flush();
                areaChat.append("[Privado a " + receptor + "] " + texto + "\n"); // Mostrar localmente el mensaje enviado
                areaChat.setCaretPosition(areaChat.getDocument().getLength());
                txtDetalle.setText(""); 
            } catch (IOException e) {
                areaChat.append("[Error] No se pudo enviar el mensaje privado: " + e.getMessage() + "\n");
                areaChat.setCaretPosition(areaChat.getDocument().getLength());
            }
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new PantallaLobby().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Desconectarse;
    public javax.swing.JTextArea areaChat;
    private javax.swing.JButton btnPartida;
    private javax.swing.JButton btnPublico;
    private javax.swing.JButton btnSendPrivate;
    private javax.swing.JComboBox<String> comboReceptores;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblDetalle;
    private javax.swing.JLabel lblPersona;
    private javax.swing.JPanel panelLobby;
    private javax.swing.JTextField txtDetalle;
    private javax.swing.JTextField txtPublico;
    // End of variables declaration//GEN-END:variables
}