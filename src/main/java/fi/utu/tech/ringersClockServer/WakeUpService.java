package fi.utu.tech.ringersClockServer;

import fi.utu.tech.ringersClock.entities.NetContainer;
import fi.utu.tech.ringersClock.entities.WakeUpGroup;
import fi.utu.tech.weatherInfo.FMIWeatherService;
import fi.utu.tech.weatherInfo.WeatherData;

import java.io.IOException;
import java.sql.Time;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

// import Testaus.MyTask;

/*
 * Esim. kellonajan ylläpitämiseen ja hyödyntämiseen ota mallia paketeista, jotka on importoitu App.javaan
 * Siellä tärkeä metodi on esimerkiksi clockUpdate(); ota siitä mallia.
 */

public class WakeUpService extends Thread {

	private ServerSocketListener listener;
	private FMIWeatherService FMIWS;
	private Time aika;
	private Clock kello;

	private CopyOnWriteArrayList<ExistingClientService> yhteydet = new CopyOnWriteArrayList<ExistingClientService>(); // toteuta
																														// niin,
																														// että
																														// tämä
																														// lista
																														// pysyy
																														// pävivittyneenä

	private static Timer timer = new Timer();

	private Date time;

	// ArrayList itsessään ei ole säieturvallinen. Arvot eivät välttämättä päivity
	// oikein, kun useampi säie
	// käsittelee niitä samanaikaisesti: synkroniaongelmia.
	// Käytetään joko toisen tyyppistä listaa tai tehdään paketointi
	// Collections.synchronizedList() paketoi listan säieturvalliseksi
	// List<ExistingClientService> yhteydet = Collections.synchronizedList(new
	// ArrayList<ExistingClientService>());

	// ArrayList<WakeUpGroup> ryhmät;

	// Säieturvallinen avain-arvolista, joka säilyttää tiedon palvelimella olevista
	// herätysryhmistä ja niihin liitetyistä asiakasyhteyksistä
	ConcurrentHashMap<String, CopyOnWriteArrayList<ExistingClientService>> groupConnectionsList;

	// Key-herätysaika muodossa 00:00 ja
	// Sisäluokka MyTask toteuttaa herätyksiä
	ConcurrentHashMap<String, CopyOnWriteArrayList<WakeUpGroup>> wakeUpTimesList;

	/**
	 * ServerSocketListener kutsuu tätä metodia, kun uusi asiakasyhteys muodostetaan
	 * 
	 * @param ecs
	 */
	public void lisääYhteys(ExistingClientService ecs) {
		System.out.println("Lisätään uusi asiakasyhteys " + ecs);
		this.yhteydet.add(ecs);
	}

	/**
	 * Metodi, jolla toteutetaan asiakkaan lähettämä pyyntö liittyä ryhmään.
	 * 
	 * @author attep
	 * @param paketti
	 */
	public void joinGroup(WakeUpGroup wug, ExistingClientService ecs) { // Otetaan parametrina sisään ECS, jotta
																		// lähettäjä voidaan tunnistaa

		CopyOnWriteArrayList<ExistingClientService> gcl = new CopyOnWriteArrayList<ExistingClientService>();
		gcl = groupConnectionsList.get(wug.getID()); // otetaan talteen avaimen kohdalla oleva yhteyslista
		gcl.add(ecs); // lisätään uusi yhteys olemassaolevaan ryhmään. TÄSTÄ RIVISTÄ TULEE
						// JOINGROUP-KOMENNOLLA NULLPOINTEREXCEPTION
		groupConnectionsList.replace(wug.getID(), gcl); // päivittää asiakasyhteyslistaan kyseisen herätysryhmän
														// kohdalle
		// päivitetyn yhteyslistan

		groupJoinedNotification(wug, ecs);

		System.out.println("Asiakas " + ecs.toString() + " liittyi ryhmään " + wug.getName());

	}

