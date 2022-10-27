package fi.utu.tech.ringersClock;

import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import fi.utu.tech.ringersClock.UI.MainViewController;
import fi.utu.tech.ringersClock.entities.NetContainer;
import fi.utu.tech.ringersClock.entities.WakeUpGroup;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

public class Gui_IO {

	private MainViewController cont;

	private ClockClient cc;

	private boolean kuuluuRyhmään;
	private WakeUpGroup omaRyhmä;

	public void setCC(ClockClient cc) {
		this.cc = cc;
	}

	public Gui_IO(MainViewController cont) {
		this.cont = cont;
		this.kuuluuRyhmään = false;

	}
	
	
	public void setKuuluuRyhmään (boolean kuuluu) {
		this.kuuluuRyhmään = kuuluu;
	}
	
	public void setOmaRyhmä (WakeUpGroup ryhmä) {
		this.omaRyhmä = ryhmä;
	}
	
	

	/*
	 * Method for displaying the time of the alarm Use this method to set the alarm
	 * time in the UI The time is received from the server
	 * 
	 * DO not edit
	 */
	public void setAlarmTime(Instant time) {
		Platform.runLater(new Runnable() {  //Staattinen metodi - ottaa runnablen sisälleen
			@Override
			public void run() {
				cont.setAlarmTime(time);
			}
		});
	}

