package Cliente;

import Mapas.Mapas;
import Mapas.cargarMapas;
import Personajes.Jugador;
import Personajes.Zombie;
import Modelos.Mensaje;
import Modelos.TipoMensaje;
import Sonidos.ReproductorAudio;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections; // Para Collections.emptyList()
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.SwingUtilities;

public class ZonaJuego extends javax.swing.JFrame implements ReceptorMensajes, KeyListener {
    private String nombreJugador;
    private ReproductorAudio reproductor = new ReproductorAudio();
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private List<Jugador> jugadores = new ArrayList<>();
    private List<Zombie> zombies = new ArrayList<>();
    private Mapas panelMapaActual;
    private char[][] definicionMapa;
    private boolean controlesActivos = true;
    private String nombreMapaActual;
    private long tiempoInicio;
    private javax.swing.Timer timer;
    private EscuchaServidorThread escucha;

    

    public ZonaJuego() {
        initComponents();
        reproductor.reproducir("/Sonidos/musica.wav");
        txtSms.setEditable(false);

        this.addKeyListener(this);
        this.setFocusable(true);
        // No llamar a requestFocusInWindow() aquí, hacerlo en initData después de setVisible(true)
    }
    
    public void iniciarCronometro() {
        tiempoInicio = System.currentTimeMillis();
        timer = new javax.swing.Timer(1000, e -> actualizarLabelTiempo());
        timer.start();
    }
    
        private void actualizarLabelTiempo() {
        long tiempoActual = System.currentTimeMillis();
        long totalSegundos = (tiempoActual - tiempoInicio) / 1000;

        long minutos = totalSegundos / 60;
        long segundos = totalSegundos % 60;

        String tiempoFormateado = String.format("%02d:%02d", minutos, segundos);
        lblTiempo.setText(tiempoFormateado);
    }


    
    public void cargarPanelMapa(String nombreArchivoMapa) {
        System.out.println("CLIENTE ZonaJuego: Cargando panel del mapa...");
        this.definicionMapa = cargarMapas.cargarMapaDesdeArchivo(nombreArchivoMapa);
        this.panelMapaActual = new Mapas(definicionMapa);

        Dimension dim = panelMapaActual.getPreferredSize();
        jPanel1.setLayout(null);
        jPanel1.setPreferredSize(dim);
        jPanel1.setSize(dim);

        panelMapaActual.setBounds(0, 0, dim.width, dim.height);

        jPanel1.removeAll();
        jPanel1.add(panelMapaActual);
        jPanel1.revalidate();
        jPanel1.repaint();
        System.out.println("CLIENTE ZonaJuego: Panel del mapa cargado y añadido.");
    }
    
    // setJugadoresInterno y setZombiesInterno ya no son necesarios con la nueva forma de actualizar listas.

    public void actualizarEstadoJuego(List<Jugador> nuevosJugadores, List<Zombie> nuevosZombies) {
        System.out.println("CLIENTE ZonaJuego.actualizarEstadoJuego - Jugadores recibidos: " + (nuevosJugadores != null ? nuevosJugadores.size() : "null") + 
                           ", Zombies: " + (nuevosZombies != null ? nuevosZombies.size() : "null"));
        if (nuevosJugadores != null && !nuevosJugadores.isEmpty()) {
            Jugador j = nuevosJugadores.get(0); // Asumimos que al menos hay un jugador si la lista no es vacía
            // Solo loguear si el jugador es el propio cliente para no spamear por otros
            if (j.getNombre().equals(this.nombreJugador)) {
                System.out.println("CLIENTE ZonaJuego.actualizarEstadoJuego - Mi jugador ("+j.getNombre()+") en DTO: (" + j.getX() + "," + j.getY() + "), Salud: " + j.getSalud());
            }
        }

        // Reemplazar las listas internas con nuevas copias de las listas recibidas
        this.jugadores = new ArrayList<>(nuevosJugadores != null ? nuevosJugadores : Collections.emptyList());
        this.zombies = new ArrayList<>(nuevosZombies != null ? nuevosZombies : Collections.emptyList());
        
        // System.out.println("CLIENTE ZonaJuego.actualizarEstadoJuego - this.jugadores actualizado, size: " + this.jugadores.size());
        // if (!this.jugadores.isEmpty() && this.jugadores.get(0).getNombre().equals(this.nombreJugador)){
        //     System.out.println("CLIENTE ZonaJuego.actualizarEstadoJuego - Mi jugador en this.jugadores: " + this.jugadores.get(0).getNombre() + 
        //                        " en (" + this.jugadores.get(0).getX() + "," + this.jugadores.get(0).getY() + ")");
        // }

        SwingUtilities.invokeLater(this::repaintMapa);
    }

