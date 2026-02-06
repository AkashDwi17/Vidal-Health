package VidalHealth.example.Vidal.Health.config;

import VidalHealth.example.Vidal.Health.service.HiringService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StartupRunner {

    @Bean
    CommandLineRunner startOnBoot(HiringService hiringService) {
        return args -> hiringService.executeHiringFlow();
    }
}