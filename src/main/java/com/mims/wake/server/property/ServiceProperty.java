package com.mims.wake.server.property;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class ServiceProperty  implements InitializingBean, DisposableBean {
    private String serviceId;					// Push Service ID
    private String inboundQueueCapacity;		// Inbound Message Queue capacity
    private String outboundQueueCapacity;		// Outbound Message Queue capacity
    private String outboundServerPort;			// Outbound Server listen port
    private ServerType outboundServerType;		// Outbound Server communication type
    private String outboundServerWsUri;			// Inbound, Outbound Server TcpSocket IP / Outbound Server FileSocket SubPath
    											// Outbound Server WebSocket URI, if Outbound Server type is WEBSOCKET
    @PostConstruct
    public void afterPropertiesSet() {
        if (serviceId == null) {
            throw new IllegalArgumentException("The 'serviceId' property is null");
        }
        if (Integer.parseInt(inboundQueueCapacity) <= 0) {
            throw new IllegalArgumentException("The 'inboundQueueCapacity' property is invalid [" + inboundQueueCapacity + "]");
        }
        if (Integer.parseInt(outboundQueueCapacity) <= 0) {
            throw new IllegalArgumentException("The 'inboundQueueCapacity' property is invalid [" + outboundQueueCapacity + "]");
        }
        if (Integer.parseInt(outboundServerPort) <= 0) {
            throw new IllegalArgumentException("The 'outboundServerPort' property is invalid [" + outboundServerPort + "]");
        }
        if (outboundServerType == null) {
            throw new IllegalArgumentException("The 'outboundServerType' property is null");
        }
        if (outboundServerWsUri == null) {
            throw new IllegalArgumentException("The 'outboundServerWsUri' property is null");
        }
    }

    public String getServiceId() {
        return serviceId;
    }
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public int getInboundQueueCapacity() {
        return Integer.parseInt(inboundQueueCapacity);
    }
    public void setInboundQueueCapacity(int inboundQueueCapacity) {
        this.inboundQueueCapacity = Integer.toString(inboundQueueCapacity);
    }

    public int getOutboundQueueCapacity() {
        return Integer.parseInt(outboundQueueCapacity);
    }
    public void setOutboundQueueCapacity(int outboundQueueCapacity) {
        this.outboundQueueCapacity = Integer.toString(outboundQueueCapacity);
    }

    public int getOutboundServerPort() {
        return Integer.parseInt(outboundServerPort);
    }
    public void setOutboundServerPort(int outboundServerPort) {
        this.outboundServerPort = Integer.toString(outboundServerPort);
    }

    public ServerType getOutboundServerType() {
        return outboundServerType;
    }
    public void setOutboundServerType(ServerType outboundServerType) {
        this.outboundServerType = outboundServerType;
    }

    public String getOutboundServerWsUri() {
        return outboundServerWsUri;
    }
	public void setOutboundServerWsUri(String outboundServerWsUri) {
		this.outboundServerWsUri = outboundServerWsUri;
		if (this.outboundServerType == ServerType.WEBSOCKET) {
			if (outboundServerWsUri != null && !outboundServerWsUri.startsWith("/"))
				this.outboundServerWsUri = "/" + outboundServerWsUri;
		}
	}

	@Override
	public void destroy() throws Exception {
		// TODO Auto-generated method stub
		
	}
}