	public void groupJoinedNotification(WakeUpGroup wug, ExistingClientService ecs) {

		NetContainer groupJoinedNotificationNC = new NetContainer("groupJoinedNotification", wug);

		ecs.lähetäPaketti(groupJoinedNotificationNC);

		NetContainer setAlarmTimeNC = new NetContainer("setAlarmTime", getTimeInstant(wug));
		ecs.lähetäPaketti(setAlarmTimeNC);

	}

	/**
	 * Luo palvelimelle uuden ryhmän asiakkaan pyynnön mukaisesti.
	 * 
	 * @param ryhmä
	 * @param ecs
	 */
	public void lisääRyhmä(WakeUpGroup ryhmä, ExistingClientService ecs) { // käsky "createNewGroup" asiakkaalta

		CopyOnWriteArrayList<ExistingClientService> ecsl = new CopyOnWriteArrayList<ExistingClientService>();
		ecsl.add(ecs);

		this.groupConnectionsList.put(ryhmä.getID(), ecsl); // lisää groupConnectionsListiin uuden avain-arvoparin
															// (ryhmä -
		// yhteyslista, jossa ensimmäinen yhteys)

		// Lisätään wakeUpTimes-avainarvoparilistaan uusi herätysaika.
		// Tarkistetaan, onko tämä herätysaika jo olemassa joillekin muille ryhmille.
		if (wakeUpTimesList.containsKey(ryhmä.getWakeUpTime())) { // Herätysaika jo olemassa, lisätään uusi herätysryhmä
																	// tälle herätysajalle
			CopyOnWriteArrayList<WakeUpGroup> wgl = new CopyOnWriteArrayList<WakeUpGroup>();
			wgl = wakeUpTimesList.get(ryhmä.getWakeUpTime());
			wgl.add(ryhmä);
			wakeUpTimesList.replace(ryhmä.getWakeUpTime(), wgl);

		} else { // Herätysaikaa ei olemassa, lisätään uusi
			CopyOnWriteArrayList<WakeUpGroup> wgl = new CopyOnWriteArrayList<WakeUpGroup>(); // pitäisi vielä tarkistaa,
																								// onko jo olemassa
																								// ryhmiä, joilla on
																								// sama herätysaika
			wgl.add(ryhmä);
			wakeUpTimesList.put(ryhmä.getWakeUpTime(), wgl);

		}

		groupCreatedNotification(ryhmä, ecs);
		System.out.println(
				"Luotu ryhmä " + ryhmä.getName() + " (Herätysaika:" + ryhmä.getWakeUpTime() + ", Sade ei sallittu:"
						+ ryhmä.getRainDemand() + ", Pakkanen ei sallittu:" + ryhmä.getTemperatureDemand() + ")");
	}

	/**
	 * Lähettää asiakkaalle vahvistuksen ryhmän lisäämisestä. Lähettää lisäksi
	 * päivitetyn listan palvelimella olevista ryhmistä.
	 * 
	 * @param ryhmä
	 * @param ecs
	 */
	public void groupCreatedNotification(WakeUpGroup ryhmä, ExistingClientService ecs) {

		NetContainer groupCreatedNotificationNC = new NetContainer("GroupCreatedNotification", ryhmä);
		ecs.lähetäPaketti(groupCreatedNotificationNC); // lähetetään ryhmänluonti-ilmoitus

		NetContainer setAlarmTimeNC = new NetContainer("setAlarmTime", getTimeInstant(ryhmä));

		ecs.lähetäPaketti(setAlarmTimeNC);

		updateExistingGroups(); // päivitetään lista olemassaolevista ryhmistä kaikkiin asiakassovelluksiin
	}

	/**
	 * Tämä metodi käynnistetään, kun kello saavuttaa jonkin säädetyn herätysajan
	 * Johtajalle lähetetään pyyntö vahvistaa herätys
	 * 
	 * @param ryhmä
	 */
	public void alarmGroupLeader(WakeUpGroup ryhmä) {

		ExistingClientService leaderConnection = groupConnectionsList.get(ryhmä.getID()).get(0); // ryhmän johtajan
																									// yhteys
		// indeksissä 0
		NetContainer confirmAlarmNC = new NetContainer("confirmAlarm", ryhmä);
		leaderConnection.lähetäPaketti(confirmAlarmNC);

	}

