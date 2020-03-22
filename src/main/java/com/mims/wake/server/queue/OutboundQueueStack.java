package com.mims.wake.server.queue;

import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mims.wake.common.PushMessage;
import com.mims.wake.server.property.PushServiceProperty;
import com.mims.wake.server.property.ServiceType;

import io.netty.channel.Channel;

public class OutboundQueueStack extends Thread {
	private static final Logger LOG = LoggerFactory.getLogger(OutboundQueueStack.class);

    private final BlockingQueue<PushMessage> queue;		// message queue
    private Vector<Channel> channels;					// message push channel
    private Vector<PushMessage> stack;					// message stack storage
    private int maxTcpConnNumber;						// maximum TCP connection number
    private int realTcpConnCount;						// real TCP connection count
    private int stackClearTime;							// stack clear time

	/**
	 * constructor with parameters
	 * 
	 */
	public OutboundQueueStack(int capacity) {
		this.queue = new LinkedBlockingQueue<PushMessage>(capacity);
		this.channels = new Vector<Channel>();
		this.stack = new Vector<PushMessage>();
        this.maxTcpConnNumber = 1;
        this.realTcpConnCount = 0;
	}
	
	/**
	 * set property
	 * 
	 * @param capacity 			: stack capacity
	 * @param tcpConnMaxNum		: maximum TCP connection number
	 */
	public void setProperty(PushServiceProperty property) {
        this.maxTcpConnNumber = property.getOutboundConnectionNumber();
        this.stackClearTime = property.getOutboundQueueClearTime();
	}
	
	/**
	 * connection socket
	 * 
	 */
	public void connection(String serviceId) {
		if (serviceId.equals(ServiceType.TCPSOCKET)) {
			if (realTcpConnCount < maxTcpConnNumber)
				++realTcpConnCount;
		}
	}
	
	/**
	 * disconnection TCP
	 * 
	 */
	public void disconnection(String serviceId) {
		if (serviceId.equals(ServiceType.TCPSOCKET)) {
			if (realTcpConnCount > 0)
				--realTcpConnCount;
		}
	}

	/**
	 * 미전송 메시지 보관
	 * 
	 * @param msg : push message
	 */
	public void pushStack(PushMessage msg) {
		String serviceId = msg.getServiceId();
		if (serviceId.equals(ServiceType.TCPSOCKET)) {
			if (realTcpConnCount < maxTcpConnNumber)
				stack.add(msg);
		}
		else
			stack.add(msg);
	}

	/**
	 * 미전송 메세지 꺼내기
	 * 
	 * @param serviceId    : service id
	 */
	public void popStack(String serviceId, Channel channel) {
		try {
			if (serviceId == null || serviceId.isEmpty() || channel == null)
				throw new Exception();
			
			connection(serviceId);
			
			Vector<PushMessage> stock = new Vector<PushMessage>();
			for(int ix=0; ix < stack.size(); ++ix) {
				PushMessage msg = stack.get(ix);
				String sid = msg.getServiceId();
				if (sid.equals(serviceId)) {
					channels.add(channel);
					queue.offer(msg);
					retentionMsg(serviceId, msg, stock);
				}
				else
					stock.add(msg);
			}
			stack = stock;
		} catch (Exception e) {
			LOG.error("[OutboundQueueStack popStack] >>>>>");
		}
	}
	
	/**
	 * TCP에 연결할 Client가 남아 있으면 메세지 보존
	 * 
	 * @param serviceId	: service id
	 * @param msg		: push message	
	 */
	public void retentionMsg(String serviceId, PushMessage msg, Vector<PushMessage> stock) {
		if(serviceId.equals(ServiceType.TCPSOCKET)) {
			if(realTcpConnCount < maxTcpConnNumber)
				stock.add(msg);
		}
	}
	
    /**
     * 쓰레드 종료
     */
    public void shutdown() {
        this.interrupt();
    }
    
    /**
     * 큐에서 메시지를 추출하여 클라이언트 채널에 전송
     * @see java.lang.Thread#run()
     */
    @Override
	public void run() {
		while (!isInterrupted()) {
			PushMessage message = null;
			try {
				message = queue.take();
			} catch (InterruptedException e) {
				break;
			}
			
			if (message != null) {
				PushMessage msg = new PushMessage(message);
				channels.forEach((channel) -> {
					channel.writeAndFlush(msg);
					LOG.info("[{}] [{}] [{}] Pop Stack {}", getName(), msg.getServiceId(), msg.getClientId(), msg);
				});
				channels.clear();
			}
		}
	}
}
