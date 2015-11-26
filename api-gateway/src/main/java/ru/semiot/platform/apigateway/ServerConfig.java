package ru.semiot.platform.apigateway;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Sources;

@LoadPolicy(LoadType.FIRST)
@Sources({"file:/semiot-platform/api-gateway/config.properties"})
public interface ServerConfig extends Config {
    
    @DefaultValue("http://deviceproxyservice:3030/ds/query")
    String sparqlEndpoint();
    
    @DefaultValue("admin")
    String sparqlUsername();
    
    @DefaultValue("pw")
    String sparqlPassword();
    
    @DefaultValue("ws://wamprouter:8080/ws")
    String wampUri();
    
    @DefaultValue("realm1")
    String wampRealm();
    
    @DefaultValue("http://opentsdb:4242/api/query")
    String tsdbEndpoint();
    
}