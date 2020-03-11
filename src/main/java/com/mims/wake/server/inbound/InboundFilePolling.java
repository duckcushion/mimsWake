// [YPK]
package com.mims.wake.server.inbound;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mims.wake.common.PushMessage;
import com.mims.wake.server.property.PushBaseProperty;
import com.mims.wake.server.property.ServiceType;
import com.mims.wake.server.queue.InboundQueue;
import com.mims.wake.util.commonUtil;

public class InboundFilePolling {
	private static final Logger LOG = LoggerFactory.getLogger(InboundFilePolling.class);

	private int interval;
	private String pathFile;
	private Timer timer;
	private Map<String, InboundQueue> inboundQueues;

	public InboundFilePolling(PushBaseProperty property) {
		this.interval = 1000;
		this.pathFile = getPathFile(property.getOutboundServerWsUri());
		this.setInboundQueues(null);
	}

	public void startup(Map<String, InboundQueue> inboundQueues) {
		try {
			this.timer = new Timer();
			this.setInboundQueues(inboundQueues);

			TimerTask timerTask = new TimerTask() {
				@Override
				public void run() {
					fileRead();
				}
			};

			this.timer.schedule(timerTask, 0, this.interval);
			LOG.info("[InboundFilePolling] started, file paht " + this.pathFile);

		} catch (Exception e) {
			LOG.error("[InboundFilePolling] failed to startup", e);
			shutdown();
		}
	}
	

	public void fileRead() {
		try {
			Vector<String> arrFile = commonUtil.getFileNames(pathFile, "json");
			for (int ix = 0; ix < arrFile.size(); ++ix) {
				String pathFile = arrFile.get(ix);

				File file = new File(pathFile);
				if (file.exists() == false)
					continue;

				// parsing gid and cid
				String fname = file.getName();
				int pos = fname.lastIndexOf(".");
				fname = fname.substring(0, pos);
				pos = fname.lastIndexOf("_");
				String groupId = fname.substring(0, pos);
				String clientId = fname.substring(pos + 1, fname.length());
				
				// read message
				String msg = "";
				FileReader fileReader = new FileReader(file);
				BufferedReader bufReader = new BufferedReader(fileReader);
				String buff = "";
				while ((buff = bufReader.readLine()) != null) {
					msg += buff;
				}
				bufReader.close();
				fileReader.close();
				file.delete(); // read only once

//				// send to websocket
//				String serviceId = ServiceType.WEBSOCKET;
//				PushMessage pushMsgWeb = new PushMessage(serviceId, groupId, clientId, msg);
//				inboundQueues.get(serviceId).enqueue(pushMsgWeb);
//
//				// send to tcpsocket
//				serviceId = ServiceType.TCPSOCKET;
//				PushMessage pushMsgTcp = new PushMessage(serviceId, groupId, clientId, msg);
//				inboundQueues.get(serviceId).enqueue(pushMsgTcp);
				
				PushMessage pushMsg = new PushMessage("", groupId, clientId, msg);
				inboundQueues.forEach((sid, queue) -> {
					queue.enqueue(new PushMessage(sid, pushMsg.getGroupId(), pushMsg.getClientId(), pushMsg.getMessage()));
				});

				LOG.info("[========== InboundFilePolling ==========] Scan => Push : {}", msg);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void shutdown() {
		if (this.timer != null) {
			this.timer.cancel();
		}
		LOG.info("[InboundFilePolling] shutdown");
	}

	public Map<String, InboundQueue> getInboundQueues() {
		return inboundQueues;
	}

	public void setInboundQueues(Map<String, InboundQueue> inboundQueues) {
		this.inboundQueues = inboundQueues;
	}
	
	private String getPathFile(String subPath) {
		String token = commonUtil.pathToken();
		return System.getProperty("user.dir") + token + subPath;
	}
}
