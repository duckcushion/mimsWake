package com.mims.wake;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.mims.wake.server.Server;
import com.mims.wake.server.property.PushBaseProperty;
import com.mims.wake.server.property.PushServiceProperty;

/**
 * Spring-Boot Start (with Main)
 * @author GenieInBed
 */
@SpringBootApplication
@ImportResource({"classpath*:application-config.xml"})
public class MimsWakeApplication implements CommandLineRunner  {

	private static final Logger logger = LoggerFactory.getLogger(MimsWakeApplication.class);

	/**
	 * MIMS 항적 프로그램(Spring Boot) 시작.
	 * run() 에서 실제 프로그램 시작
	 * 
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		logger.info("■□■□■MIM Wake Websocket Server Start■□■□■");
		SpringApplication.run(MimsWakeApplication.class, args);
		
	}
	
	/**
	 * CommandLineRunner 에 의해 자동 실행
	 *
	 * @param strings
	 * @throws Exception
	 */
	@Override
	public void run(String... strings) throws Exception {

		Server server = new Server();
		
		try (ConfigurableApplicationContext context = new ClassPathXmlApplicationContext("application-config.xml")) {
            PushBaseProperty baseProperty = context.getBean(PushBaseProperty.class);
            Collection<PushServiceProperty> serviceProperties = context.getBeansOfType(PushServiceProperty.class).values();

            // Push 서버 모듈 기동
            server.startupServer(false, baseProperty, serviceProperties);
            
            synchronized (MimsWakeApplication.class) {
            	MimsWakeApplication.class.wait();
            }

        } catch (Exception e) {
        	logger.error("startup failed", e);

        } finally {
            server.shutdownServer();
        }
		

	}
	
}