    public void repaintMapa() {
        if (panelMapaActual == null) {
            System.out.println("CLIENTE ZonaJuego.repaintMapa: panelMapaActual es null, llamando a cargarPanelMapa.");
            return;
        }
        
        // System.out.println("CLIENTE ZonaJuego.repaintMapa: Pasando jugadores (" + this.jugadores.size() + ") y zombies (" + this.zombies.size() + ") al panel del mapa.");
        // if (!this.jugadores.isEmpty() && this.jugadores.get(0).getNombre().equals(this.nombreJugador)){
        //     System.out.println("CLIENTE ZonaJuego.repaintMapa - Mi jugador en this.jugadores ANTES de setJugadores: " + this.jugadores.get(0).getNombre() + 
        //                        " en (" + this.jugadores.get(0).getX() + "," + this.jugadores.get(0).getY() + ")");
        // }

        panelMapaActual.setJugadores(this.jugadores); 
        panelMapaActual.setZombies(this.zombies);

        // System.out.println("CLIENTE ZonaJuego.repaintMapa: Llamando a revalidate y repaint en jPanel1.");
        jPanel1.revalidate();
        jPanel1.repaint();    
    }
    
    public void initData(String nombreJugador, ObjectOutputStream salida, ObjectInputStream entrada, String nombreMapa, EscuchaServidorThread escucha) {
        this.nombreJugador = nombreJugador;
        this.salida = salida;
        this.entrada = entrada;
        this.nombreMapaActual = nombreMapa;
        this.escucha = escucha;

        setTitle("Zona de juego de " + nombreJugador);

        cargarPanelMapa(nombreMapa);
        pack(); 
        setLocationRelativeTo(null);
        setVisible(true); 

        SwingUtilities.invokeLater(() -> {
            boolean focusObtenido = this.requestFocusInWindow();
            if (!focusObtenido) {
                System.err.println("CLIENTE ZonaJuego: ADVERTENCIA: ZonaJuego (JFrame) no pudo obtener el foco inicialmente.");
            } else {
                System.out.println("CLIENTE ZonaJuego: ZonaJuego (JFrame) obtuvo el foco inicial correctamente.");
            }
            // Adicionalmente, verificar si el panel principal tiene el foco si es necesario,
            // aunque el KeyListener está en el JFrame.
            // System.out.println("CLIENTE ZonaJuego: Foco en JFrame después de invokeLater: " + this.isFocusOwner());
            // System.out.println("CLIENTE ZonaJuego: Componente con foco: " + (getFocusOwner() != null ? getFocusOwner().getClass().getSimpleName() : "null"));
        });
    }
    
    @Override
    public void dispose() {
        reproductor.detener(); 
        super.dispose();       
    }
    
    public void desactivarControles() {
    this.controlesActivos = false;
    }

    public void activarControles() {
        this.controlesActivos = true;
    }
    
