package VidalHealth.example.Vidal.Health.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GenerateWebhookRequest {

    private String name;
    private String regNo;
    private String email;

}