	/**
	 * Lähettää herätyskäskyn kaikille parametrina annetun ryhmän jäsenille eli
	 * asiakassovelluksille.
	 * 
	 * @param ryhmä
	 */
	public void alarmAllImplement(WakeUpGroup ryhmä) {

		NetContainer AlarmClientNC = new NetContainer("AlarmClient", ryhmä);

		/*
		 * Serialisointi tiedonsiirron yhteydessä muuttanut olioiden identiteetin Olion
		 * identiteetin tunnistaminen: toIdentity()-metodi? *
		 * 
		 * Kaikilla olioilla on hashCode, jonka perusteella voidaan tarkastella sen
		 * identiteettiä
		 * 
		 * Ongelman ratkaisu: muutetaan hashmapin avain ryhmäID:ksi joka on UUID:sta
		 * muutettu Stringiksi (yksinkertaisuuden vuoksi). ONGELMA RATKAISTU.
		 * 
		 */

		for (ExistingClientService ecs : groupConnectionsList.get(ryhmä.getID())) {
			ecs.lähetäPaketti(AlarmClientNC);
		}

		System.out.println("Ryhmän " + ryhmä.getName() + " johtaja hyväksyi herätyksen -> herätetään ryhmän jäsenet.");

		groupConnectionsList.remove(ryhmä.getID()); // poistetaan tämä herätysryhmä (ja siihen liittyvät
													// asiakasyhteydet) palvelimelta

		removeWakeUp(ryhmä);

		updateExistingGroups(); // päivitetään kaikille asiakassovelluksille lista olemassaolevista ryhmistä.
								// herätyksen toteuttaminen poisti tämän ryhmän.

	}

	/**
	 * Toteutetaan hälytyksen peruutus. Tämä metodi käynnistyy, kun ryhmän johtaja
	 * on perunut hälytyksen.
	 * 
	 * @param ryhmä
	 */
	public void cancelAlarmImplement(WakeUpGroup ryhmä) {

		NetContainer cancelAlarmImplementNC = new NetContainer("CancelAlarmImplement", ryhmä);
		for (ExistingClientService ecs : groupConnectionsList.get(ryhmä.getID())) {
			ecs.lähetäPaketti(cancelAlarmImplementNC);
		}

		this.groupConnectionsList.remove(ryhmä.getID()); // Poistetaan herätysryhmä palvelimen listasta
		// this.wakeUpTimesList.remove(ryhmä.getWakeUpTime()); // Poistetaan herätysaika
		// palvelimen listasta

		removeWakeUp(ryhmä);

		System.out
				.println("Ryhmän " + ryhmä.getName() + " johtaja ei hyväksynyt herätystä -> ei herätä ryhmän jäseniä.");

		updateExistingGroups(); // päivitetään kaikille asiakassovelluksille tieto olemassaolevista ryhmistä.
								// herätyksen peruminen poisti tämän ryhmän.

	}

