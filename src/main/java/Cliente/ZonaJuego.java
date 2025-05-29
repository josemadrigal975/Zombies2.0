
package Cliente;

import Mapas.Mapas;
import Mapas.cargarMapas;
import Personajes.Jugador;
import Personajes.Zombie;
import Modelos.Mensaje;
import Modelos.TipoMensaje;
import Sonidos.ReproductorAudio;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer; // Importar Timer para el cronómetro

public class ZonaJuego extends javax.swing.JFrame implements ReceptorMensajes, KeyListener {
    private String nombreJugador;
    private ReproductorAudio reproductor = new ReproductorAudio();
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private List<Jugador> jugadores = new ArrayList<>();
    private List<Zombie> zombies = new ArrayList<>();
    private Mapas panelMapaActual;
    private char[][] definicionMapa;
    private Jugador miJugador;
    private JLabel lblMunicionDisplay;
    
    // Nuevas variables de la versión B
    private boolean controlesActivos = true; // Por defecto activos al inicio
    private String nombreMapaActual;
    private long tiempoInicioPartidaServidor; // Para sincronizar con el servidor si es necesario
    private Timer cronometroSwing; // javax.swing.Timer para UI
    private EscuchaServidorThread escucha; // Referencia a EscuchaServidorThread

    public ZonaJuego() {
        initComponents();
        reproductor.reproducir("/Sonidos/musica.wav");
        txtSms.setEditable(false);

        this.addKeyListener(this);
        this.setFocusable(true);

        if (lblMunicionDisplay == null) {
            lblMunicionDisplay = new JLabel("Munición: N/A");
            lblMunicionDisplay.setForeground(java.awt.Color.YELLOW); 
            lblMunicionDisplay.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
            lblMunicionDisplay.setBounds(10, 10, 150, 20);
            jLayeredPane1.add(lblMunicionDisplay, javax.swing.JLayeredPane.MODAL_LAYER);
        }
        lblMunicionDisplay.setVisible(false);
        
        // Configurar cronómetro (pero no iniciarlo hasta que el juego realmente comience)
        lblTiempo.setText("00:00"); // Valor inicial para el label del tiempo
    }
    
    // Método para iniciar el cronómetro localmente
    public void iniciarCronometroLocal() {
        final long startTime = System.currentTimeMillis();
        if (cronometroSwing != null && cronometroSwing.isRunning()) {
            cronometroSwing.stop();
        }
        cronometroSwing = new Timer(1000, e -> {
            long elapsedTime = System.currentTimeMillis() - startTime;
            long totalSegundos = elapsedTime / 1000;
            long minutos = totalSegundos / 60;
            long segundos = totalSegundos % 60;
            lblTiempo.setText(String.format("%02d:%02d", minutos, segundos));
        });
        cronometroSwing.start();
    }

    // Método para actualizar el tiempo desde el servidor (si se implementa así)
    public void actualizarLabelTiempoConServidor(long totalSegundosServidor) {
        this.tiempoInicioPartidaServidor = totalSegundosServidor; // Podría usarse para recalcular o solo mostrar
        long minutos = totalSegundosServidor / 60;
        long segundos = totalSegundosServidor % 60;
        lblTiempo.setText(String.format("%02d:%02d", minutos, segundos));
    }
    
