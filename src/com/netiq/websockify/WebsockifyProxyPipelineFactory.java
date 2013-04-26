package com.netiq.websockify;

import static org.jboss.netty.channel.Channels.pipeline;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;

import com.netiq.websockify.WebsockifyClient.SSLSetting;

public class WebsockifyProxyPipelineFactory implements ChannelPipelineFactory {

	private final ClientSocketChannelFactory vnccscf;
	private final IProxyTargetResolver resolver;
	private final WebSocketClientHandshaker handshaker;
	private final SSLSetting sslSetting;
	private final String keystore;
	private final String keystorePassword;
	private final String keystoreKeyPassword;
	private final String webDirectory;

	public WebsockifyProxyPipelineFactory(ClientSocketChannelFactory vnccscf, IProxyTargetResolver resolver, WebSocketClientHandshaker handshaker, SSLSetting sslSetting, String keystore, String keystorePassword, String keystoreKeyPassword, String webDirectory) {
		this.vnccscf = vnccscf;
		this.resolver = resolver;
		this.handshaker = handshaker;
		this.sslSetting = sslSetting;
		this.keystore = keystore;
		this.keystorePassword = keystorePassword;
		this.keystoreKeyPassword = keystoreKeyPassword;
		this.webDirectory = webDirectory;
	}

	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline p = pipeline(); // Note the static import.

		p.addLast("decoder", new HttpResponseDecoder());
		p.addLast("encoder", new HttpRequestEncoder());
		// p.addLast("unification", new PortUnificationHandler(vnccscf,
		// resolver, sslSetting, keystore, keystorePassword,
		// keystoreKeyPassword, webDirectory));
		p.addLast("handler", new WebsockifyProxyHandler(vnccscf, resolver, handshaker, webDirectory));
		return p;

	}

}