	/**
	 * Ottaa vastaan asiakkaalta tulleen käskyn erota ryhmästä. Poistaa
	 * ryhmäjäsenyyden. Jos ainoa jäsen erosi, palvelimelta poistetaan koko
	 * herätysryhmä ja tähän liittyvät herätysajat. Jos ryhmä poistetaan, lähetetään
	 * myös kaikille palvelimen asiakkaille päivitetty ryhmälista
	 * updateExistingGroups()-metodilla
	 * 
	 * @param ryhmä
	 * @param ecs
	 * @author attep
	 */
	public void resignGroup(WakeUpGroup ryhmä, ExistingClientService ecs) {

		String wu_time = ryhmä.getWakeUpTime();
		CopyOnWriteArrayList<ExistingClientService> gcl = groupConnectionsList.get(ryhmä.getID());

		if (gcl.size() > 1) { // jos herätysryhmässä on useampi jäsen, poistetaan vain eronnut jäsen

			CopyOnWriteArrayList<ExistingClientService> ecs_list = groupConnectionsList.get(ryhmä.getID());

			if (ecs_list.indexOf(ecs) == 0) { // eroaja on johtaja

				removeGroupMembers(ryhmä);				
				groupConnectionsList.remove(ryhmä.getID());
				removeWakeUp(ryhmä);
				
				System.out.println("Ryhmän " + ryhmä.getName() + " johtaja " + "(yhteys " + ecs + ") erosi. Poistetaan koko ryhmä ja sen herätykset palvelimelta.");
				updateExistingGroups();
				
			} else { // eroaja ei ole johtaja, ryhmää pysyy edelleen olemassa. ainoastaan yksi jäsen poistetaan.
				ecs_list.remove(ecs);
				groupConnectionsList.replace(ryhmä.getID(), ecs_list);
				
				System.out.println("Jäsen " + ecs + " erosi ryhmästä " + ryhmä.getName());
			}

		} else { // jos herätysryhmässä oli vain yksi jäsen, poistetaan samalla koko ryhmä
					// palvelimelta. Jos herätysryhmä poistetaan, myös siihen liittyvät herätykset
					// (wakeUpTime) pitää poistaa.
			groupConnectionsList.remove(ryhmä.getID());
			
			System.out.println("Ryhmän " + ryhmä.getName() + " ainoa jäsen (johtaja) erosi. Poistetaan ryhmä.");
			
			CopyOnWriteArrayList<WakeUpGroup> wugs = wakeUpTimesList.get(wu_time);  // otetaan ulos herätysryhmälista tältä herätysajalta

			if (wugs.size() > 1) { // jos on useampia herätysryhmiä samalla herätysajalla, ei
															// voida poistaa koko herätysaikaa
				
				wugs.remove(ryhmä);
				wakeUpTimesList.replace(wu_time, wugs);

				updateExistingGroups(); // koska palvelimelta poistettiin ryhmä, lähetetään kaikille asiakkaille
										// päivitetty tieto olemassaolevista ryhmistä. tarvitaan erikseen kahteen
										// if-lohkoon.

			} else { // jos herätysryhmiä herätysajalle on vain yksi, poistetaan koko herätysaika
						// palvelimelta
				wakeUpTimesList.remove(wu_time);

				updateExistingGroups(); // koska palvelimelta poistettiin ryhmä, lähetetään kaikille asiakkaille
										// päivitetty tieto olemassaolevista ryhmistä. tarvitaan erikseen kahteen
										// if-lohkoon.

			}
		}

		resignGroupNotification(ryhmä, ecs); // lähetetään asiakkaalle eroamisvahvistus

	}

	/**
	 * Tätä metodia kutsutaan, kun ryhmän johtaja eroaa ryhmästä. Kun johtaja eroaa,
	 * koko ryhmä poistetaan palvelimelta. Ennen ryhmän poistamista kaikki sen
	 * jäsenet pitää erottaa.
	 * 
	 * Poistetaan ryhmästä kaikki jäsenet paitsi johtaja. Poistetaan siis kaikki
	 * yhteydet indeksistä 1 alkaen.
	 * 
	 * @param ryhmä
	 */
	public void removeGroupMembers(WakeUpGroup ryhmä) {

		NetContainer removeGroupMembersNC = new NetContainer("removeGroupMembers", ryhmä);

		CopyOnWriteArrayList<ExistingClientService> ccl = groupConnectionsList.get(ryhmä.getID()); // otetaan ulos tämän
																									// ryhmän
																									// asiakasyhteydet

		for (int i = 1; i < ccl.size(); i++) { // käydään läpi kaikki asiakasyhteydet paitsi johtaja
			ccl.get(i).lähetäPaketti(removeGroupMembersNC);
		}
	}

