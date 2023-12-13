package io.domotik8s.knxcontroller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KnxController {

    public static void main(String[] args) {
        SpringApplication.run(KnxController.class, args);
    }

}
