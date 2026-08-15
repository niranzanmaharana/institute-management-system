package com.ims.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Hooks;

@SpringBootApplication
public class ApiGatewayApplication {

  public static void main(String[] args) {
    // Propagate Micrometer Observation / MDC across Reactor threads (gateway → logs)
    Hooks.enableAutomaticContextPropagation();
    SpringApplication.run(ApiGatewayApplication.class, args);
  }
}