	/**
	 * Lähettää asiakkaalle vahvistuksen siitä, että ryhmästä on erottu.
	 * resignGroup()-metodi kutsuu tätä metodia, kun eroaminen on suoritettu.
	 * 
	 * @param ryhmä
	 * @param ecs
	 */
	public void resignGroupNotification(WakeUpGroup ryhmä, ExistingClientService ecs) {
		NetContainer resignGroupNotificationNC = new NetContainer("resignGroupNotification", ryhmä);

		ecs.lähetäPaketti(resignGroupNotificationNC);
	}

	/**
	 * Poistaa palvelimelta herätyksen parametrina annetulta ryhmältä. Herätysaika
	 * jää palvelimelle, jos toisella ryhmällä on sama herätysaika.
	 * 
	 * @param ryhmä
	 */
	public void removeWakeUp(WakeUpGroup ryhmä) {

		CopyOnWriteArrayList<WakeUpGroup> wuglist = wakeUpTimesList.get(ryhmä.getWakeUpTime());
		String wut = ryhmä.getWakeUpTime();
		wuglist.remove(ryhmä);

		if (wuglist.size() > 0) { // jos herätysajalla oli muitakin ryhmiä, herätysaika jää palvelimelle
			wakeUpTimesList.replace(wut, wuglist);
		} else { // jos ainoa herätysaikaan sidottu ryhmä poistettiin, koko herätysaika
					// poistetaan palvelimelta
			wakeUpTimesList.remove(wut);
		}

	}

	/**
	 * Lähetetään päivitetty lista palvelimella olevista herätysryhmistä kaikille
	 * palvelimen asiakkaille. Kutsuu asiakkaan päässä käyttöliittymän
	 * fillGroups()-metodia, joka päivittää käyttöliittymään olemassaolevat ryhmät
	 * 
	 * Päivitä myös asiakkaille, jotka eivät kuulu mihinkään ryhmään !!! Käytä
	 * listaa "yhteydet" Lakanneen yhteyden poistuminen? Periaatteessa voisi
	 * toteuttaa niin, että lopettava asiakas lähettää paketin Kun ikkuna suljetaan,
	 * ajetaan aina jokin metodi (kuuluu käyttöliittymän toimintaan). Tämän
	 * toiminnallisuuden hyödyntämiseksi tarvittaisiin kuitenkin luultavasti enemmän
	 * JavaFX-osaamista
	 * 
	 * Kun verkkoyhteyksiä muodostetaan, streamit pitäisi myös sulkea
	 * 
	 * try-with-resources-lause: voitaisiin käyttää, kun ObjectOutputStream
	 * sulkeutuu
	 * 
	 * try-catch-rakenne updateExistingGroupsiin? catch-lauseessa (kun tulee
	 * poikkeus eli yhteyttä ei ole) kyseinen yhteys poistetaan
	 * 
	 * refactor --> rename: muuttujien ym nimeäminen kaikki esiintymät kerralla
	 * 
	 * 
	 * 
	 */
	public void updateExistingGroups() {
		CopyOnWriteArrayList<WakeUpGroup> existingGroups = new CopyOnWriteArrayList<WakeUpGroup>();

		for (CopyOnWriteArrayList<WakeUpGroup> cowal : wakeUpTimesList.values()) {
			existingGroups.addAll(cowal);
		}

		NetContainer updateExistingGroupsNC = new NetContainer("UpdateExistingGroups", existingGroups);

		// Käydään läpi kaikki asiakasyhteydet. Tämä käy läpi vain yhteydet, jotka
		// kuuluvat johonkin ryhmään. EI RIITÄ!
//		for (CopyOnWriteArrayList<ExistingClientService> ecslist : groupConnectionsList.values()) {
//			for (ExistingClientService ecs2 : ecslist) {
//				ecs2.lähetäPaketti(updateExistingGroupsNC);
//			}
//		}

		for (ExistingClientService ecs : yhteydet) {
			try {
				System.out.println("Lähetetään ryhmäpäivitykset asiakkaalle " + ecs);
				ecs.lähetäPaketti(updateExistingGroupsNC);
			} catch (Error e) { // poistetaan yhteys, jos sitä ei ole olemassa (= tulee poikkeus). EI TOIMI, PÄÄTTYNEET ASIAKASYHTEYDET JÄÄVÄT KUMMITTELEMAAN
				System.out.println("Yhteyttä " + ecs
						+ " ei löydetty ryhmäpäivityksen yhteydessä, joten poistetaan se yhteyslistalta.");
				yhteydet.remove(ecs);
			}
		}

	}

