package com.netiq.websockify;

import java.io.PrintStream;

import com.netiq.websockify.WebsockifyClient.SSLSetting;

public class Websockify {

	private boolean showHelp = false;

	private boolean enableSSL = false;

	private boolean requireSSL = false;

	private String webDirectory = null;

	private String keystore = null;

	private String keystorePassword = null;

	private String keystoreKeyPassword = null;

	private int directProxyTimeout = 5000;
	
	private int sourcePort = 8000;

	private String vncHost = "127.0.0.1";

	private int vncPort = 5900;

	public Websockify() {
	}

	public void printUsage(PrintStream out) {
		out.println("Usage:");
		out.println(" java -jar websockify.jar [options] source_port target_addr target_port");
		out.println();
		out.println("Options:");
		out.println();
		out.println("Example:");
		out.println(" java -jar websockify.jar 5900 server.example.net 5900");
	}

	public static void main(String[] args) throws Exception {
		new Websockify().doMain(args);
	}

	public void doMain(String[] args) throws Exception {

		if (showHelp) {
			printUsage(System.out);
			return;
		}

		SSLSetting sslSetting = SSLSetting.OFF;
		if (requireSSL)
			sslSetting = SSLSetting.REQUIRED;
		else if (enableSSL)
			sslSetting = SSLSetting.ON;

		if (sslSetting != SSLSetting.OFF) {
			if (keystore == null || keystore.isEmpty()) {
				System.err.println("No keystore specified.");
				printUsage(System.err);
				System.exit(1);
			}

			if (keystorePassword == null || keystorePassword.isEmpty()) {
				System.err.println("No keystore password specified.");
				printUsage(System.err);
				System.exit(1);
			}

			if (keystoreKeyPassword == null || keystoreKeyPassword.isEmpty()) {
				keystoreKeyPassword = keystorePassword;
			}

			try {
				WebsockifySslContext.validateKeystore(keystore, keystorePassword, keystoreKeyPassword);
			} catch (Exception e) {
				System.err.println("Error validating keystore: " + e.getMessage());
				printUsage(System.err);
				System.exit(2);
			}
		}

		System.out.println("Websockify Proxying *:" + sourcePort + " to " + vncHost + ':' + vncPort + " ...");
		if (sslSetting != SSLSetting.OFF)
			System.out.println("SSL is " + (sslSetting == SSLSetting.REQUIRED ? "required." : "enabled."));

//		PortUnificationHandler.setConnectionToFirstMessageTimeout(directProxyTimeout);

		WebsockifyClient wsc = new WebsockifyClient();
		wsc.connect(sourcePort, vncHost, vncPort, sslSetting, keystore, keystorePassword, keystoreKeyPassword, webDirectory);

	}

}
