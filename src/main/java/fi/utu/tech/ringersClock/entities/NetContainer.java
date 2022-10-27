package fi.utu.tech.ringersClock.entities;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


public class NetContainer implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;

	private String lähettäjä;	
	private String vastaanottaja;	
	private Integer portti;
		
	/**
	 * Komento, joka siirretään asiakkaalta palvelimelle tai päinvastoin
	 */
	private String komento;
		
	/**
	 * Object-tyyppinen tiedonsiirrossa kuljetettava muuttuja, joka on tyypillisesti WakeUpGroup
	 */
	private Object sisältö;
	
	public String getKomento() {
		return this.komento;
	}
	
	public Object getSisältö() {
		return this.sisältö;
	}
	
	public NetContainer(String komento, Object objekti) {
		this.komento = komento;
		this.sisältö = objekti;
		
	}
	
	// Ei varmaan tarvita tällaista
//	public NetContainer(String komento, Object objekti, String lähettäjä, String vastaanottaja, Integer portti) {
//		this.komento = komento;
//		this.sisältö = objekti;
//		this.lähettäjä = lähettäjä;
//		this.vastaanottaja = vastaanottaja;
//		this.portti = portti;		
//	}


}
