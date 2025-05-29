package Cliente;

import Mapas.Mapas;
import Mapas.cargarMapas;
import Personajes.Jugador;
import Personajes.Zombie;
import Modelos.Mensaje;
import Modelos.TipoMensaje;
import Sonidos.ReproductorAudio;
import java.awt.Dimension;
import java.awt.Point; // Para las coordenadas del clic
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter; // NUEVO
import java.awt.event.MouseEvent;  // NUEVO
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections; 
import java.util.List;
import javax.swing.JLabel; // NUEVO para munición
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
    private Jugador miJugador; // NUEVO: para referencia rápida al jugador actual
    private JLabel lblMunicionDisplay; // NUEVO: para mostrar munición (variable de instancia)


    public ZonaJuego() {
        initComponents(); // Esto inicializa lblMunicion si lo añades en el initComponents
        reproductor.reproducir("/Sonidos/musica.wav");
        txtSms.setEditable(false);

        this.addKeyListener(this);
        this.setFocusable(true);

        // Si lblMunicionDisplay NO se añade a través del diseñador (initComponents):
        if (lblMunicionDisplay == null) { // Solo crear si no fue inicializado por el diseñador
            lblMunicionDisplay = new JLabel("Munición: N/A");
            lblMunicionDisplay.setForeground(java.awt.Color.YELLOW); 
            lblMunicionDisplay.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
            // Posicionarlo: jLayeredPane1 es el panel de fondo del JFrame por defecto si es el único.
            // Si jLayeredPane1 usa GroupLayout (default en NetBeans para el ContentPane),
            // añadirlo aquí programáticamente sin alterar el GroupLayout es complicado.
            // Asumimos que jLayeredPane1 tiene un layout que permite setBounds o lo añades a un subpanel.
            // Para un layout simple, lo añadimos sobre jPanel1 (el panel del mapa).
            // Esto requiere que jPanel1 no sea el contenedor directo del mapa con BorderLayout, sino que permita superposiciones.
            // O mejor, añádelo al jLayeredPane1 directamente.
            lblMunicionDisplay.setBounds(10, 10, 150, 20); // Coordenadas relativas a jLayeredPane1
            jLayeredPane1.add(lblMunicionDisplay, javax.swing.JLayeredPane.MODAL_LAYER); // Capa alta
        }
         lblMunicionDisplay.setVisible(false); // Ocultar inicialmente
    }
    
    public void cargarPanelMapa() {
        System.out.println("CLIENTE ZonaJuego: Cargando panel del mapa...");
        definicionMapa = cargarMapas.cargarMapaDesdeArchivo("mapa1.txt");
        panelMapaActual = new Mapas(definicionMapa);

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

        panelMapaActual.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMapClick(e);
            }
        });
    }
    
    private void handleMapClick(MouseEvent e) {
        if (miJugador != null && miJugador.esFrancotirador()) {
            if (miJugador.getMunicionFrancotirador() <= 0) {
                txtSms.append("[Sistema] Estás sin munición.\n");
                txtSms.setCaretPosition(txtSms.getDocument().getLength());
                return;
            }

            int tileX = e.getX() / 40; 
            int tileY = e.getY() / 40;

            if (tileX >= 0 && tileX < definicionMapa[0].length && tileY >= 0 && tileY < definicionMapa.length) {
                System.out.println("CLIENTE ZonaJuego: Francotirador " + nombreJugador + " apunta a (" + tileX + "," + tileY + ")");
                try {
                    Point objetivo = new Point(tileX, tileY);
                    Mensaje msgDisparo = new Mensaje(nombreJugador, objetivo, "SERVER", TipoMensaje.DISPARO_FRANCOTIRADOR);
                    salida.writeObject(msgDisparo);
                    salida.flush();
                } catch (IOException ex) {
                    txtSms.append("[Error] No se pudo enviar el disparo: " + ex.getMessage() + "\n");
                    txtSms.setCaretPosition(txtSms.getDocument().getLength());
                }
            }
        }
        this.requestFocusInWindow();
    }


    public void actualizarEstadoJuego(List<Jugador> nuevosJugadores, List<Zombie> nuevosZombies) {
        this.jugadores = new ArrayList<>(nuevosJugadores != null ? nuevosJugadores : Collections.emptyList());
        this.zombies = new ArrayList<>(nuevosZombies != null ? nuevosZombies : Collections.emptyList());
        
        boolean miJugadorEncontrado = false;
        for (Jugador j : this.jugadores) {
            if (j.getNombre().equals(this.nombreJugador)) {
                miJugador = j; 
                miJugadorEncontrado = true;
                if (j.esFrancotirador()) {
                    lblMunicionDisplay.setText("Balas: " + j.getMunicionFrancotirador());
                    lblMunicionDisplay.setVisible(true);
                } else {
                    lblMunicionDisplay.setVisible(false);
                }
                break;
            }
        }
        if (!miJugadorEncontrado) {
            // Si mi jugador ya no está en la lista (p.ej. murió y el servidor no lo envía)
            // O si el juego terminó y la lista de jugadores está vacía.
             if (miJugador != null) { // Si teníamos una referencia previa
                 if (!miJugador.esFrancotirador() && !miJugador.isVivo()) {
                     lblMunicionDisplay.setText("ELIMINADO");
                     lblMunicionDisplay.setForeground(java.awt.Color.RED);
                     lblMunicionDisplay.setVisible(true);
                 } else if (miJugador.esFrancotirador()) {
                     // Si era francotirador y ya no está en la lista, podría ser fin de juego
                     // o algo así. Mantenemos la última munición visible o un mensaje.
                     // lblMunicionDisplay.setText("Francotirador Inactivo");
                     // lblMunicionDisplay.setVisible(true);
                 } else {
                     lblMunicionDisplay.setVisible(false);
                 }
             } else { // No hay referencia a miJugador (inicio de juego o error)
                lblMunicionDisplay.setVisible(false);
             }
             miJugador = null; // Limpiar referencia si no se encontró
        }


        SwingUtilities.invokeLater(this::repaintMapa);
    }

    public void repaintMapa() {
        if (panelMapaActual == null) {
            System.out.println("CLIENTE ZonaJuego.repaintMapa: panelMapaActual es null, llamando a cargarPanelMapa.");
            cargarPanelMapa();
        }
        
        panelMapaActual.setJugadores(this.jugadores); 
        panelMapaActual.setZombies(this.zombies);

        jPanel1.revalidate();
        jPanel1.repaint();    
    }
    
    public void initData(String nombreJugador, ObjectOutputStream salida, ObjectInputStream entrada) {
        this.nombreJugador = nombreJugador;
        this.salida = salida;
        this.entrada = entrada;

        setTitle("Zona de juego de " + nombreJugador);
        if (lblMunicionDisplay != null) { // Asegurarse de que exista
             lblMunicionDisplay.setVisible(false);
        }


        cargarPanelMapa(); 
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
        });
    }
    
    @Override
    public void dispose() {
        reproductor.detener(); 
        super.dispose();       
    }
    
    public void actualizarListaJugadores(List<String> jugadoresNombres) {
        comboElegir.removeAllItems();
        comboElegir.addItem("ALL"); 

        for (String jugadorNombre : jugadoresNombres) {
            if (!jugadorNombre.equals(nombreJugador)) {
                comboElegir.addItem(jugadorNombre);
            }
        }
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
        // Si vas a añadir lblMunicionDisplay aquí desde el diseñador:
        // lblMunicionDisplay = new javax.swing.JLabel(); 

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                formKeyPressed(evt);
            }
        });

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
        btnEnviarPublico.setFocusable(false); // Para que no robe foco del JFrame para KeyListener
        btnEnviarPublico.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEnviarPublicoActionPerformed(evt);
            }
        });

        txtPublico.addFocusListener(new java.awt.event.FocusAdapter() { // Para cuando gana foco
            public void focusGained(java.awt.event.FocusEvent evt) {
                // Opcional: manejar algo cuando el campo de texto gana foco
            }
        });
         txtPublico.addKeyListener(new java.awt.event.KeyAdapter() { // Para Enter en txtPublico
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    btnEnviarPublicoActionPerformed(null); // Simula clic en botón
                }
            }
        });


        comboElegir.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "ALL" }));
        comboElegir.setFocusable(false);
        comboElegir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboElegirActionPerformed(evt);
            }
        });

        btnEnviarPrivado.setText("Enviar Mensaje Privado");
        btnEnviarPrivado.setFocusable(false);
        btnEnviarPrivado.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEnviarPrivadoActionPerformed(evt);
            }
        });
        
        txtPrivado.addKeyListener(new java.awt.event.KeyAdapter() { // Para Enter en txtPrivado
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    btnEnviarPrivadoActionPerformed(null); // Simula clic en botón
                }
            }
        });


        btnSalir.setText("Salir");
        btnSalir.setFocusable(false);
        btnSalir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSalirActionPerformed(evt);
            }
        });

        // Ejemplo de cómo se vería lblMunicionDisplay si se añade en el diseñador:
        // lblMunicionDisplay.setText("Munición: 10/10");
        // jLayeredPane1.setLayer(lblMunicionDisplay, javax.swing.JLayeredPane.MODAL_LAYER);
        // ... y luego añadirlo al GroupLayout ...

        jLayeredPane1.setLayer(jPanel1, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(jScrollPane1, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(btnEnviarPublico, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(txtPublico, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(comboElegir, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(btnEnviarPrivado, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(txtPrivado, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(btnSalir, javax.swing.JLayeredPane.DEFAULT_LAYER);
        // jLayeredPane1.setLayer(lblMunicionDisplay, javax.swing.JLayeredPane.MODAL_LAYER); // Si se añade aquí

        javax.swing.GroupLayout jLayeredPane1Layout = new javax.swing.GroupLayout(jLayeredPane1);
        jLayeredPane1.setLayout(jLayeredPane1Layout);
        jLayeredPane1Layout.setHorizontalGroup(
            jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jLayeredPane1Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
                        .addComponent(txtPublico)
                        .addComponent(btnEnviarPublico, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(comboElegir, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnEnviarPrivado, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                        .addComponent(txtPrivado))
                    .addComponent(btnSalir, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(20, Short.MAX_VALUE))
        );
        jLayeredPane1Layout.setVerticalGroup(
            jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jLayeredPane1Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jLayeredPane1Layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtPublico, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnEnviarPublico)
                        .addGap(18, 18, 18)
                        .addComponent(comboElegir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtPrivado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnEnviarPrivado)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnSalir)
                .addContainerGap(20, Short.MAX_VALUE))
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
                txtSms.setCaretPosition(txtSms.getDocument().getLength());
            }
        }
        this.requestFocusInWindow(); 
    }//GEN-LAST:event_btnEnviarPublicoActionPerformed

    private void btnEnviarPrivadoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEnviarPrivadoActionPerformed
        String texto = txtPrivado.getText().trim();
        Object selectedItem = comboElegir.getSelectedItem();
        
        if (selectedItem == null) {
            txtSms.append("[Error] Ningún destinatario seleccionado.\n");
            txtSms.setCaretPosition(txtSms.getDocument().getLength());
            this.requestFocusInWindow();
            return;
        }
        String receptor = selectedItem.toString();

        if (!texto.isBlank()) {
            try {
                TipoMensaje tipo = receptor.equals("ALL") ? TipoMensaje.PUBLICO : TipoMensaje.PRIVADO;
                Mensaje mensaje = new Mensaje(nombreJugador, texto, receptor, tipo);
                salida.writeObject(mensaje);
                
                 if (tipo == TipoMensaje.PRIVADO && !receptor.equals("ALL") && !receptor.equals(nombreJugador)) {
                     txtSms.append("[Privado a " + receptor + "] " + texto + "\n");
                 }
                txtPrivado.setText("");

            } catch (IOException e) {
                txtSms.append("[Error] No se pudo enviar el mensaje privado\n");
            }
             txtSms.setCaretPosition(txtSms.getDocument().getLength());
        }
        this.requestFocusInWindow(); 
    }//GEN-LAST:event_btnEnviarPrivadoActionPerformed

    private void comboElegirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboElegirActionPerformed
        this.requestFocusInWindow(); 
    }//GEN-LAST:event_comboElegirActionPerformed

    private void formKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyPressed
        // No es necesario llamar a keyPressed(evt) aquí si ya está registrado el KeyListener
        // en el constructor. De hecho, podría causar doble procesamiento.
        // Si este método fue generado por el diseñador de Netbeans al añadir un KeyListener
        // directamente al JFrame, y también lo añades programáticamente con `this.addKeyListener(this)`,
        // puedes eliminar la llamada aquí o el `this.addKeyListener(this)`.
        // Es más limpio manejarlo solo con `this.addKeyListener(this)`.
    }//GEN-LAST:event_formKeyPressed

    private void btnSalirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSalirActionPerformed
         try {
            if (salida != null) {
                // Opcional: Enviar un mensaje de FINALIZAR_JUEGO o similar para que el servidor sepa
                // Mensaje msgDesconexion = new Mensaje(nombreJugador, "DESCONECTADO_VOLUNTARIAMENTE", "SERVER", TipoMensaje.FINALIZAR_JUEGO);
                // salida.writeObject(msgDesconexion);
                // salida.flush();
                // El servidor ya maneja la desconexión a través de EOFException o SocketException en ThreadServidor.
            }
        } catch (Exception e) { 
            System.err.println("Error al intentar notificar desconexión (opcional): " + e.getMessage());
        } finally {
            this.dispose(); // Cierra la ventana, detiene la música, y debería terminar el hilo de EscuchaServidor por error de stream
            // Considerar si es necesario un System.exit(0) si la aplicación no cierra completamente
            // debido a otros hilos no demonio. Normalmente, cerrar la última ventana JFrame con
            // EXIT_ON_CLOSE debería terminar la JVM.
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
    private javax.swing.JTextField txtPrivado;
    private javax.swing.JTextField txtPublico;
    private javax.swing.JTextArea txtSms;
    // End of variables declaration//GEN-END:variables
    // Si lblMunicionDisplay fue añadido en el diseñador, también estaría aquí:
    // private javax.swing.JLabel lblMunicionDisplay;

    @Override
    public void recibirMensaje(Mensaje mensaje) {
        SwingUtilities.invokeLater(() -> {
            if (txtSms == null) {
                System.err.println("CLIENTE ZonaJuego.recibirMensaje: txtSms es null. Mensaje no mostrado: " + mensaje);
                return;
            }

            if (mensaje.getTipo() == TipoMensaje.PUBLICO) {
                txtSms.append(mensaje.getEnviador() + ": " + mensaje.getContenido().toString() + "\n");
            } else if (mensaje.getTipo() == TipoMensaje.PRIVADO) {
                 if (mensaje.getEnviador().equals("Servidor")) { // Mensajes del sistema/feedback
                     txtSms.append("[Sistema] " + mensaje.getContenido().toString() + "\n");
                 } else if (!mensaje.getEnviador().equals(nombreJugador)) { // No mostrar mis propios mensajes privados enviados
                     txtSms.append("[Privado de " + mensaje.getEnviador() + "] " + mensaje.getContenido().toString() + "\n");
                 }
            } else if (mensaje.getTipo() == TipoMensaje.FINALIZAR_JUEGO) {
                 txtSms.append("[Servidor] " + mensaje.getContenido().toString() + "\n");
                 txtPublico.setEnabled(false);
                 txtPrivado.setEnabled(false);
                 btnEnviarPrivado.setEnabled(false);
                 btnEnviarPublico.setEnabled(false);
                 // Podrías querer mostrar un mensaje de "Juego finalizado" más prominentemente
            }
             else if (mensaje.getContenido() != null) { 
                 String contenidoStr = mensaje.getContenido().toString();
                 if (contenidoStr.contains("Desconectado") || contenidoStr.contains("Conexión perdida") || contenidoStr.contains("Error de comunicación")) {
                    txtSms.append("[" + mensaje.getEnviador() + "] " + contenidoStr + "\n");
                 }
            }
            txtSms.setCaretPosition(txtSms.getDocument().getLength());
        });
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // No se usa
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (txtPublico.hasFocus() || txtPrivado.hasFocus()) {
            // El listener de Enter en los campos de texto ya maneja esto.
            return; 
        }

        if (miJugador != null && miJugador.esFrancotirador()) {
            return; // Francotiradores no se mueven con teclas
        }
        
        if (miJugador != null && !miJugador.isVivo()){
            return; // Jugadores muertos no se mueven
        }

        if (salida == null) {
            return;
        }

        String direccion = null;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:    direccion = "UP";    break;
            case KeyEvent.VK_DOWN:  direccion = "DOWN";  break;
            case KeyEvent.VK_LEFT:  direccion = "LEFT";  break;
            case KeyEvent.VK_RIGHT: direccion = "RIGHT"; break;
        }

        if (direccion != null) {
            try {
                Mensaje msgMovimiento = new Mensaje(nombreJugador, direccion, "SERVER", TipoMensaje.MOVER);
                salida.writeObject(msgMovimiento);
                salida.flush();
            } catch (IOException ex) {
                txtSms.append("[Error] No se pudo enviar el movimiento: " + ex.getMessage() + "\n");
                txtSms.setCaretPosition(txtSms.getDocument().getLength());
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // No se usa
    }
}