	/*
	 * Method for clearing the time of the alarm. Use this method when alarm is
	 * cancelled.
	 * 
	 * DO not edit
	 */
	public void clearAlarmTime() {

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				cont.clearAlarmTime();    // Nollaukset toteutetaan täällä mainViewControllerin metodissa.
											// omaRyhmä = null ja kuuluuRyhmään = false
			}
		});
	}

	/*
	 * Method for appending text to the status display You can write status messages
	 * to UI using this method.
	 * 
	 * DO not edit
	 */
	public void appendToStatus(String text) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				cont.appendToStatus(text);
			}
		});
	}

	/*
	 * Method for filling the existing wake-up groups to Choosebox Must run every
	 * time when the existing wake-up groups are receiver from the server
	 *
	 * DO not edit
	 */
	public void fillGroups(CopyOnWriteArrayList<WakeUpGroup> list) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				cont.fillGroups(list);
				System.out.println("Group information updated from server.");
			}
		});
	}

	/*
	 * This method is for waking up the ringer when it is time.
	 * 
	 * DO not edit
	 */

	public void alarm() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				cont.alarm();
				cont.clearAlarmTime(); // clearAlarmTime myös asettaa kuuluuRyhmään = false sekä tyhjentää muuttujan omaRyhmä
			}
		});
	}

	/*
	 * This method must only on the client is the group leader. The idea is to use
	 * this method to confirm the wake up before waking up the rest of the team
	 * 
	 * DO not edit
	 */

	public void confirmAlarm(WakeUpGroup group) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				Alert alert = new Alert(AlertType.CONFIRMATION);
				alert.setTitle("Confirm alarm");
				alert.setHeaderText("Do you want wake up the team " + group.getName() + "?");
				alert.setContentText("The weather seems to be ok.");
				alert.showAndWait().ifPresent((btnType) -> {
					if (btnType == ButtonType.OK) {
						AlarmAll(group);
					} else {
						CancelAlarm(group);
					}
				});
			}
		});
	}

	/*
	 * This method is run if the leader accepts the wake-up Now you have to wake up
	 * the rest of the team
	 * 
	 * IMPLEMENT THIS ONE
	 */
	public void AlarmAll(WakeUpGroup group) {
		System.out.println("AlarmAll " + group.getName());

		NetContainer alarmAllNC = new NetContainer("AlarmAll", group);

		cc.lähetäPaketti(alarmAllNC);   // käskyketjun päässä kaikille asiakkaille ajetaan metodi alarm()

		// lähetä käsky ClockClientille
	}

	/*
	 * This method is run if the leader cancel the wake-up The alarm is cancelled
	 * and should be removed from server
	 * 
	 * IMPLEMENT THIS ONE
	 */
	public void CancelAlarm(WakeUpGroup group) {
		System.out.println("CancelAll " + group.getName());

		NetContainer CancelAlarmNC = new NetContainer("CancelAlarm", group);

		cc.lähetäPaketti(CancelAlarmNC);  // käskyketjun päässä (AlarmAllImplement) kaikille asiakkaille ajetaan metodi clearAlarmTime()
											// nollaa herätysajan, asettaa kuuluuRyhmään = false ja tyhjentää muuttujan omaRyhmä


		// lähetä käsky ClockClientille
	}

	/*
	 * This method is run when user pressed the create button Now the group with
	 * wake-up time must be sent to server
	 * 
	 * IMPLEMENT THIS ONE
	 */
	public void createNewGroup(String name, Integer hour, Integer minutes, boolean norain, boolean temp) {
		
		if (this.kuuluuRyhmään == false) {   // voi luoda uuden ryhmän vain, jos ei ole voimassaolevaa ryhmäjäsenyyttä
		System.out.println("Create New Group pressed, group name: " + name + " Wake-up time: "
				+ String.format("%02d", hour) + ":" + String.format("%02d", minutes) + " No rain allowed: " + norain
				+ " Temperature over 0 deg: " + temp);

		WakeUpGroup uusiryhmä = new WakeUpGroup(name, hour, minutes, norain, temp);

		NetContainer uusiRyhmäPaketti = new NetContainer("createNewGroup", uusiryhmä);

		cc.lähetäPaketti(uusiRyhmäPaketti);

		// lähetä käsky ClockClientille
		
		} else {
			appendToStatus("Cannot create new group, already a member of " + this.omaRyhmä.getName() + "\nResign from group first\n");			
		}
			
	}

	/*
	 * This method is run when user pressed the join button The info must be sent to
	 * server
	 * 
	 * IMPLEMENT THIS ONE
	 * 
	 * Vasta kun palvelimelta on saatu vastaus (groupJoinedNotification),
	 * asiakassovellukseen tallennetaan tieto siitä, että kuulutaan tähän ryhmään.
	 * Ryhmään liittyminen edellyttää siis onnistunutta tiedonsiirtoa palvelimen
	 * kanssa. Ryhmään kuulumista ei siis vielä tallenneta tämän joinGroup()-metodin
	 * toteutuksessa.
	 * 
	 */

	public void joinGroup(WakeUpGroup group) {
		System.out.println("Join Group pressed");

		if (this.kuuluuRyhmään == false) {   // ei voi liittyä ryhmään, jos kuuluu jo johonkin ryhmään

			NetContainer joinGroupNC = new NetContainer("joinGroup", group);
			cc.lähetäPaketti(joinGroupNC);

			// lähetä käsky ClockClientille

		} else {
			appendToStatus("Cannot join group " + group.getName() + " , already a member of group " + this.omaRyhmä.getName() + "\nResign from group first\n");
		}

	}

	/*
	 * This method is run when user pressed the resign button The info must be sent
	 * to server
	 * 
	 * IMPLEMENT THIS ONE
	 */
	public void resignGroup() {
		System.out.println("Resign Group pressed");

		if (this.kuuluuRyhmään == true) { // on mahdollista erota vain, jos asiakas kuuluu johonkin ryhmään
			NetContainer resignGroupNC = new NetContainer("resignGroup", this.omaRyhmä);

			cc.lähetäPaketti(resignGroupNC);
			// lähetä käsky ClockClientille
		} else {
			appendToStatus("No group memberships, cannot resign\n");
		}

	}

	/**
	 * Ottaa vastaan palvelimen lähettämän vahvistuksen ryhmästä eroamisesta ja
	 * tulostaa tästä ilmoituksen käyttöliittymään.
	 */
	public void resignGroupNotification(NetContainer paketti) {
		WakeUpGroup wug = (WakeUpGroup) paketti.getSisältö();
		System.out.println("Server response: Resigned from group " + wug.getName());

		appendToStatus("Server response: Resigned from group " + wug.getName() + "\n");

		clearAlarmTime(); // clearAlarmTime myös asettaa kuuluuRyhmään = false ja tyhjentää muuttujan omaRyhmä

	}
	
	/**
	 * Ryhmän johtaja erosi ryhmästä, joten kaikki muut jäsenet erotetaan ja ryhmä poistetaan.
	 * Ottaa vastaan palvelimen lähettämän erottamiskäskyn.
	 * @param paketti
	 */
	public void removedFromGroup(WakeUpGroup ryhmä) {
		
		appendToStatus("Group leader resigned from group " + ryhmä.getName() + "\nGroup removed");
		
		clearAlarmTime();
		
		
	}

	/**
	 * Ottaa vastaan palvelimen lähettämän vahvistuksen uuden ryhmän luomisesta ja
	 * tulostaa tästä ilmoituksen käyttöliittymään.
	 * 
	 * @author attep
	 * @param paketti
	 */
	public void groupCreatedNotification(NetContainer paketti) {
		WakeUpGroup wug = (WakeUpGroup) paketti.getSisältö();

		this.kuuluuRyhmään = true;
		this.omaRyhmä = wug;
		appendToStatus("Server response: Group added\n" + "Group name: " + wug.getName() + "\nWake-up time: " + wug.getWakeUpTime() + "\n");

		System.out.println("Server response: Group " + wug.getName() + " was created");
	}

	/**
	 * Ottaa vastaan palvelimen lähettämän vahvistuksen uuteen ryhmään liittymisestä
	 * ja tulostaa tästä ilmoituksen käyttöliittymään.
	 * 
	 * Vasta tässä vaiheessa merkitään asiakassovellukseen tieto siitä, että
	 * kuulutaan tähän ryhmään. Ryhmään liittyminen edellyttää siis onnistunutta
	 * tiedonsiirtoa palvelimen kanssa.
	 * 
	 * @author attep
	 * @param paketti
	 */
	public void groupJoinedNotification(NetContainer paketti) {
		WakeUpGroup wug = (WakeUpGroup) paketti.getSisältö();

		this.cc.setHerätysryhmä(wug); // asetetaan herätysryhmä asiakassovellukseen

		this.kuuluuRyhmään = true;
		this.omaRyhmä = wug;
		appendToStatus(
				"Server response: Joined group\n" + "Group name " + wug.getName() + "\nWake-up time: " + wug.getWakeUpTime() + "\n");

		System.out.println("Server response: Succesfully joined group " + wug.getName());
	}

}
