package edu.tamu.cse.lenss.edgeKeeper.orch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.nd4j.shade.jackson.core.JsonParseException;
import org.nd4j.shade.jackson.databind.JsonMappingException;
import org.yaml.snakeyaml.Yaml;

public class YamlParser {
	static final Logger logger = Logger.getLogger(YamlParser.class);

	public boolean isValidYaml(File deployPath, String list) {
		Yaml yaml = new Yaml();
		InputStream inputStream = this.getClass()
				.getClassLoader()
				.getResourceAsStream(System.getProperty("user.dir") + File.separatorChar + deployPath.getName() + File.separatorChar + list );
		Map<String, Object> obj = (Map<String, Object>) yaml.load(inputStream);
		System.out.println("My YAML String is: " + obj);

		return false;
	}

	List<String> convertYamlToContainers(File deployPath, List<String> deployList) throws JsonParseException, JsonMappingException, IOException {
		List<String> myList   = new ArrayList<>();
		for (String list : deployList){
			System.out.println(list);
			InputStream inputStream = new FileInputStream(new File(System.getProperty("user.dir") + File.separatorChar + deployPath.getName() + File.separatorChar + list));
			Yaml yaml = new Yaml();
			Map<String, Object> composeFile = (Map<String, Object>) yaml.load(inputStream);
			//Map<String, Object> services = (Map<String, Object>) data.get("services");
			Map<String, Object> services = extracted(composeFile, "services");
			System.out.println("Services found : " + services.keySet());
			for (String serviceName : services.keySet()) {
				Map<String, Object> containers = (Map<String, Object>)services.get(serviceName);
				myList.add(containers.get("container_name").toString());
				//System.out.println("Container : " + myList);
			}
			//System.out.println(jsonString);
		}
		return myList;
	}

	private Map<String, Object> extracted(Map<String, Object> data, String myKey) {
		return (Map<String, Object>)data.get(myKey);
	}

	/*
	 * private static String convertYamlToJson(String yaml) { try { ObjectMapper
	 * yamlReader = new ObjectMapper(new YAMLFactory()); Object obj =
	 * yamlReader.readValue(yaml, Object.class); ObjectMapper jsonWriter = new
	 * ObjectMapper(); return
	 * jsonWriter.writerWithDefaultPrettyPrinter().writeValueAsString(obj); } catch
	 * (JsonProcessingException ex) { ex.printStackTrace(); } catch (IOException ex)
	 * { ex.printStackTrace(); } return null; }
	 */

	static String readFile(String path, Charset encoding)
			throws IOException
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}


}
