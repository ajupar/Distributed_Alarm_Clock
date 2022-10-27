package fi.utu.tech.ringersClockServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerSocketListener extends Thread {

	protected String host;
	protected int port;
	protected WakeUpService wup;
	protected CopyOnWriteArrayList<ExistingClientService> yhteydet;   // ylläpitää tietoa asiakasyhteyksistä. muut luokat hakevat tästä tietoa get-metodilla

	public ServerSocketListener(String host, int port, WakeUpService wup) {
		this.host = host;
		this.port = port;
		this.wup = wup;
		yhteydet = new CopyOnWriteArrayList<ExistingClientService>();
	}

	/**
	 * Kuuntelee saapuvia yhteydenottoja ja luo uuden soketin, kun yhteys on
	 * hyväksytty.
	 */
	public void run() {

		try {
			
			// ServerSocketin konstruktoriin pitää antaa parametrina portti
			// Periaatteessa tarvitaan parametrina myös host, mutta ei tässä koska ohjelma on sen verran yksinkertainen
			// Kun vain portti on määritetty, soketti kuuntelee näitä portteja kaikkialla (sekä localhost että ulkoinen portti)
			// Joissakin tapauksissa annetaan parametrina INetAddress-olio.
			ServerSocket palvelinsoketti = new ServerSocket(this.port);

			while (true) {

				Socket uusisoketti = palvelinsoketti.accept();
				ExistingClientService uusiyhteys = new ExistingClientService(uusisoketti, this.wup);
				this.yhteydet.add(uusiyhteys);
				// jos käyttää run()-metodia, oltaisiin menty ExistingClientServicen while-silmukkaan ja jääty sinne jumiin
				// kun ajetaan start(), tämä olio käynnistyy omaan säikeeseen
				uusiyhteys.start();
				wup.lisääYhteys(uusiyhteys);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

	}
}
