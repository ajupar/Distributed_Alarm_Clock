package fi.utu.tech.ringersClock.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.UUID;

import fi.utu.tech.ringersClockServer.ExistingClientService;

/*
 * Entity class presenting a WakeUpGroup. The class is not complete.
 * You need to add some variables.
 * 
 * LISÄÄ MUUTTUJIA
 * 
 */

public class WakeUpGroup implements Serializable {

	private static final long serialVersionUID = 1L;
	private String name;
	private static Integer ID = 1;
	
	private String ryhmäID;
	private Integer hour;
	private Integer minutes;
	private boolean norain;
	private boolean temp;

	private String wakeUpTime;

//	private ArrayList<String> jäsenet;
	private CopyOnWriteArrayList<ExistingClientService> asiakasyhteydet;

	public WakeUpGroup(String name) {
		super();
		this.ryhmäID = UUID.randomUUID().toString();
		this.name = name;
	}

	public WakeUpGroup(String name, Integer hour, Integer minutes, boolean norain, boolean temp) {
		this.ryhmäID = UUID.randomUUID().toString();
		this.name = name;
		this.hour = hour;
		this.minutes = minutes;
		this.norain = norain;
		this.temp = temp;

		// Testattu JShellillä. Näin saadaan aika oikeaan muotoon, vaikka minuutit tai
		// tunnit olisivat yhdellä numerolla.
		// hh:mm
		this.wakeUpTime = String.format("%02d", hour) + ":" + String.format("%02d", minutes);
	}

	public WakeUpGroup(String name, Integer hour, Integer minutes, boolean norain, boolean temp, String wakeUpTime) {
		this.ryhmäID = UUID.randomUUID().toString();
		this.name = name;
		this.hour = hour;
		this.minutes = minutes;
		this.norain = norain;
		this.temp = temp;
		this.wakeUpTime = wakeUpTime;
	}
	
	public Integer getHour() {
		return this.hour;
	}
	
	public Integer getMinutes() {
		return this.minutes;
	}

	public boolean getRainDemand() {
		return this.norain;
	}

	public boolean getTemperatureDemand() {
		return this.temp;
	}

	public void setWakeUpTime(String wut) {
		this.wakeUpTime = wut;
	}

	public String getWakeUpTime() {
		return this.wakeUpTime;
	}

	public CopyOnWriteArrayList<ExistingClientService> getAsiakasyhteydet() {
		return this.asiakasyhteydet;
	}

	public void setAsiakasyhteydet(CopyOnWriteArrayList<ExistingClientService> cowal) {
		this.asiakasyhteydet = cowal;

	}

	public void herätäJäsenet() {

	}

	public void herätäJohtaja() {

	}

	public String getName() {
		return this.name;
	}

	public String getID() {
		return this.ryhmäID;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setID(String ID) {
		this.ryhmäID = ID;
	}

	@Override
	public String toString() {
		return this.getName();
	}

	@Override
	public boolean equals(Object o) {
		boolean equals = false;

		if (o instanceof WakeUpGroup) {
			if (this.getName().equals(((WakeUpGroup) o).getName())) { // oikeat sulkeet, jotta pakotettu tyyppimuunnos
																		// kohdistuu o:hon. tyyppimuunnoksen ansiosta
																		// voidaan suorittaa o:lle metodi getName()
				equals = true;
			}
		}

		return equals;
	}

}
