/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Sonidos;

import java.io.IOException;
import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 *
 * @author jos_m
 */
public class ReproductorAudio {
    private Clip clip;

    public void reproducir(String ruta) {
        try {
            URL sonido = getClass().getResource(ruta);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(sonido);
            clip = AudioSystem.getClip();
            clip.open(audioIn);

            // Volumen bajo (opcional)
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                control.setValue(-15.0f); // volumen más bajo (0 = normal, -80 = silencio)
            }

            clip.loop(Clip.LOOP_CONTINUOUSLY); // reproducir en bucle
            clip.start();
        } catch (Exception e) {
            System.err.println("No se pudo reproducir el sonido: " + e.getMessage());
        }
    }

    public void detener() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
        }
    }
}