	/*
	 * Muodostetaan ja lähetetään asiakkaan kelloon säädettävä herätysaika.
	 * 
	 */
	public Instant getTimeInstant(WakeUpGroup ryhmä) {

		String zdt_string = ZonedDateTime.now().toString();

		StringBuilder zdt_sb = new StringBuilder();
		zdt_sb.append(zdt_string);
		zdt_sb.replace(11, 13, String.format("%02d", ryhmä.getHour()));
		zdt_sb.replace(14, 16, String.format("%02d", ryhmä.getMinutes()));
		zdt_sb.replace(17, 19, "00");
		zdt_string = zdt_sb.toString();

		ZonedDateTime wut_zdt = ZonedDateTime.parse(zdt_string);
		Instant wut_instant = wut_zdt.toInstant();

		return wut_instant;
	}

	public void setListener(ServerSocketListener l) {
		this.listener = l;
	}

	public WakeUpService() {
		this.groupConnectionsList = new ConcurrentHashMap<String, CopyOnWriteArrayList<ExistingClientService>>();
		this.wakeUpTimesList = new ConcurrentHashMap<String, CopyOnWriteArrayList<WakeUpGroup>>();

		// https://www.iitk.ac.in/esc101/05Aug/tutorial/essential/threads/timer.html
		// Get the Date corresponding to 11:01:00 pm today.
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.SECOND, 00);
		this.time = calendar.getTime();

	}

	/**
	 * Kuuntelee sääpalvelua? Hakee säätiedot kerran 5 minuutissa
	 * 
	 * PAREMPI RATKAISU: Toteuta niin, että hakee säätiedot hieman ennen herätystä.
	 * Tätä varten palvelin pitää kirjaa tulevista herätysajoista
	 */

	public void run() {

		timer.scheduleAtFixedRate(new MyTask(), this.time, 1000 * 60 * 1);
		Random rnd = new Random(); // Käytetään säikeen nukkumisen asettamissa

		while (true) {

			try {
				ZonedDateTime zdtNowServer = ZonedDateTime.now();
				System.out.println("WakeUpServer käynnissä: " + zdtNowServer.toString());

				Thread.sleep((rnd.nextInt(10) + 1) * 60 * 1000);

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	class MyTask extends TimerTask {

		public void run() {

			ZonedDateTime zdtNow = ZonedDateTime.now();

			// System.out.println("Serverin kello: " +
			// DateTimeFormatter.ISO_INSTANT.format(zdtNow).toString().substring(11, 19));

			String tunnit = zdtNow.toString().substring(11, 13);
			String minuutit = zdtNow.toString().substring(14, 16);
			String aika = tunnit + ":" + minuutit;

			// System.out.println("Tunnit: " + tunnit + " Minuutit: " + minuutit);

			// herätys lähtee tästä
			// if (Integer.parseInt(minuutit) == 0 || Integer.parseInt(minuutit) % 1 == 0) {
			if (Integer.parseInt(minuutit) == 0 || Integer.parseInt(minuutit) % 5 == 0) {
				System.out.println(
						"Kello: " + zdtNow.toString().substring(11, 19) + " Tarkistetaan ryhmien hälytysajat.");

				// TÄSSÄ kohtaa pitää käydä alarmTimesForGroups lista ja tarkistaa onko listalla
				// sama aika

				// System.out.println(" [debug print 01, aika Stringinä] " + aika);

				// for (CopyOnWriteArrayList<ExistingClientService> ecslist :
				// groupConnectionsList.values()) {
				// for (ExistingClientService ecs : ecslist) {
				// System.out.println(" [debug print 02] existing client service " +
				// ecs.toString());
				// }
				// }

				if (wakeUpTimesList.containsKey(aika)) {
					// Haetaan listaan ryhmät, joiden herätysaika on sama kuin aika
					CopyOnWriteArrayList<WakeUpGroup> wugs = new CopyOnWriteArrayList<WakeUpGroup>();
					wugs.addAll(wakeUpTimesList.get(aika));

					try {
						// Haetaan säätiedot
						WeatherData wData = FMIWeatherService.getWeather();

						// Käydään lista läpi ja tarkastetaan ryhmän säävaateet
						for (WakeUpGroup wug : wugs) {
							// System.out.println(" [debug print 03, wug: säävaatimukset] Sade: " +
							// wug.getRainDemand() + " Lämpötila: " + wug.getTemperatureDemand());
							// Ŕyhmälle ei ole asetettu säävaateita
							if (wug.getRainDemand() == false && wug.getTemperatureDemand() == false) {
								// Säälle ei ole vaateita ---> Johtajan saa herättää
								if (true) {
									System.out.println("   Ryhmä " + wug.getName() + " säävaateet:"
											+ " Ei säävaateita -> Lähetetään herätyskäsky ryhmän johtajalle.");
									alarmGroupLeader(wug);
								}
							}
							// Ŕyhmälle on asetettu vaade: ei saa sataa (lämpötila saa olla pakkasella)
							else if (wug.getRainDemand() == true && wug.getTemperatureDemand() == false) {
								// Jos ei saada ---> Johtajan saa herättää
								if (!wData.getItIsRaining()) {
									System.out.println("   Ryhmä " + wug.getName() + " säävaateet:"
											+ " Ei sadetta (Säätila: ei sada) -> Lähetetään herätyskäsky ryhmän johtajalle.");
									alarmGroupLeader(wug);
								} else {
									System.out.println("   Ryhmä " + wug.getName() + " säävaateet:"
											+ " Ei sadetta (Säätila: sataa) -> Ei herätetä ryhmän johtajaa.");
								}
							}
							// Ŕyhmälle on asetettu vaade: ei saa olla pakkasta (saa sataa)
							else if (wug.getRainDemand() == false && wug.getTemperatureDemand() == true) {
								// Jos ei alle 0 nolla astetta ---> Johtajan saa herättää
								if (!wData.getItIsTooCold()) {
									System.out.println("   Ryhmä " + wug.getName() + " säävaateet:"
											+ " Ei pakkasta (Säätila: ei pakkasta) -> Lähetetään herätyskäsky ryhmän johtajalle.");
									alarmGroupLeader(wug);
								} else {
									System.out.println("   Ryhmä " + wug.getName() + " säävaateet:"
											+ " Ei pakkasta (Säätila: pakkasta) -> Ei herätetä ryhmän johtajaa.");
								}
							}
							// Ŕyhmälle on asetettu vaateet: ei saa sataa JA lämpötila ei saa olla
							// pakkasella
							else if (wug.getRainDemand() == true && wug.getTemperatureDemand() == true) {
								// Jos ei saada JA ei alle 0 nolla astetta ---> Johtajan saa herättää
								if (!wData.getItIsRaining() && !wData.getItIsTooCold()) {
									System.out.println("   Ryhmä " + wug.getName() + " säävaateet:"
											+ " Ei pakkasta ja Ei sada (Säätila: ei pakkasta ja ei sada) -> Lähetetään herätyskäsky ryhmän johtajalle.");
									alarmGroupLeader(wug);
								} else {
									System.out.println("   Ryhmä " + wug.getName() + " säävaateet:"
											+ " Ei pakkasta ja Ei sada (Säätila: on pakkasta tai sataa) -> Ei herätetä ryhmän johtajaa.");
								}
							}
						} // for
					} catch (ParserConfigurationException | SAXException | IOException e) {
						e.printStackTrace();
					} // catch
				} // if
			} // if
		} // run()
	} // MyTask

}
