package fi.utu.tech.ringersClockServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import fi.utu.tech.ringersClock.ClockClient;
import fi.utu.tech.ringersClock.entities.*;

/*
 * 
 * luodaan näitä olioita yhtä paljon kuin on käynnissä olevia asiakassovelluksia
 * run()-metodi suorittaa metodia socket.readObject() tms. loputtomassa while()-silmukassa
 * parsetetaan viestejä
 * luetaan container-paketin käskymuuttujan sisältö
 * 
 */

/**
 * Huolehtii yhteydestä olemassaoleviin asiakasohjelmiin.
 * 
 * @author attep
 *
 */
public class ExistingClientService extends Thread {

//	private ServerSocketListener listener;
	private WakeUpService wup;

	// Soketti itsessään on jo valmis yhteys, joten portteja ei tarvita tähän
	// luokkamuuttujiksi
	// Soketti sisältää portit, IP-osoitteet ym.
	private Socket soketti;

	private ObjectInputStream sisääntulo;
	private ObjectOutputStream ulostulo;

	private InputStream in;
	private OutputStream out;

	public void lueKäsky(NetContainer paketti) {
		switch (paketti.getKomento()) {
		// case-lohko tarvitsee loppuun break-komennon, jos haluaa suorituksen loppuvan
		// siihen.
		// muuten kaikki komennot ajetaan sarjana sisääntulopisteestä asti loppuun.
		// Usein tarvitaan myös "default case". Tietoturvan kannalta on tässä ihan hyvä,
		// että default casea ei ole.
		// Silloin pääsisi lähettämään sisällöltään virheellisiä paketteja, jotka silti
		// ajetaan.
		case "AlarmAll":
			// Johtaja lähettää palvelimelle koko ryhmän herätyskäskyn
			if (paketti.getSisältö() instanceof WakeUpGroup) {
				wup.alarmAllImplement((WakeUpGroup)paketti.getSisältö());
				
			}
			break;

		case "CancelAlarm":
			/// Johtaja lähettää palvelimelle hälytyksen peruutuskäskyn
			if (paketti.getSisältö() instanceof WakeUpGroup) {
				wup.cancelAlarmImplement((WakeUpGroup) paketti.getSisältö());
			}
			break;

		case "createNewGroup":
			// Ryhmän luoja lähettää palvelimelle ryhmänluontikäskyn
			if (paketti.getSisältö() instanceof WakeUpGroup) {
				wup.lisääRyhmä((WakeUpGroup) paketti.getSisältö(), this);
			}
			break;
			
		case "joinGroup":
			// Asiakas lähettää palvelimelle pyynnön liittyä ryhmään
			
			wup.joinGroup((WakeUpGroup)paketti.getSisältö(), this);

			break;

		case "resignGroup":
			// Asiakas lähettää palvelimelle käskyn erota ryhmästä
			
			wup.resignGroup((WakeUpGroup)paketti.getSisältö(), this);			

			break;

		// default casen syntaksi
		// default:

		}
	}

	public void lähetäPaketti(NetContainer paketti) {

		try {
			ulostulo.writeObject(paketti);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

	}

	public void lähetäKuittaus() {

	}

	// Soketti huolehtii liikenteen linkittämisestä
	public ExistingClientService(Socket soketti, WakeUpService wup) {

		try {

			this.soketti = soketti;
			this.wup = wup;

			// sisään- ja ulostulovirrat saadaan soketista
			this.in = soketti.getInputStream();
			this.out = soketti.getOutputStream();

			// Soketeista saatavat streamit ovat kaikkein matalinta tasoa: tavuvirta (byte)
			// Nämä streamit annetaan käsiteltäviksi korkeamman tason streameille

			// Objekti ei ole ehjä, ennen kuin konstruktori on ajettu loppuun
			// ObjectStreameilla on jo omaa liikennettä, mikä voi tuottaa ongelmia
			// konstruktorin valmistumisen kannalta

		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void run() {

		try {

			this.ulostulo = new ObjectOutputStream(this.out);
			this.sisääntulo = new ObjectInputStream(this.in);
			
			// ECS:n muodostamisen yhteydessä (eli kun asiakas ottanut yhteyden) palvelin lähettää asiakkaalle listan olemassaolevista herätysryhmistä
			CopyOnWriteArrayList<WakeUpGroup> existingGroups = new CopyOnWriteArrayList<WakeUpGroup>();
			
			for (CopyOnWriteArrayList<WakeUpGroup> cowal : wup.wakeUpTimesList.values()) {
				existingGroups.addAll(cowal);
			}
			
			NetContainer existingGroupsNC = new NetContainer("UpdateExistingGroups", existingGroups);
			this.lähetäPaketti(existingGroupsNC);

			while (true) {

				// Luetaan sisääntulosta tulevaa syötettä objekteina
				Object objekti = sisääntulo.readObject();
				// Tarkistetaan, onko sisääntulo oikean tyyppinen eli NetContainer
				if (objekti instanceof NetContainer) {
					NetContainer nc = (NetContainer) objekti;
					lueKäsky(nc);  // luetaan NCssä oleva käsky
					// tee käskyjen toteutus lueKäsky()-metodiin
				}

			}

		} catch (IOException | ClassNotFoundException ioe) {
			// ClassNotFoundException voi tulla readObject()-muuttujasta
			ioe.printStackTrace();
		}

	}

}
