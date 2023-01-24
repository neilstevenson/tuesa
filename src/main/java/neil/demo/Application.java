package neil.demo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Date;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;

public class Application {
	private static final String MY_NAME = "tuesa";

	public static void main(String[] args) throws Exception {
		String input = System.getProperty("MY_INPUT");

		ClientConfig clientConfig = new ClientConfig();

		clientConfig.setInstanceName(MY_NAME);
		
		clientConfig.getNetworkConfig().getKubernetesConfig()
		.setEnabled(true)
		.setProperty("service-dns", "neil-hazelcast.neil.svc.cluster.local");
		
		clientConfig.getNetworkConfig().getKubernetesConfig()
		.getProperties()
		.entrySet()
		.forEach(entry -> {
			System.out.printf("KUBERNETES CONFIG, '%s'=='%s'",
					entry.getKey(), entry.getValue());
		});
		
		HazelcastInstance hazelcastInstance = HazelcastClient.newHazelcastClient(clientConfig);

    	System.out.println("START ------------" + new Date());

        try (BufferedReader bufferedReader =
                new BufferedReader(
                        new InputStreamReader(Application.class.getClassLoader().getResourceAsStream(input)))) {
        	String line;
        	int count = 0;
        	while ((line = bufferedReader.readLine()) != null) {
            	String[] tokens = line.split(" ");
            	String mapName = tokens[1];
            	String key = tokens[2];
            	String value = tokens[3];

            	if (count % 100 == 0) {
                	System.out.println(Arrays.asList(tokens));
            	}
            	hazelcastInstance.getMap(mapName).set(key, value);
            	count++;
            }
        	
        	System.out.println("------------");
        	System.out.printf("Wrote %d%n", count);
        	System.out.println("------------");
        } catch (Exception e) {
        	e.printStackTrace();
        }
    	System.out.println("END ------------" + new Date());
		
		hazelcastInstance.shutdown();
	}

}
