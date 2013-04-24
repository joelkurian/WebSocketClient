package com.netiq.websockify;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;

public class WebsockifyClient {
	private Executor executor;
	private ClientBootstrap wbcb;
	private ClientSocketChannelFactory vnccscf;
	private Channel serverChannel = null;

	public enum SSLSetting {
		OFF, ON, REQUIRED
	};

	public WebsockifyClient() {
		// Configure the bootstrap.
		executor = Executors.newCachedThreadPool();
		wbcb = new ClientBootstrap(new NioClientSocketChannelFactory(executor, executor));

		// Set up the event pipeline factory.
		vnccscf = new NioClientSocketChannelFactory(executor, executor);
	}

	public void connect(int localPort, String remoteHost, int remotePort) {
		connect(localPort, remoteHost, remotePort, null);
	}

	public void connect(int localPort, String remoteHost, int remotePort, String webDirectory) {
		connect(localPort, remoteHost, remotePort, SSLSetting.OFF, null, null, null, webDirectory);
	}

	public void connect(int localPort, String remoteHost, int remotePort, SSLSetting sslSetting, String keystore, String keystorePassword, String keystoreKeyPassword, String webDirectory) {
		connect(localPort, new StaticTargetResolver(remoteHost, remotePort), sslSetting, keystore, keystorePassword, keystoreKeyPassword, webDirectory);
	}

	public void connect(int localPort, IProxyTargetResolver resolver) {
		connect(localPort, resolver, null);
	}

	public void connect(int localPort, IProxyTargetResolver resolver, String webDirectory) {
		connect(localPort, resolver, SSLSetting.OFF, null, null, null, webDirectory);
	}

	public void connect(int localPort, IProxyTargetResolver resolver, SSLSetting sslSetting, String keystore, String keystorePassword, String keystoreKeyPassword, String webDirectory) {
		if (serverChannel != null) {
			close();
		}
		Channel ch = null;
		try {
			URI uri = new URI("ws://localhost:8000");
            HashMap<String, String> customHeaders = new HashMap<String, String>();
//            customHeaders.put("Sec-WebSocket-Extensions", "x-webkit-deflate-frame");
            customHeaders.put("server", "vncserver");
			final WebSocketClientHandshaker handshaker = new WebSocketClientHandshakerFactory().newHandshaker(uri, WebSocketVersion.V13, "binary, base64", false, customHeaders);
			wbcb.setPipelineFactory(new WebsockifyProxyPipelineFactory(vnccscf, resolver, sslSetting, keystore, keystorePassword, keystoreKeyPassword, webDirectory));
			// cb.setPipelineFactory(new ChannelPipelineFactory() {
			// public ChannelPipeline getPipeline() throws Exception {
			// ChannelPipeline pipeline = Channels.pipeline();
			//
			// pipeline.addLast("decoder", new HttpResponseDecoder());
			// pipeline.addLast("encoder", new HttpRequestEncoder());
			// pipeline.addLast("ws-handler", new
			// WebSocketClientHandler(handshaker));
			// return pipeline;
			// }
			// });
			// Connect
			System.out.println("WebSocket Client connecting");
			ChannelFuture future = wbcb.connect(new InetSocketAddress(uri.getHost(), uri.getPort()));
			future.syncUninterruptibly();

			ch = future.getChannel();
			try {
				handshaker.handshake(ch).syncUninterruptibly();
				System.out.println("Haha - Connected....");
			} catch (Exception e) {
				e.printStackTrace();
			}

			ch.write(new TextWebSocketFrame("yahoo"));

		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

	}

	public void close() {
		if (serverChannel != null && serverChannel.isBound()) {
			serverChannel.close();
			serverChannel = null;
		}
	}

	public Channel getChannel() {
		return serverChannel;
	}

	/**
	 * Validates that a keystore with the given parameters exists and can be
	 * used for an SSL context.
	 * 
	 * @param keystore
	 *            - path to the keystore file
	 * @param password
	 *            - password to the keystore file
	 * @param keyPassword
	 *            - password to the private key in the keystore file
	 * @return null if valid, otherwise a string describing the error.
	 */
	public void validateKeystore(String keystore, String password, String keyPassword) throws KeyManagementException, UnrecoverableKeyException, IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException {
		WebsockifySslContext.validateKeystore(keystore, password, keyPassword);
	}

}
