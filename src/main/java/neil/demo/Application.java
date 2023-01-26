package neil.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

public class Application {
	private static final String MY_NAME = "tuesa";

	public static void main(String[] args) throws Exception {
		
		String input = System.getProperty("MY_INPUT");
		String mapNameDefault = System.getProperty("MY_MAP_NAME", "");

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
    	System.out.println("Input: '" + Objects.toString(input) + "'");
    	System.out.println("MapName: '" + Objects.toString(mapNameDefault) + "'");
    	System.out.println("Map count " + hazelcastInstance.getDistributedObjects().size());
    	File file = new File(input);
    	
        try (BufferedReader bufferedReader =
                new BufferedReader(
                        new InputStreamReader(new FileInputStream(file)))) {
        	String line;
        	int count = 0;
        	while ((line = bufferedReader.readLine()) != null) {
            	String[] tokens = line.split(" ");
            	String mapName = null;
            	String key = null;
            	String prefix = "";
            	if (mapNameDefault.equals("")) {
                	mapName = tokens[1];
                	key = tokens[2];
            	} else {
            		if (mapNameDefault.equals(":")) {
                    	mapName = tokens[1].substring(5,13);
                    	key = tokens[1].substring(14);
                    	prefix = tokens[2] + " ";
            		} else {
                    	mapName = mapNameDefault;
                    	key = tokens[1] + "-" + tokens[2];
            		}
            	}
            	String value = prefix + tokens[3];

            	if (count % 100 == 0) {
                	System.out.println(mapName + " " + Arrays.asList(tokens));
            	}
            	Object oldValue = hazelcastInstance.getMap(mapName).get(key);
            	if (oldValue == null) {
                	hazelcastInstance.getMap(mapName).set(key, value);
            	} else {
                	hazelcastInstance.getMap(mapName).set(key, oldValue + "," + value);
            	}
            	count++;
            }
        	
        	System.out.println("------------");
        	System.out.printf("Wrote %d%n", count);
        	System.out.println("------------");
        } catch (Exception e) {
        	e.printStackTrace();
        }

    	System.out.println("Map count " + hazelcastInstance.getDistributedObjects().size());
    	Collection<DistributedObject> distributedObjects = hazelcastInstance.getDistributedObjects();
    	if (distributedObjects.size() < 10) {
    		for (DistributedObject distributedObject : distributedObjects) {
    			if (distributedObject instanceof IMap) {
    				IMap<?, ?> iMap = (IMap<?, ?>) distributedObject;
    				System.out.printf("%s %d%n", iMap.getName(), iMap.size());
    				Set<?> keys = iMap.keySet();
    				Iterator<?> iterator = keys.iterator();
    				int count = 0;
    				while (iterator.hasNext() & count < 5) {
    					Object key = iterator.next();
    					System.out.printf(" %s %s%n", key.toString(), iMap.get(key).toString());
    					count++;
    				}
    			}
    		}
    	}
    	System.out.println("END ------------" + new Date());
		
		hazelcastInstance.shutdown();
	}

}
