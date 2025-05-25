/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Mapas;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jos_m
 */
public class cargarMapas {
    public static char[][] cargarMapaDesdeArchivo(String nombreArchivo) {
        List<char[]> filas = new ArrayList<>();

        try {
            // Cargar desde el classpath (src/main/resources/Mapas/)
            InputStream is = cargarMapas.class.getResourceAsStream("/Mapas/" + nombreArchivo);

            // Validación por si no se encuentra el archivo
            if (is == null) {
                System.err.println("⚠️ No se encontró el archivo: /Mapas/" + nombreArchivo);
                return new char[0][];
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String linea;
            while ((linea = br.readLine()) != null) {
                filas.add(linea.toCharArray());
            }
            br.close();
        } catch (IOException e) {
            System.err.println("❌ Error leyendo el mapa: " + e.getMessage());
            return new char[0][];
        }

        return filas.toArray(new char[0][]);
    }
}
