package com.mims.wake.server.queue;

import java.util.HashMap;
import java.util.Map;
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

	private final Map<String, ServiceInfo> 			serviceGroup;			// service socket
    private final BlockingQueue<ChannelInfo> 		queue;					// message queue
    private Vector<PushMessage> 					stack;					// message stack storage

	/**
	 * constructor with parameters
	 * 
	 */
	public OutboundQueueStack(int capacity) {
		this.serviceGroup = new HashMap<String, ServiceInfo>();
		this.queue = new LinkedBlockingQueue<ChannelInfo>(capacity);
		this.stack = new Vector<PushMessage>();
	}
	
    /**
     * queue stack start
     */
    public void startup() {
        this.start();
    }
	
	/**
	 * set property
	 * 
	 * @param prop 			: PushServiceProperty
	 */
	public void setProperty(PushServiceProperty prop) {
		String serviceId = prop.getServiceId();
		if(serviceId.equals(ServiceType.TCPSOCKET) || serviceId.equals(ServiceType.WEBSOCKET)) {
			serviceGroup.put(serviceId, new ServiceInfo(prop, 0));
		}
	}
	
	/**
	 * connection socket
	 * 
	 */
	public void connection(String serviceId) {
		if (!serviceGroup.containsKey(serviceId))
            return;
		ServiceInfo sInfo = serviceGroup.get(serviceId);
		sInfo.increase();
	}
	
	/**
	 * disconnection socket
	 * 
	 */
	public void disconnection(String serviceId) {
		if (!serviceGroup.containsKey(serviceId))
            return;
		ServiceInfo sInfo = serviceGroup.get(serviceId);
		sInfo.decrease();
	}

	/**
	 * 미전송 메시지 보관
	 * 
	 * @param msg : save message
	 */
	public void pushStack(PushMessage msg, boolean isEmpty) {
		String serviceId = msg.getServiceId();
		if (!serviceGroup.containsKey(serviceId))
            return;
		
		if (isEmpty) {
			stack.add(msg);
		} else {
			// 최대 연결 허용 개수만큼 보관
			ServiceInfo sInfo = serviceGroup.get(serviceId);
			if (sInfo.isStock())
				stack.add(msg);
		}
	}

	/**
	 * 미전송 메세지 꺼내기
	 * 
	 * @param serviceId    	: service id
	 * @param channel		: connected channel
	 */
	public void popStack(String serviceId, Channel channel) {
		try {
			if (serviceId == null || serviceId.isEmpty() || channel == null)
				throw new Exception();
			
			if (!serviceGroup.containsKey(serviceId))
	            return;
			
			connection(serviceId);
			ServiceInfo sInfo = serviceGroup.get(serviceId);
			
			Vector<PushMessage> stock = new Vector<PushMessage>();
			for(int ix=0; ix < stack.size(); ++ix) {
				PushMessage msg = stack.get(ix);
				String sid = msg.getServiceId();
				if (sid.equals(serviceId)) {
					queue.offer(new ChannelInfo(channel, msg));
					if (sInfo.isStock())
						stock.add(msg);
				} else
					stock.add(msg);
			}
			stack = stock;
		} catch (Exception e) {
			LOG.error("[OutboundQueueStack popStack] >>>>>");
		}
	}
	
    /**
     * queue stack stop
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
			ChannelInfo cInfo = null;
			try {
				cInfo = queue.take();
			} catch (InterruptedException e) {
				break;
			}
			
			if (cInfo != null) {
				cInfo.sendMessage();
				PushMessage msg = cInfo.getMessage();
				LOG.info("[{}] [{}] [{}] Pop Stack {}", getName(), msg.getServiceId(), msg.getClientId(), msg);
			}
		}
	}
    
    ////////////////////////////////////////////////////////////////////////////////
    // class ServiceInfo
    //
    public class ServiceInfo {
    	private final PushServiceProperty property;
    	private int connectionCount;
    	
    	public ServiceInfo(PushServiceProperty property, int count) {
    		this.property = property;
    		this.connectionCount = count;
    	}
    	
    	public void increase() {
    		if(connectionCount < property.getOutboundConnectionNumber())
    			++connectionCount;
    	}
    	
    	public void decrease() {
    		if(connectionCount > 0)
    			--connectionCount;
    	}
    	
    	public boolean isStock() {
    		return (connectionCount < property.getOutboundConnectionNumber());
    	}
    }    
    
    ////////////////////////////////////////////////////////////////////////////////
    // class ChannelInfo
    //
    public class ChannelInfo {
    	private final Channel channel;
    	private final PushMessage message;
    	
    	public ChannelInfo(Channel channel, PushMessage message) {
    		this.channel = channel;
    		this.message = message;
    	}
    	
    	public void sendMessage() {
    		if(channel != null) {
    			channel.writeAndFlush(message);
    			try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
    		}
    	}
    	
    	public PushMessage getMessage() {
    		return message;
    	}
    }
}