    public void actualizarTiempoPartida(long segundos) {
        lblTiempo.setText("Tiempo: " + segundos + "s");
    }


    
    public void actualizarListaJugadores(List<String> jugadoresNombres) {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("ALL");

        for (String jugadorNombre : jugadoresNombres) {
            if (!jugadorNombre.trim().equalsIgnoreCase(nombreJugador.trim())) {
                model.addElement(jugadorNombre);
            }
        }
        comboElegir.setModel(model);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLayeredPane1 = new javax.swing.JLayeredPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtSms = new javax.swing.JTextArea();
        btnEnviarPublico = new javax.swing.JButton();
        txtPublico = new javax.swing.JTextField();
        comboElegir = new javax.swing.JComboBox<>();
        btnEnviarPrivado = new javax.swing.JButton();
        txtPrivado = new javax.swing.JTextField();
        btnSalir = new javax.swing.JButton();
        lblTiempo = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 344, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 313, Short.MAX_VALUE)
        );

        txtSms.setColumns(20);
        txtSms.setRows(5);
        jScrollPane1.setViewportView(txtSms);

        btnEnviarPublico.setText("Enviar Mensaje Público");
        btnEnviarPublico.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEnviarPublicoActionPerformed(evt);
            }
        });

        comboElegir.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboElegir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboElegirActionPerformed(evt);
            }
        });

        btnEnviarPrivado.setText("Enviar Mensaje Privado");
        btnEnviarPrivado.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEnviarPrivadoActionPerformed(evt);
            }
        });

        btnSalir.setText("Salir");
        btnSalir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSalirActionPerformed(evt);
            }
        });

        lblTiempo.setFont(new java.awt.Font("Papyrus", 3, 24)); // NOI18N
        lblTiempo.setText("00:00");

        jLayeredPane1.setLayer(jPanel1, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(jScrollPane1, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(btnEnviarPublico, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(txtPublico, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(comboElegir, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(btnEnviarPrivado, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(txtPrivado, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(btnSalir, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(lblTiempo, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout jLayeredPane1Layout = new javax.swing.GroupLayout(jLayeredPane1);
        jLayeredPane1.setLayout(jLayeredPane1Layout);
        jLayeredPane1Layout.setHorizontalGroup(
            jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jLayeredPane1Layout.createSequentialGroup()
                .addGap(66, 66, 66)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(comboElegir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnEnviarPrivado, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(txtPrivado))
                    .addGroup(jLayeredPane1Layout.createSequentialGroup()
                        .addComponent(btnSalir)
                        .addGap(80, 80, 80))
                    .addComponent(lblTiempo, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 18, Short.MAX_VALUE)
                .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jScrollPane1)
                        .addComponent(txtPublico))
                    .addComponent(btnEnviarPublico))
                .addGap(25, 25, 25))
        );
        jLayeredPane1Layout.setVerticalGroup(
            jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jLayeredPane1Layout.createSequentialGroup()
                .addGap(37, 37, 37)
                .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jLayeredPane1Layout.createSequentialGroup()
                        .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(jLayeredPane1Layout.createSequentialGroup()
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(33, 33, 33))
                            .addGroup(jLayeredPane1Layout.createSequentialGroup()
                                .addGap(11, 11, 11)
                                .addComponent(lblTiempo, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnSalir)
                                .addGap(18, 18, 18)
                                .addComponent(comboElegir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(15, 15, 15)))
                        .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtPrivado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtPublico, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(19, 19, 19)
                        .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnEnviarPrivado)
                            .addComponent(btnEnviarPublico))))
                .addContainerGap(46, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnEnviarPublicoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEnviarPublicoActionPerformed
        String texto = txtPublico.getText().trim();
        if (!texto.isBlank()) {
            try {
                Mensaje mensaje = new Mensaje(nombreJugador, texto, "ALL", TipoMensaje.PUBLICO);
                salida.writeObject(mensaje);
                txtPublico.setText("");
            } catch (IOException e) {
                txtSms.append("[Error] No se pudo enviar el mensaje público\n");
            }
        }
        this.requestFocusInWindow(); 
    }//GEN-LAST:event_btnEnviarPublicoActionPerformed

    private void btnEnviarPrivadoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEnviarPrivadoActionPerformed
        String texto = txtPrivado.getText().trim();
        Object selectedItem = comboElegir.getSelectedItem();
        
        if (selectedItem == null) {
            txtSms.append("[Error] Ningún destinatario seleccionado.\n");
            this.requestFocusInWindow();
            return;
        }
        String receptor = selectedItem.toString();

        if (!texto.isBlank()) {
            try {
                TipoMensaje tipo = receptor.equals("ALL") ? TipoMensaje.PUBLICO : TipoMensaje.PRIVADO;
                Mensaje mensaje = new Mensaje(nombreJugador, texto, receptor, tipo);
                salida.writeObject(mensaje);

                // Mostrar mensaje local
                if (tipo == TipoMensaje.PRIVADO) {
                    txtSms.append("[Yo → " + receptor + "] " + texto + "\n");
                }

                txtPrivado.setText("");
            } catch (IOException e) {
                txtSms.append("[Error] No se pudo enviar el mensaje privado\n");
            }
        }
        this.requestFocusInWindow(); 
    }//GEN-LAST:event_btnEnviarPrivadoActionPerformed

    private void comboElegirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboElegirActionPerformed
        this.requestFocusInWindow(); 
    }//GEN-LAST:event_comboElegirActionPerformed

    private void formKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyPressed
        keyPressed(evt); 
    }//GEN-LAST:event_formKeyPressed

    private void btnSalirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSalirActionPerformed
        try {
            // Notificar al servidor
            Mensaje mensaje = new Mensaje(nombreJugador, "SALIR_PARTIDA", "SERVIDOR", TipoMensaje.CONTROL);
            salida.writeObject(mensaje);
            salida.flush();

            // Cerrar juego y volver a lobby
            this.dispose();
            PantallaLobby lobby = new PantallaLobby();
            lobby.initData(nombreJugador, salida, entrada, escucha);
            lobby.setVisible(true); 
            escucha.setReceptor(lobby);
            lobby.bloquearIngreso();

        } catch (IOException e) {
            System.err.println("Error al salir del juego: " + e.getMessage());
        }
    }//GEN-LAST:event_btnSalirActionPerformed

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ZonaJuego().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnEnviarPrivado;
    private javax.swing.JButton btnEnviarPublico;
    private javax.swing.JButton btnSalir;
    private javax.swing.JComboBox<String> comboElegir;
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblTiempo;
    private javax.swing.JTextField txtPrivado;
    private javax.swing.JTextField txtPublico;
    private javax.swing.JTextArea txtSms;
    // End of variables declaration//GEN-END:variables

    @Override
    public void recibirMensaje(Mensaje mensaje) {
        SwingUtilities.invokeLater(() -> {
            if (mensaje.getTipo() == TipoMensaje.PUBLICO) {
                txtSms.append(mensaje.getEnviador() + ": " + mensaje.getContenido().toString() + "\n");
            } else if (mensaje.getTipo() == TipoMensaje.PRIVADO) {
                 txtSms.append("[Privado de " + mensaje.getEnviador() + "] " + mensaje.getContenido().toString() + "\n");
            } else if (mensaje.getContenido() != null) {
                 String contenidoStr = mensaje.getContenido().toString();
                 if (contenidoStr.contains("Desconectado") || contenidoStr.contains("Conexión perdida") || contenidoStr.contains("Error de comunicación")) {
                    txtSms.append("[" + mensaje.getEnviador() + "] " + contenidoStr + "\n");
                 }
            }
        });
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // No se usa
    }

    @Override
    public void keyPressed(KeyEvent e) {
        System.out.println("CLIENTE ZonaJuego keyPressed: " + KeyEvent.getKeyText(e.getKeyCode()) + 
                           ", Foco en JFrame: " + this.isFocusOwner() +
                           ", Componente con foco: " + (getFocusOwner() != null ? getFocusOwner().getClass().getSimpleName() : "null"));

        if (txtPublico.hasFocus() || txtPrivado.hasFocus()) {
            System.out.println("CLIENTE ZonaJuego: Foco en campo de texto, ignorando tecla de juego: " + KeyEvent.getKeyText(e.getKeyCode()));
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                if (txtPublico.hasFocus()) {
                    btnEnviarPublico.doClick();
                } else if (txtPrivado.hasFocus()) {
                    btnEnviarPrivado.doClick();
                }
            }
            return; 
        }

        if (salida == null) {
            System.out.println("CLIENTE ZonaJuego: Salida es null, no se puede enviar movimiento.");
            return;
        }

        String direccion = null;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:    direccion = "UP";    break;
            case KeyEvent.VK_DOWN:  direccion = "DOWN";  break;
            case KeyEvent.VK_LEFT:  direccion = "LEFT";  break;
            case KeyEvent.VK_RIGHT: direccion = "RIGHT"; break;
        }

        if (!controlesActivos) {
        System.out.println("CLIENTE ZonaJuego: Teclas desactivadas, no se enviará movimiento.");
        return;
        }

        if (direccion != null) {
            System.out.println("CLIENTE ZonaJuego: Intentando enviar movimiento: " + direccion + " para " + nombreJugador);
            try {
                Mensaje msgMovimiento = new Mensaje(nombreJugador, direccion, "SERVER", TipoMensaje.MOVER);
                salida.writeObject(msgMovimiento);
                salida.flush();
            } catch (IOException ex) {
                txtSms.append("[Error] No se pudo enviar el movimiento: " + ex.getMessage() + "\n");
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // No se usa
    }
    
    public void iniciarJuego() {
        controlesActivos = true;
        if (timer != null) timer.stop(); // por si venías de otro nivel
        iniciarCronometro();
    }
    
    public String getNombreJugador() {
        return nombreJugador;
    }


}