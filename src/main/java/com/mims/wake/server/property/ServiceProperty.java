package com.mims.wake.server.property;

import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Properties;
import java.util.Vector;

import javax.annotation.Resource;

import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.InputStreamSource;
import org.springframework.stereotype.Service;

import com.mims.wake.util.commonUtil;

@Service("prop")
public class ServiceProperty {	
	private Properties _properties;
	private PushBaseProperty _baseProperty;
	private Collection<PushServiceProperty> _serviceProperties;
	private String _pathFile;
	private XMLDecoder xmlDecoder;

	public ServiceProperty() {
		String path = System.getProperty("user.dir");
		Vector<String> arrFile = commonUtil.getFileNames(path, "xml");
		if (!arrFile.isEmpty())
			_pathFile = arrFile.get(0);
	}

	public void loadPropFile() {
		if(_pathFile.isEmpty())
			return;
		
		try {		
			Properties prop = new Properties();
			InputStream stream = new FileInputStream(_pathFile);

//			FileSystemResourceLoader fileSystemResourceLoader = new FileSystemResourceLoader();
//			Resource propResource = (Resource) fileSystemResourceLoader.getResource(_pathFile);
//			InputStream is = ((InputStreamSource) propResource).getInputStream();
//
//			_properties = new Properties();
//			_properties.load(is);
			
			// xml
			prop.loadFromXML(stream);
			String base = prop.getProperty("baseProperty");
			System.out.println("SERVER_IP=" + prop.get("inboundServerPort"));
			
			stream.close();
		} catch (IOException e) {

		}
	}

	public String get(String key) {
		return _properties.getProperty(key);
	}
	
	public void XmlDecodeFromFile() {
		try {
			FileInputStream fis = new FileInputStream(_pathFile);
			BufferedInputStream bis = new BufferedInputStream(fis);
			xmlDecoder = new XMLDecoder(bis);
			_baseProperty = (PushBaseProperty) xmlDecoder.readObject();
			System.out.println(_baseProperty.toString());
		} catch (IOException e) {

		}
	}
}
