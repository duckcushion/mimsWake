package com.mims.wake.server.inbound;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mims.wake.common.PushMessage;
import com.mims.wake.server.kmtf.Field;
import com.mims.wake.server.kmtf.KmtfMessage;
import com.mims.wake.server.kmtf.Set;
import com.mims.wake.server.kmtf.kmtfParser;
import com.mims.wake.server.property.ServiceType;
import com.mims.wake.server.queue.InboundQueue;
import com.mims.wake.util.JsonUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Inbound Server와 송신자간 연결된 채널에서 발생하는 이벤트 처리용 핸들러
 */
public class InboundTcpSocketServerMsgHandler extends SimpleChannelInboundHandler<ByteBuf> {

	private static final Logger logger = LogManager.getLogger(InboundTcpSocketServerMsgHandler.class);

	private final Map<String, InboundQueue> inboundQueues; // Inbound Queue collection

	/**
	 * constructor with a parameter
	 * 
	 * @param inboundQueues Inbound Queue collection
	 */
	public InboundTcpSocketServerMsgHandler(Map<String, InboundQueue> inboundQueues) {
		this.inboundQueues = inboundQueues;
	}

	/**
	 * 클라이언트와 채널이 연결되어 사용 가능한 상태가 되었을 때 동작<br>
	 * -연결 정보 로깅
	 * 
	 * @param ctx ChannelHandlerContext object
	 * @see io.netty.channel.ChannelInboundHandlerAdapter#channelActive(io.netty.channel.ChannelHandlerContext)
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		logger.info("[InboundServerHandler] connected {}", ctx.channel());

		try {
			logger.info("[{}] Welcom to [{}]", new Date(), InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		ctx.fireChannelActive();
	}

	/**
	 * 클라이언트로부터 메시지 수신했을 때 동작<br>
	 * -Service ID에 해당하는 InboundQueue에 추가
	 * 
	 * @param ctx ChannelHandlerContext object
	 * @param msg 수신된 메시지
	 * @see io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty.channel.ChannelHandlerContext,
	 *      java.lang.Object)
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
		// LOG.info("[InboundServerHandler] received {} from {}", msg.toString(),
		// ctx.channel());
		logger.info("[InboundServerHandler] FROM {}", ctx.channel());

		ByteBuf req = (ByteBuf) msg;
		String content = req.toString(Charset.defaultCharset());

		/*
		 * 01. [Client] Web Browser 에게 메시지를 전달 - Service ID is "client.websocket"
		 */

		// [Client] ServiceID
		String serviceId = "client.websocket";

		// [Client] KMTF parse
		PushMessage pushMsg = new PushMessage();

		KmtfMessage message;
		try {
			message = kmtfParser.parseFormat(content);
			if (message.getKmtfId() == null) {
				
				// [+] [YPK] Receive JSON
				ObjectMapper mapper = new ObjectMapper();
				Map<String, String> mapJson = mapper.readValue(content, new TypeReference<Map<String, String>>() {
				});
				serviceId = mapJson.get("serviceId");
				if (serviceId != null && serviceId.contains(ServiceType.TCPSOCKET)) {
						logger.info("[Receive JSON from Outbound Server] >>>>>>>>>> {}", content);
					pushMsg.setServiceId(serviceId);
					pushMsg.setGroupId(mapJson.get("groupId"));
					pushMsg.setClientId(mapJson.get("clientId"));
					pushMsg.setMessage(mapJson.get("message"));
				} else {
					return;
				}
				// [-]
					
			} else {
				
				System.out.println("------------------");
				System.out.println("kmtfId : " + message.getKmtfId());
				System.out.println("setId : " + message.getSetId());
				System.out.println("createTime : " + message.getCreateTime());
				System.out.println("sourceSystemId : " + message.getSourceSystemId());
				System.out.println("destnationSystemId : " + message.getDestnationSystemId());
				System.out.println("messageId : " + message.getMessageId());
				System.out.println("cudm : " + message.getCudm());
				System.out.println("mode : " + message.getMode());
				System.out.println("version : " + message.getVersion());
				System.out.println("msgSeq : " + message.getMsgSeq());
				System.out.println("------------------");

				List<Set> setList = message.getSetList();

				for (Set s : setList) {
					LinkedHashMap<Integer, Field> map = s.getFieldMap();
					for (Object key : map.keySet()) {
						Field ff = map.get(key);
						System.out.println(
								s.getSid() + " | " + ff.getIndex() + " | " + ff.getName() + " | " + ff.getValue());
					}
					System.out.println("");
				}

				// System.out.println(message.getData());
				// System.out.println(JsonUtil.getJsonStringFromList(message.getData()));

				// [Client] PushMessage Setting
				pushMsg.setServiceId(serviceId);
				pushMsg.setGroupId(message.getMode());
				pushMsg.setClientId(message.getSetId());
				pushMsg.setMessage(JsonUtil.getJsonStringFromList(message.getData()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//

		// LOG.info("[InboundServerHandler] RECEIVED {} ", content);
		//serviceId = ServiceType.WEBSOCKET;
		//pushMsg.setServiceId(serviceId);
		//inboundQueues.get(serviceId).enqueue(pushMsg);

		/*
		 * 03. [DB] Database 에 메시지 저장
		 */
		
		// [YPK] 모든 Service 메세지 전달 
		inboundQueues.forEach((sid, queue) -> {
			queue.enqueue(new PushMessage(sid, pushMsg.getGroupId(), pushMsg.getClientId(), pushMsg.getMessage()));
			
			// 실제모드(OPER) 또는 연습모드(EXER) 전체를 Client에 전송하기 위해 Queue에 하나 더 전달
			// ex) "OPER"로 접속된 Client에게는 공중항적(A2R), 해상항적(S2R)를 모두 전달
			if ("OPER".equals(pushMsg.getGroupId())) {
				queue.enqueue(new PushMessage(sid, pushMsg.getGroupId(), "OPER", pushMsg.getMessage()));
			} else if ("EXER".equals(pushMsg.getGroupId())) {
				queue.enqueue(new PushMessage(sid, pushMsg.getGroupId(), "EXER", pushMsg.getMessage()));
			}
        });
	}

	/**
	 * 클라이언트와 채널이 해제되어 사용 불가능한 상태가 되었을 때 동작<br>
	 * -연결해제 정보 로깅
	 * 
	 * @param ctx ChannelHandlerContext object
	 * @see io.netty.channel.ChannelInboundHandlerAdapter#channelInactive(io.netty.channel.ChannelHandlerContext)
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.info("[InboundServerHandler] DISCONNECTED {}", ctx.channel());
	}

	/**
	 * 채널의 I/O 오퍼레이션 도중 예외가 발생했을 때 동작<br>
	 * -예외 정보 로깅<br>
	 * -채널 연결해제
	 * 
	 * @param ctx   ChannelHandlerContext object
	 * @param cause 발생한 예외
	 * @see io.netty.channel.ChannelInboundHandlerAdapter#exceptionCaught(io.netty.channel.ChannelHandlerContext,
	 *      java.lang.Throwable)
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		logger.error("[InboundServerHandler] ERROR " + ctx.channel() + ", it will be closed", cause);
		ctx.close();
	}

}