    public void cargarPanelMapa(String nombreArchivoMapa) {
        this.nombreMapaActual = nombreArchivoMapa; // Guardar nombre del mapa actual
        System.out.println("CLIENTE ZonaJuego: Cargando panel del mapa '" + nombreArchivoMapa + "'...");
        definicionMapa = cargarMapas.cargarMapaDesdeArchivo(nombreArchivoMapa); // Usar el nombre de archivo
        if (definicionMapa == null || definicionMapa.length == 0) {
            System.err.println("CLIENTE ZonaJuego: Error crítico, la definición del mapa es nula o vacía para " + nombreArchivoMapa);
            // Mostrar un mensaje de error o manejarlo
            txtSms.append("[Error Sistema] No se pudo cargar el mapa: " + nombreArchivoMapa + "\n");
            definicionMapa = new char[][]{{'X'}}; // Mapa de fallback
        }
        
        if (panelMapaActual != null) {
            jPanel1.remove(panelMapaActual); // Remover el panel de mapa anterior si existe
        }
        panelMapaActual = new Mapas(definicionMapa);

        Dimension dim = panelMapaActual.getPreferredSize();
        jPanel1.setLayout(null); 
        jPanel1.setPreferredSize(dim);
        jPanel1.setSize(dim);
        panelMapaActual.setBounds(0, 0, dim.width, dim.height);

        jPanel1.add(panelMapaActual);
        jPanel1.revalidate();
        jPanel1.repaint();
        System.out.println("CLIENTE ZonaJuego: Panel del mapa '" + nombreArchivoMapa + "' cargado y añadido.");

        // Re-agregar MouseListener si se crea un nuevo panelMapaActual
        panelMapaActual.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMapClick(e);
            }
        });
        // Solicitar foco de nuevo por si acaso
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }
    
    private void handleMapClick(MouseEvent e) {
        if (miJugador != null && miJugador.esFrancotirador() && controlesActivos) {
            if (miJugador.getMunicionFrancotirador() <= 0) {
                txtSms.append("[Sistema] Estás sin munición de francotirador.\n");
                txtSms.setCaretPosition(txtSms.getDocument().getLength());
                return;
            }
            int tileX = e.getX() / 40; 
            int tileY = e.getY() / 40;

            if (definicionMapa != null && tileX >= 0 && tileX < definicionMapa[0].length && tileY >= 0 && tileY < definicionMapa.length) {
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
                // Verificar si mi jugador murió o llegó a la meta según el servidor
                if (!j.isVivo() && !j.esFrancotirador()) { // Francotiradores no "mueren" por salud
                    lblMunicionDisplay.setText("ELIMINADO");
                    lblMunicionDisplay.setForeground(java.awt.Color.RED);
                    lblMunicionDisplay.setVisible(true);
                    desactivarControles(); // Jugador muerto no se mueve
                } else if (j.isLlegoMeta() && !j.esFrancotirador()){ // Francotirador no llega a meta
                    // El mensaje de "LLEGO_META" se maneja en EscuchaServidorThread
                    // Aquí solo actualizamos el estado visual si es necesario
                    lblMunicionDisplay.setText("EN SALIDA");
                    lblMunicionDisplay.setForeground(java.awt.Color.GREEN);
                    lblMunicionDisplay.setVisible(true);
                    desactivarControles();
                }
                break;
            }
        }
        if (!miJugadorEncontrado) {
             if (miJugador != null) { 
                 if (!miJugador.esFrancotirador()) { // Solo si no era francotirador y ya no está
                     lblMunicionDisplay.setText("ELIMINADO");
                     lblMunicionDisplay.setForeground(java.awt.Color.RED);
                     lblMunicionDisplay.setVisible(true);
                 } else { // Si era francotirador y no está, podría ser fin de juego
                     lblMunicionDisplay.setVisible(false); // Ocultar si ya no está
                 }
             } else { 
                lblMunicionDisplay.setVisible(false);
             }
             miJugador = null; 
             desactivarControles(); // Si mi jugador no está en la lista, no debería poder moverse
        }
        SwingUtilities.invokeLater(this::repaintMapa);
    }

    public void repaintMapa() {
        if (panelMapaActual == null) {
            System.out.println("CLIENTE ZonaJuego.repaintMapa: panelMapaActual es null. Intentando cargar el mapa: " + (nombreMapaActual != null ? nombreMapaActual : "mapa1.txt"));
            cargarPanelMapa(nombreMapaActual != null ? nombreMapaActual : "mapa1.txt"); // Usar el mapa actual o un default
            if (panelMapaActual == null) return; // Si sigue siendo null después del intento, salir.
        }
        panelMapaActual.setJugadores(this.jugadores); 
        panelMapaActual.setZombies(this.zombies);
        jPanel1.revalidate();
        jPanel1.repaint();    
    }
    
    public void initData(String nombreJugador, ObjectOutputStream salida, ObjectInputStream entrada, String nombreMapaInicial, EscuchaServidorThread escucha) {
        this.nombreJugador = nombreJugador;
        this.salida = salida;
        this.entrada = entrada;
        this.nombreMapaActual = nombreMapaInicial;
        this.escucha = escucha; // Guardar referencia

        setTitle("Zona de juego de " + nombreJugador + " - Mapa: " + nombreMapaActual);
        if (lblMunicionDisplay != null) {
             lblMunicionDisplay.setVisible(false); // Ocultar al inicio
        }

        cargarPanelMapa(nombreMapaActual); 
        pack(); 
        setLocationRelativeTo(null);
        setVisible(true); 

        // Solicitar foco después de que la ventana sea visible
        SwingUtilities.invokeLater(() -> {
            boolean focusObtenido = this.requestFocusInWindow();
            if (!focusObtenido) {
                System.err.println("CLIENTE ZonaJuego: ADVERTENCIA: ZonaJuego (JFrame) no pudo obtener el foco inicialmente.");
            } else {
                System.out.println("CLIENTE ZonaJuego: ZonaJuego (JFrame) obtuvo el foco inicial correctamente.");
            }
        });
        // El servidor enviará un mensaje CONTROL("INICIAR_JUEGO") o REINICIAR_JUEGO para empezar el cronómetro y activar controles.
    }
    
    @Override
    public void dispose() {
        reproductor.detener(); 
        if (cronometroSwing != null && cronometroSwing.isRunning()) {
            cronometroSwing.stop();
        }
        // No cerrar 'salida' ni 'entrada' aquí, se manejan a nivel de ClienteZombie o al cerrar la aplicación.
        super.dispose();       
    }

    // Métodos de control de la versión B
    public void desactivarControles() {
        this.controlesActivos = false;
        System.out.println("CLIENTE ZonaJuego: Controles DESACTIVADOS para " + nombreJugador);
    }

    public void activarControles() {
        this.controlesActivos = true;
        System.out.println("CLIENTE ZonaJuego: Controles ACTIVADOS para " + nombreJugador);
        // Asegurar que la ventana tenga el foco para recibir KeyEvents
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }
    
    // Llamado por EscuchaServidorThread cuando llega CONTROL("INICIAR_JUEGO") o REINICIAR_JUEGO
    public void iniciarJuego() {
        System.out.println("CLIENTE ZonaJuego: Iniciando juego/nivel para " + nombreJugador);
        activarControles();
        iniciarCronometroLocal();
        // Actualizar UI si es necesario, como el nombre del mapa
        setTitle("Zona de juego de " + nombreJugador + " - Mapa: " + nombreMapaActual);
    }
    
    public String getNombreJugador() {
        return nombreJugador;
    }
    
    public void actualizarListaJugadores(List<String> jugadoresNombres) {
        // Asegurarse que el modelo del ComboBox se crea en el EDT
        SwingUtilities.invokeLater(() -> {
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("ALL");

            for (String jugadorNombre : jugadoresNombres) {
                if (!jugadorNombre.trim().equalsIgnoreCase(nombreJugador.trim())) {
                    model.addElement(jugadorNombre);
                }
            }
            comboElegir.setModel(model);
        });
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
        btnSalirLobby = new javax.swing.JButton();
        lblTiempo = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
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
        btnEnviarPublico.setFocusable(false);
        btnEnviarPublico.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEnviarPublicoActionPerformed(evt);
            }
        });

        txtPublico.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtPublicoKeyPressed(evt);
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

        txtPrivado.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtPrivadoKeyPressed(evt);
            }
        });

        btnSalirLobby.setText("Salir al Lobby");
        btnSalirLobby.setFocusable(false);
        btnSalirLobby.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSalirLobbyActionPerformed(evt);
            }
        });

        lblTiempo.setFont(new java.awt.Font("Papyrus", 3, 24)); // NOI18N
        lblTiempo.setText("00:00");
        lblTiempo.setToolTipText("Tiempo de partida");

        jLayeredPane1.setLayer(jPanel1, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(jScrollPane1, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(btnEnviarPublico, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(txtPublico, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(comboElegir, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(btnEnviarPrivado, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(txtPrivado, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(btnSalirLobby, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(lblTiempo, javax.swing.JLayeredPane.DEFAULT_LAYER);

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
                    .addComponent(btnSalirLobby, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblTiempo, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(20, Short.MAX_VALUE))
        );
        jLayeredPane1Layout.setVerticalGroup(
            jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jLayeredPane1Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jLayeredPane1Layout.createSequentialGroup()
                        .addComponent(lblTiempo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtPublico, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnEnviarPublico)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(comboElegir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtPrivado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnEnviarPrivado)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnSalirLobby))
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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
        enviarMensajePublicoLogic();
        this.requestFocusInWindow(); 
    }//GEN-LAST:event_btnEnviarPublicoActionPerformed

    private void btnEnviarPrivadoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEnviarPrivadoActionPerformed
        enviarMensajePrivadoLogic();
        this.requestFocusInWindow(); 
    }//GEN-LAST:event_btnEnviarPrivadoActionPerformed

    private void comboElegirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboElegirActionPerformed
        this.requestFocusInWindow(); 
    }//GEN-LAST:event_comboElegirActionPerformed

    private void btnSalirLobbyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSalirLobbyActionPerformed
        try {
            if (salida != null) {
                Mensaje mensaje = new Mensaje(nombreJugador, "SALIR_PARTIDA", "SERVER", TipoMensaje.CONTROL);
                salida.writeObject(mensaje);
                salida.flush();
                desactivarControles(); // Jugador ya no participa
            }
            // La transición al lobby ahora se maneja por el mensaje VOLVER_LOBBY del servidor
            // que es procesado por EscuchaServidorThread.
            // Aquí solo notificamos al servidor.
            txtSms.append("[Sistema] Solicitado salir al lobby. Esperando confirmación del servidor...\n");
            txtSms.setCaretPosition(txtSms.getDocument().getLength());

        } catch (IOException e) {
            txtSms.append("[Error] No se pudo enviar la solicitud de salida: " + e.getMessage() + "\n");
            txtSms.setCaretPosition(txtSms.getDocument().getLength());
        }
        this.requestFocusInWindow();
    }//GEN-LAST:event_btnSalirLobbyActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        btnSalirLobbyActionPerformed(null); // Reutilizar la lógica para salir al lobby.
                                        // Si se quiere cerrar toda la app, se necesita otra lógica.
    }//GEN-LAST:event_formWindowClosing

    private void txtPublicoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtPublicoKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            enviarMensajePublicoLogic();
            this.requestFocusInWindow();
        }
    }//GEN-LAST:event_txtPublicoKeyPressed

    private void txtPrivadoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtPrivadoKeyPressed
         if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            enviarMensajePrivadoLogic();
            this.requestFocusInWindow();
        }
    }//GEN-LAST:event_txtPrivadoKeyPressed
    
    private void enviarMensajePublicoLogic() {
        String texto = txtPublico.getText().trim();
        if (!texto.isBlank()) {
            try {
                Mensaje mensaje = new Mensaje(nombreJugador, texto, "ALL", TipoMensaje.PUBLICO);
                salida.writeObject(mensaje);
                salida.flush();
                txtPublico.setText("");
            } catch (IOException e) {
                txtSms.append("[Error] No se pudo enviar el mensaje público: " + e.getMessage() + "\n");
                txtSms.setCaretPosition(txtSms.getDocument().getLength());
            }
        }
    }

    private void enviarMensajePrivadoLogic() {
        String texto = txtPrivado.getText().trim();
        Object selectedItem = comboElegir.getSelectedItem();
        
        if (selectedItem == null) {
            txtSms.append("[Error] Ningún destinatario seleccionado.\n");
            txtSms.setCaretPosition(txtSms.getDocument().getLength());
            return;
        }
        String receptor = selectedItem.toString();

        if (!texto.isBlank()) {
             if (receptor.equals("ALL")) { // Si se selecciona "ALL", enviar como público
                txtPublico.setText(texto);
                enviarMensajePublicoLogic();
                txtPrivado.setText("");
                return;
            }
            try {
                Mensaje mensaje = new Mensaje(nombreJugador, texto, receptor, TipoMensaje.PRIVADO);
                salida.writeObject(mensaje);
                salida.flush();
                txtSms.append("[Privado a " + receptor + "] " + texto + "\n");
                txtSms.setCaretPosition(txtSms.getDocument().getLength());
                txtPrivado.setText("");
            } catch (IOException e) {
                txtSms.append("[Error] No se pudo enviar el mensaje privado: " + e.getMessage() + "\n");
                txtSms.setCaretPosition(txtSms.getDocument().getLength());
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnEnviarPrivado;
    private javax.swing.JButton btnEnviarPublico;
    private javax.swing.JButton btnSalirLobby;
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
        // Este método ahora solo recibe mensajes de chat o del sistema para ZonaJuego.
        // Las actualizaciones de estado del juego, jugadores, etc., las maneja EscuchaServidorThread
        // y llama a los métodos específicos de esta clase si es necesario.
        SwingUtilities.invokeLater(() -> {
            if (txtSms == null) {
                System.err.println("CLIENTE ZonaJuego.recibirMensaje: txtSms es null. Mensaje no mostrado: " + mensaje);
                return;
            }

            if (mensaje.getTipo() == TipoMensaje.PUBLICO) {
                txtSms.append(mensaje.getEnviador() + ": " + mensaje.getContenido().toString() + "\n");
            } else if (mensaje.getTipo() == TipoMensaje.PRIVADO) {
                 if (mensaje.getEnviador().equals("Servidor") || mensaje.getReceptor().equals("LOCAL")) { 
                     txtSms.append("[Sistema] " + mensaje.getContenido().toString() + "\n");
                 } else if (!mensaje.getEnviador().equals(nombreJugador)) { 
                     txtSms.append("[Privado de " + mensaje.getEnviador() + "] " + mensaje.getContenido().toString() + "\n");
                 }
            } else if (mensaje.getTipo() == TipoMensaje.FINALIZAR_JUEGO) {
                 txtSms.append("[Servidor] " + mensaje.getContenido().toString() + "\n");
                 desactivarControles();
                 // Otras acciones como deshabilitar botones de chat si es necesario
                 btnEnviarPrivado.setEnabled(false);
                 btnEnviarPublico.setEnabled(false);
                 txtPrivado.setEnabled(false);
                 txtPublico.setEnabled(false);
            } else if (mensaje.getContenido() != null) { 
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
            // El KeyListener en los JTextField maneja Enter.
            return; 
        }

        if (!controlesActivos) {
            System.out.println("CLIENTE ZonaJuego: Teclas desactivadas (controlesActivos=false), no se enviará movimiento.");
            return;
        }

        if (miJugador != null && miJugador.esFrancotirador()) {
            // Francotiradores no se mueven con teclas, usan clic.
            return; 
        }
        
        if (miJugador != null && !miJugador.isVivo()){
            System.out.println("CLIENTE ZonaJuego: Jugador muerto, no se puede mover.");
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

        if (direccion != null) {
            // System.out.println("CLIENTE ZonaJuego: Intentando enviar movimiento: " + direccion + " para " + nombreJugador);
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