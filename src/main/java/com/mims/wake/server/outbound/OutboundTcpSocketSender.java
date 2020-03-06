// [YPK]
package com.mims.wake.server.outbound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mims.wake.common.PushConstant;
import com.mims.wake.common.PushMessage;
import com.mims.wake.common.PushMessageDecoder;
import com.mims.wake.common.PushMessageEncoder;
import com.mims.wake.server.property.PushServiceProperty;
import com.mims.wake.server.queue.OutboundQueueManager;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

/**
 * TCP 통신을 사용하는 Outbound Sender 타입
 */
public class OutboundTcpSocketSender extends OutboundServer {
	private static final Logger LOG = LoggerFactory.getLogger(OutboundTcpSocketSender.class);

	private final PushServiceProperty property; // Push Service property
	private final OutboundQueueManager outboundQueueManager; // OutboundQueue 인스턴스 관리자

	private static final String DEFAULT_INBOUND_SERVER_HOST = "127.0.0.1";
	private static final int DEFAULT_INBOUND_SERVER_PORT = 13101;

	/**
	 * constructor with parameters
	 * 
	 * @param property             Push Service property
	 * @param outboundQueueManager OutboundQueue 인스턴스 관리자
	 */
	public OutboundTcpSocketSender(PushServiceProperty property, OutboundQueueManager outboundQueueManager) {
		super(property);
		this.property = property;
		this.outboundQueueManager = outboundQueueManager;
	}

	@Override
	public void startup() {
		LOG.info("[OutboundTcpSocketSender:{}] starting...", property.getServiceId());
		// do nothing
	}

	/**
	 * TCP Socket 통신용 이벤트 핸들러를 설정하는 ChannelInitializer 인스턴스를 생성한다.<br>
	 * 
	 * @return ChannelInitializer 인스턴스
	 * @see chess.push.server.outbound.OutboundServer#getChannelInitializer()
	 */
	@Override
	protected ChannelInitializer<SocketChannel> getChannelInitializer() {
		return new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel socketChannel) {
				ChannelPipeline pipeline = socketChannel.pipeline();
				pipeline.addLast(new DelimiterBasedFrameDecoder(Integer.MAX_VALUE, PushConstant.DEFAULT_DELIMITER));
				pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8), new StringEncoder(CharsetUtil.UTF_8));
				pipeline.addLast(new PushMessageDecoder(), new PushMessageEncoder(PushConstant.DEFAULT_DELIMITER_STR));
				pipeline.addLast(new OutboundServerHandler(property, outboundQueueManager));
			}
		};
	}

	@Override
	public void send(PushMessage msg) throws Exception {
		String serviceId = msg.getServiceId();
		String groupId = msg.getGroupId();
		String clientId = msg.getClientId();

		String inboundServerHost = System.getProperty("inboundServerHost", DEFAULT_INBOUND_SERVER_HOST);
		int inboundServerPort = Integer
				.parseInt(System.getProperty("inboundServerPort", String.valueOf(DEFAULT_INBOUND_SERVER_PORT)));

		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
					pipeline.addLast(new PushMessageEncoder(PushConstant.DEFAULT_DELIMITER_STR));
					pipeline.addLast(new TcpSocketSenderHandler());
				}
			});

			ChannelFuture future = bootstrap.connect(inboundServerHost, inboundServerPort);

			future.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					PushMessage message = new PushMessage(serviceId, groupId, clientId, msg.getMessage());
					future.channel().writeAndFlush(message);
					Thread.sleep(10L);
				}
			}).addListener(ChannelFutureListener.CLOSE).sync();

		} finally {
			group.shutdownGracefully();
		}
	}
}

class TcpSocketSenderHandler extends SimpleChannelInboundHandler<PushMessage> {

	private static final Logger LOG = LoggerFactory.getLogger(TcpSocketSenderHandler.class);

	@Override
	public void channelRead0(ChannelHandlerContext ctx, PushMessage msg) throws Exception {
		// do nothing
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		LOG.error("[TestTcpSocketSenderHandler] error " + ctx.channel() + ", it will be closed", cause);
		ctx.close();
	}
}
