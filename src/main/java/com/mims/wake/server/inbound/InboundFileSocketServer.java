// [YPK]
package com.mims.wake.server.inbound;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mims.wake.server.queue.InboundQueue;

import java.util.Timer;
import java.util.TimerTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class InboundFileSocketServer {
	private static final Logger LOG = LoggerFactory.getLogger(InboundFileSocketServer.class);

	private int interval;
	private String filePath;
	private Timer timer;

	public InboundFileSocketServer(int interval, String filePath) {
		this.interval = interval;
		this.filePath = filePath;
	}

	public void startup(Map<String, InboundQueue> inboundQueues) {
		LOG.info("[InboundFile] starting...");

		try {
			this.timer = new Timer();

			TimerTask timerTask = new TimerTask() {
				@Override
				public void run() {
					System.out.println("[InboundFile] Scan");
					fileWrite();
				}
			};

			this.timer.schedule(timerTask, 0, this.interval);
			LOG.info("[InboundFile] started, file paht  " + this.filePath);

		} catch (Exception e) {
			LOG.error("[InboundFile] failed to startup", e);
			shutdown();
		}
	}

	public void shutdown() {
		this.timer.cancel();
		LOG.info("[InboundFile] shutdown");
	}

	public void fileWrite() {
		try {
			String workingPath = System.getProperty("user.dir") + filePath;
			String workingFile = workingPath + "/msg.json";

			// 파일 객체 생성
			File file = new File(workingFile);
			if (file.exists() == false)
				return;

			// 입력 스트림 생성
			FileReader fileReader = new FileReader(file);
			// 입력 버퍼 생성
			BufferedReader bufReader = new BufferedReader(fileReader);
			String line = "";
			while ((line = bufReader.readLine()) != null) {
				System.out.println(line);
			}
			// .readLine()은 끝에 개행문자를 읽지 않는다.
			bufReader.close();
			fileReader.close();
		} catch (FileNotFoundException e) {
			e.getStackTrace();
		} catch (IOException e) {
			System.out.println(e);
		}
	}
}
