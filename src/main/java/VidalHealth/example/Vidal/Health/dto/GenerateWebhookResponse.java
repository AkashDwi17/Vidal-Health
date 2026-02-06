package VidalHealth.example.Vidal.Health.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GenerateWebhookResponse {

    private String webhook;
    private String accessToken;

}