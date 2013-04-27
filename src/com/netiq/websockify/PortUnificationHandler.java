/*
 * Copyright 2011 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.netiq.websockify;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import javax.net.ssl.SSLEngine;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.ssl.SslHandler;

import com.netiq.websockify.WebsockifyClient.SSLSetting;

/**
 * Manipulates the current pipeline dynamically to switch protocols or enable
 * SSL or GZIP.
 */
public class PortUnificationHandler extends FrameDecoder {
	protected static long connectionToFirstMessageTimeout = 5000;

	private final ClientSocketChannelFactory vnccscf;
	private final IProxyTargetResolver resolver;
	private final SSLSetting sslSetting;
	private final String keystore;
	private final String keystorePassword;
	private final String keystoreKeyPassword;
	private final String webDirectory;
	private Timer msgTimer = null;
	private long directConnectTimerStart = 0;

	private PortUnificationHandler(ClientSocketChannelFactory cf, IProxyTargetResolver resolver, SSLSetting sslSetting, String keystore, String keystorePassword, String keystoreKeyPassword, String webDirectory, final ChannelHandlerContext ctx) {
		this(cf, resolver, sslSetting, keystore, keystorePassword, keystoreKeyPassword, webDirectory);
//		startDirectConnectionTimer(ctx);
	}

	public PortUnificationHandler(ClientSocketChannelFactory vnccscf, IProxyTargetResolver resolver, SSLSetting sslSetting, String keystore, String keystorePassword, String keystoreKeyPassword, String webDirectory) {
		this.vnccscf = vnccscf;
		this.resolver = resolver;
		this.sslSetting = sslSetting;
		this.keystore = keystore;
		this.keystorePassword = keystorePassword;
		this.keystoreKeyPassword = keystoreKeyPassword;
		this.webDirectory = webDirectory;
	}

//	public static long getConnectionToFirstMessageTimeout() {
//		return connectionToFirstMessageTimeout;
//	}
//
//	public static void setConnectionToFirstMessageTimeout(long connectionToFirstMessageTimeout) {
//		PortUnificationHandler.connectionToFirstMessageTimeout = connectionToFirstMessageTimeout;
//	}

	// In cases where there will be a direct VNC proxy connection
	// The client won't send any message because VNC servers talk first
	// So we'll set a timer on the connection - if there's no message by the
	// time
	// the timer fires we'll create the proxy connection to the target
	@Override
	public void channelOpen(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		System.out.println(1);
//		startDirectConnectionTimer(ctx);
		System.out.println(2);
	}

//	private void startDirectConnectionTimer(final ChannelHandlerContext ctx) {
//		System.out.println(3);
//		// cancel any outstanding timer
//		cancelDirectConnectionTimer();
//
//		// direct proxy connection disabled
//		if (connectionToFirstMessageTimeout <= 0)
//			return;
//
//		directConnectTimerStart = System.currentTimeMillis();
//
//		// cancelling a timer makes it unusable again, so we have to create
//		// another one
//		msgTimer = new Timer();
//		msgTimer.schedule(new TimerTask() {
//
//			@Override
//			public void run() {
//				switchToDirectProxy(ctx);
//			}
//
//		}, connectionToFirstMessageTimeout);
//		System.out.println(4);
//
//	}

	private void cancelDirectConnectionTimer() {
		if (directConnectTimerStart > 0) {
			long directConnectTimerCancel = System.currentTimeMillis();
			Logger.getLogger(PortUnificationHandler.class.getName()).finer("Direct connection timer canceled after " + (directConnectTimerCancel - directConnectTimerStart) + " milliseconds.");
		}

		if (msgTimer != null) {
			msgTimer.cancel();
			msgTimer = null;
		}

	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
		System.out.println(11);
		// Will use the first two bytes to detect a protocol.
		if (buffer.readableBytes() < 2) {
			return null;
		}

		cancelDirectConnectionTimer();

		final int magic1 = buffer.getUnsignedByte(buffer.readerIndex());
		final int magic2 = buffer.getUnsignedByte(buffer.readerIndex() + 1);

		if (isSsl(magic1)) {
			enableSsl(ctx);
		} else if (isFlashPolicy(magic1, magic2)) {
			switchToFlashPolicy(ctx);
		} else {
			switchToWebsocketProxy(ctx);
		}

		System.out.println(12);
		// Forward the current read buffer as is to the new handlers.
		return buffer.readBytes(buffer.readableBytes());

	}

	private boolean isSsl(int magic1) {
		if (sslSetting != SSLSetting.OFF) {
			switch (magic1) {
			case 20:
			case 21:
			case 22:
			case 23:
			case 255:
				return true;
			default:
				return magic1 >= 128;
			}
		}
		return false;
	}

	private boolean isFlashPolicy(int magic1, int magic2) {
		return (magic1 == '<' && magic2 == 'p');
	}

	private void enableSsl(ChannelHandlerContext ctx) {
		ChannelPipeline p = ctx.getPipeline();

		Logger.getLogger(PortUnificationHandler.class.getName()).fine("SSL request from " + ctx.getChannel().getRemoteAddress() + ".");

		SSLEngine engine = WebsockifySslContext.getInstance(keystore, keystorePassword, keystoreKeyPassword).getServerContext().createSSLEngine();
		engine.setUseClientMode(false);

		p.addLast("ssl", new SslHandler(engine));
		p.addLast("unificationA", new PortUnificationHandler(vnccscf, resolver, SSLSetting.OFF, keystore, keystorePassword, keystoreKeyPassword, webDirectory, ctx));
		p.remove(this);
	}

	private void switchToWebsocketProxy(ChannelHandlerContext ctx) {
		ChannelPipeline p = ctx.getPipeline();

		Logger.getLogger(PortUnificationHandler.class.getName()).fine("Websocket proxy request from " + ctx.getChannel().getRemoteAddress() + ".");
		System.out.println("webby");
		// p.addLast("decoder", new HttpRequestDecoder());
		// p.addLast("aggregator", new HttpChunkAggregator(65536));
		// p.addLast("encoder", new HttpResponseEncoder());
		// p.addLast("chunkedWriter", new ChunkedWriteHandler());
//		p.addLast("handler", new WebsockifyProxyHandler(vnccscf, resolver, webDirectory));
		p.remove(this);
	}

	private void switchToFlashPolicy(ChannelHandlerContext ctx) {
		ChannelPipeline p = ctx.getPipeline();

		Logger.getLogger(PortUnificationHandler.class.getName()).fine("Flash policy request from " + ctx.getChannel().getRemoteAddress() + ".");

		p.addLast("flash", new FlashPolicyHandler());

		p.remove(this);
	}

	private void switchToDirectProxy(ChannelHandlerContext ctx) {
		System.out.println(5);
		ChannelPipeline p = ctx.getPipeline();

		Logger.getLogger(PortUnificationHandler.class.getName()).fine("Direct proxy request from " + ctx.getChannel().getRemoteAddress() + ".");

		p.addLast("proxy", new DirectProxyHandler(ctx.getChannel(), vnccscf, resolver));

		p.remove(this);
		System.out.println(6);
	}

	// cancel the timer if channel is closed - prevents useless stack traces
	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		System.out.println(7);
		cancelDirectConnectionTimer();
		System.out.println(8);
	}

	// cancel the timer if exception is caught - prevents useless stack traces
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		cancelDirectConnectionTimer();
		e.getCause().printStackTrace();
		Logger.getLogger(PortUnificationHandler.class.getName()).severe("Exception on connection to " + ctx.getChannel().getRemoteAddress() + ": " + e.getCause().getMessage());
	}
}
