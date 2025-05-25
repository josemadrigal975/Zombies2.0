/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Modelos;

import java.io.Serializable;

/**
 *
 * @author jos_m
 */
public class Mensaje implements Serializable{
    private String enviador;
    private Object contenido;
    private String receptor;
    private TipoMensaje tipo;

    public Mensaje(String enviador, Object contenido, String receptor, TipoMensaje tipo) {
        this.enviador = enviador;
        this.contenido = contenido;
        this.receptor = receptor;
        this.tipo = tipo;
    }

    public String getEnviador() { return enviador; }
    public Object getContenido() { return contenido; }
    public String getReceptor() { return receptor; }
    public TipoMensaje getTipo() { return tipo; }

    @Override
    public String toString() {
        return "[" + tipo + "] " + enviador + " -> " + receptor + ": " + contenido;
    }
}
