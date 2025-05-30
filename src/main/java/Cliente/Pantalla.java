/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Cliente;

import Fondos.FondoPanel;
import Sonidos.ReproductorAudio;
import java.net.URL;
import javax.swing.JOptionPane;

/**
 *
 * @author jos_m
 */
public class Pantalla extends javax.swing.JFrame {
    private ClienteZombie cliente;
    private ReproductorAudio reproductor = new ReproductorAudio();
    /**
     * Creates new form Pantalla
     */
    public Pantalla() {
        initComponents();
        this.setSize(1000, 520);
        this.setLocationRelativeTo(null);

        // Fondo
        URL ruta = getClass().getResource("/Imagenes/zombiesFondo.jpg");
        if (ruta != null) {
            FondoPanel fondo = new FondoPanel(ruta);
            fondo.setBounds(0, 0, 1000, 520); // Ajustar al tamaño de la ventana
            fondo.setOpaque(false);
            jLayeredPane1.add(fondo, Integer.valueOf(0)); // Lo agregás al fondo (capa 0)
        } else {
            JOptionPane.showMessageDialog(this, "⚠ Imagen no encontrada");
        }

        // Asegurar que el panel con los botones esté encima (capa 1)
        jLayeredPane1.setLayer(panelJA, 1);

        // Hacer transparente el panel (opcional)
        panelJA.setOpaque(false);
        
        reproductor.reproducir("/Sonidos/musica.wav");
    }
    
    @Override
    public void dispose() {
        reproductor.detener(); // Detener la música antes de cerrar
        super.dispose();       // Cerrar la ventana normalmente
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLayeredPane1 = new javax.swing.JLayeredPane();
        panelJA = new javax.swing.JPanel();
        lblWelcome2 = new javax.swing.JLabel();
        btnEntrar2 = new javax.swing.JButton();
        txtNombre2 = new javax.swing.JTextField();
        lblNombre2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lblWelcome2.setFont(new java.awt.Font("Papyrus", 2, 24)); // NOI18N
        lblWelcome2.setForeground(new java.awt.Color(255, 255, 255));
        lblWelcome2.setText("Bienvenido a POOZombies");

        btnEntrar2.setText("Entrar");
        btnEntrar2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEntrar2ActionPerformed(evt);
            }
        });

        lblNombre2.setFont(new java.awt.Font("Garamond", 2, 24)); // NOI18N
        lblNombre2.setForeground(new java.awt.Color(255, 255, 255));
        lblNombre2.setText("Ingresa tu nombre");

        javax.swing.GroupLayout panelJALayout = new javax.swing.GroupLayout(panelJA);
        panelJA.setLayout(panelJALayout);
        panelJALayout.setHorizontalGroup(
            panelJALayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelJALayout.createSequentialGroup()
                .addContainerGap(226, Short.MAX_VALUE)
                .addGroup(panelJALayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblWelcome2)
                    .addGroup(panelJALayout.createSequentialGroup()
                        .addComponent(lblNombre2)
                        .addGap(58, 58, 58)
                        .addGroup(panelJALayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnEntrar2)
                            .addComponent(txtNombre2, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(299, 299, 299))
        );
        panelJALayout.setVerticalGroup(
            panelJALayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelJALayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblWelcome2)
                .addGap(95, 95, 95)
                .addGroup(panelJALayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtNombre2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblNombre2))
                .addGap(47, 47, 47)
                .addComponent(btnEntrar2)
                .addContainerGap(361, Short.MAX_VALUE))
        );

        jLayeredPane1.setLayer(panelJA, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout jLayeredPane1Layout = new javax.swing.GroupLayout(jLayeredPane1);
        jLayeredPane1.setLayout(jLayeredPane1Layout);
        jLayeredPane1Layout.setHorizontalGroup(
            jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelJA, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jLayeredPane1Layout.setVerticalGroup(
            jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jLayeredPane1Layout.createSequentialGroup()
                .addComponent(panelJA, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jLayeredPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnEntrar2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEntrar2ActionPerformed
        String nombre = txtNombre2.getText().trim();
        if (nombre.isBlank()) {
            JOptionPane.showMessageDialog(this, "Por favor, ingresa un nombre.");
            return;
        }

        cliente = new ClienteZombie(nombre); 
        this.dispose(); 
    }//GEN-LAST:event_btnEntrar2ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Pantalla.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Pantalla.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Pantalla.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Pantalla.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Pantalla().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnEntrar2;
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JLabel lblNombre2;
    private javax.swing.JLabel lblWelcome2;
    private javax.swing.JPanel panelJA;
    private javax.swing.JTextField txtNombre2;
    // End of variables declaration//GEN-END:variables
}
