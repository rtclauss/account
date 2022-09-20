package com.ibm.hybrid.cloud.sample.stocktrader.account.util;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class MPConfigSourceLoader implements ConfigSource {

    private static final String JNOSQL_SECRETS_FILE_NAME = "/jnosql_secrets.properties";
    private static final String JNOSQL_CONFIG_FILE_NAME = "/jnosql_config.properties";

    private File secretsFile;
    private File configurationFile;

    public MPConfigSourceLoader(){
        var secretsPath = Paths.get(JNOSQL_SECRETS_FILE_NAME);
        if(Files.exists(secretsPath)){
            secretsFile = secretsPath.toFile();
        }
        var configPath = Paths.get(JNOSQL_CONFIG_FILE_NAME);
        if(Files.exists(configPath)){
            configurationFile = configPath.toFile();
        }
    }

    @Override
    public Map<String, String> getProperties() {
        var properties = new Properties();
        try(FileReader secretFr =
                    new FileReader(secretsFile)) {
            properties.load(secretFr);
        } catch (IOException e) {
            System.out.println("IOException in try block =>" + e.getMessage());
        }
        try(FileReader configFr =
                    new FileReader(configurationFile)) {
            properties.load(configFr);
        } catch (IOException e) {
            System.out.println("IOException in try block =>" + e.getMessage());
        }
        return streamConvert(properties);
    }


    public Map<String, String> streamConvert(Properties prop) {
        return prop.entrySet().stream().collect(
                Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        e -> String.valueOf(e.getValue()),
                        (prev, next) -> next, HashMap::new
                ));
    }
    @Override
    public Set<String> getPropertyNames() {
        return getProperties().keySet();
    }

    @Override
    public int getOrdinal() {
        //TODO what ensures the value is used: low or high?
        return 50;
    }

    @Override
    public String getValue(final String propertyName) {
        return getProperties().get(propertyName);
    }

    @Override
    public String getName() {
        return MPConfigSourceLoader.class.getSimpleName();
    }
}
