package fi.utu.tech.ringersClock;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.time.Instant;
import java.util.concurrent.CopyOnWriteArrayList;

import fi.utu.tech.ringersClock.entities.NetContainer;
import fi.utu.tech.ringersClock.entities.WakeUpGroup;
import fi.utu.tech.ringersClockServer.ExistingClientService;
import fi.utu.tech.ringersClockServer.ServerSocketListener;

/*
 * A class for handling network related stuff
 * 
 * "Network unit"
 * 
 * Kommunikoi GUI_IO:n ja ServerSocketListenerin välissä
 * 
 * Asiakas (ClockClient) aloittaa aina yhteyden
 * 
 */

public class ClockClient extends Thread {

	private String host;
	private int port;
	private Gui_IO gio;
	
	private InputStream in;
	private OutputStream out;

	private ObjectInputStream sisääntulo;
	private ObjectOutputStream ulostulo;

	private WakeUpGroup herätysryhmä;
	private NetContainer paketti;
	
	private String ccName;

	public NetContainer getPaketti() {
		return this.paketti;
	}

	public WakeUpGroup getHerätysryhmä() {
		return this.herätysryhmä;
	}
	
	public void setHerätysryhmä(WakeUpGroup wug) {
		this.herätysryhmä = wug;
	}
	
	
	public Gui_IO getGIO() {
		return this.gio;
	}

public void lähetäPaketti(NetContainer paketti) {
		
		try { 
			ulostulo.writeObject(paketti);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

	}
	
	/**
	 * Vastaanottaa ja tulkitsee käskyjä palvelimelta (ExistingClientService <-- WakeUpService).
	 * 
	 * Käskyjen aloituskirjainten koko on valitettavasti epäjohdonmukainen.
	 * Osa alkaa isolla ja osa pienellä kirjaimella (niin kuin vastaavat metodit).
	 * Pitää olla tarkkana että käskyjen kirjoitusasu täsmää tiedonkulun kaikissa vaiheissa.
	 * 
	 * @param paketti
	 */
	public void lueKäsky(NetContainer paketti) {
		switch (paketti.getKomento()) {
		case "CancelAlarmImplement":  // palvelin lähettää ryhmän jäsenillle käskyn, joka peruu hälytykset
			
			gio.clearAlarmTime();  // nollaa herätysajan, asettaa GIO:ssa kuuluuRyhmään = false ja tyhjentää muuttujan omaRyhmä
			WakeUpGroup wug = (WakeUpGroup)paketti.getSisältö();
			gio.appendToStatus("Group leader has cancelled the alarm for group " + wug.getName());
			
			break;
		case "AlarmClient":
			
			gio.alarm();
			
			break;
			
		case "confirmAlarm":  // "alarmGroupLeader"; this is the first alarm that is sent to the leader
			
			gio.confirmAlarm((WakeUpGroup)paketti.getSisältö());
			
			break;
			
		case "GroupCreatedNotification":
			// käsky eteenpäin
			
			if(paketti.getSisältö() instanceof WakeUpGroup) {
				gio.groupCreatedNotification(paketti);
			}
			
			
			break;

		case "groupJoinedNotification":
			// käsky eteenpäin
			
			if(paketti.getSisältö() instanceof WakeUpGroup) {
				gio.groupJoinedNotification(paketti);
			}
			
			
			break;
			
		case "removeGroupMembers":
			gio.removedFromGroup((WakeUpGroup)paketti.getSisältö());			
			
			break;
			
		case "resignGroupNotification":
			
			gio.resignGroupNotification(paketti);
			
			break;
			
		case "UpdateExistingGroups":  // paketin sisältönä oltava CopyOnWriteArrayList<WakeUpGroup>, jossa palvelimeen tallennetut herätysryhmät
			if (paketti.getSisältö() instanceof CopyOnWriteArrayList<?>) {
				gio.fillGroups((CopyOnWriteArrayList<WakeUpGroup>)paketti.getSisältö());				
			}
			
			break;
			
		case "setAlarmTime":
			if (paketti.getSisältö() instanceof Instant) {
				gio.setAlarmTime((Instant)paketti.getSisältö());
			}
			
			
			break;

			// default casen syntaksi
			// default:

		}
	}


	/**
	 * Asiakas eli ClockClient muodostaa yhteyden. Yhteyden muodostus tehdään tässä konstruktorissa.
	 * 
	 * @param host
	 * @param port
	 * @param gio
	 */
	public ClockClient(String host, int port, Gui_IO gio) {
		try {

		this.host = host;
		this.port = port;
		this.gio = gio;
		
		Socket soketti = new Socket(host, port);

		this.in = soketti.getInputStream();
		this.out = soketti.getOutputStream();
		
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void run() {

		try {
			
			/* Kättelyjärjestys:
			 * OutputStream pitää luoda ensin.
			 * InputStream suorittaa kättelyn, ja tätä kättelyä varten OutputStream pitää olla ensin olemassa.
			 * Muuten tulee NullpointerException, kun lähetetään (eli lähettäjä ei ole vielä olemassa), koska kättely ei ole onnistunut.
			 */
			this.ulostulo = new ObjectOutputStream(this.out);
			this.sisääntulo = new ObjectInputStream(this.in);

			while (true) {

				// Luetaan sisääntulosta tulevaa syötettä objekteina
				// silmukan suoritus pysähtyy, kunnes saapuu luettava objekti. readObject() on blokkaava metodi
				// Tällainen ratkaisu toimii, koska kyseessä on säie
				Object objekti = sisääntulo.readObject();
				// Tarkistetaan, onko sisääntulo oikean tyyppinen eli NetContainer
				if (objekti instanceof NetContainer) {
					NetContainer nc = (NetContainer) objekti;
					lueKäsky(nc);
					// tee käskyjen toteutus lueKäsky()-metodiin
				}

			}

		} catch (IOException | ClassNotFoundException ioe) {
			// ClassNotFoundException voi tulla readObject()-muuttujasta
			ioe.printStackTrace();
		}

	}

}
