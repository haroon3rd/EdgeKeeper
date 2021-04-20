package edu.tamu.cse.lenss.edgeKeeper.orch;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import org.apache.log4j.Logger;
import org.yaml.snakeyaml.*;

public class YamlParser {
	static final Logger logger = Logger.getLogger(YamlParser.class);
	
	
	public boolean isValidYaml(File deployPath, String list) {
		Yaml yaml = new Yaml();
		InputStream inputStream = this.getClass()
		  .getClassLoader()
		  .getResourceAsStream(System.getProperty("user.dir") + File.separatorChar + deployPath.getName() + File.separatorChar + list );
		Map<String, Object> obj = yaml.load(inputStream);
		System.out.println(obj);
		
		return false;
	}
		
	
	
}
