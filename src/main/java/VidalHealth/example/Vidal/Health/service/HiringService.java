package VidalHealth.example.Vidal.Health.service;

import VidalHealth.example.Vidal.Health.dto.GenerateWebhookRequest;
import VidalHealth.example.Vidal.Health.dto.GenerateWebhookResponse;
import VidalHealth.example.Vidal.Health.dto.SubmitQueryRequest;
import VidalHealth.example.Vidal.Health.entity.FinalQueryEntity;
import VidalHealth.example.Vidal.Health.repository.QueryRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class HiringService {

    private static final String GENERATE_WEBHOOK_URL =
            "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

    private final QueryRepository queryRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public HiringService(QueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    public void executeHiringFlow() {

        /* STEP 1: Generate Webhook */
        GenerateWebhookRequest request = new GenerateWebhookRequest();
        request.setName("John Doe");
        request.setRegNo("REG12347");   // CHANGE only if your regNo is different
        request.setEmail("john@example.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<GenerateWebhookRequest> httpEntity =
                new HttpEntity<>(request, headers);

        GenerateWebhookResponse response =
                restTemplate.postForObject(
                        GENERATE_WEBHOOK_URL,
                        httpEntity,
                        GenerateWebhookResponse.class
                );

        if (response == null) {
            throw new RuntimeException("Webhook generation failed");
        }

        /* STEP 2: Decide FINAL SQL */
        String finalSql = decideSql(request.getRegNo());

        /* STEP 3: Store SQL (in memory – requirement satisfied) */
        FinalQueryEntity entity = new FinalQueryEntity();
        entity.setRegNo(request.getRegNo());
        entity.setFinalQuery(finalSql);
        queryRepository.save(entity);

        /* STEP 4: Submit SQL to webhook */
        SubmitQueryRequest submitRequest = new SubmitQueryRequest();
        submitRequest.setFinalQuery(finalSql);

        HttpHeaders submitHeaders = new HttpHeaders();
        submitHeaders.setContentType(MediaType.APPLICATION_JSON);
        submitHeaders.set("Authorization", response.getAccessToken());

        HttpEntity<SubmitQueryRequest> submitEntity =
                new HttpEntity<>(submitRequest, submitHeaders);

        restTemplate.postForEntity(
                response.getWebhook(),
                submitEntity,
                String.class
        );
    }

    /* =========================================================
       SQL LOGIC
       ========================================================= */

    private String decideSql(String regNo) {

        String digits = regNo.replaceAll("\\D", "");
        int lastTwoDigits =
                Integer.parseInt(digits.substring(digits.length() - 2));

        if (lastTwoDigits % 2 != 0) {
            // ODD → QUESTION 1
            return """
                    SELECT
                        d.DEPARTMENT_NAME,
                        p.AMOUNT AS SALARY,
                        CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS EMPLOYEE_NAME,
                        TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE
                    FROM DEPARTMENT d
                    JOIN EMPLOYEE e
                        ON d.DEPARTMENT_ID = e.DEPARTMENT
                    JOIN PAYMENTS p
                        ON e.EMP_ID = p.EMP_ID
                    WHERE DAY(p.PAYMENT_TIME) <> 1
                      AND p.AMOUNT = (
                          SELECT MAX(p2.AMOUNT)
                          FROM PAYMENTS p2
                          JOIN EMPLOYEE e2 ON p2.EMP_ID = e2.EMP_ID
                          WHERE e2.DEPARTMENT = d.DEPARTMENT_ID
                            AND DAY(p2.PAYMENT_TIME) <> 1
                      );
                    """;
        } else {

            // EVEN → QUESTION 2

            return """
                    SELECT
                        d.DEPARTMENT_NAME,
                        AVG(TIMESTAMPDIFF(YEAR, e.DOB, CURDATE())) AS AVERAGE_AGE,
                        GROUP_CONCAT(
                            CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME)
                            ORDER BY e.EMP_ID
                            SEPARATOR ', '
                        ) AS EMPLOYEE_LIST
                    FROM DEPARTMENT d
                    JOIN EMPLOYEE e
                        ON d.DEPARTMENT_ID = e.DEPARTMENT
                    JOIN PAYMENTS p
                        ON e.EMP_ID = p.EMP_ID
                    WHERE p.AMOUNT > 70000
                    GROUP BY d.DEPARTMENT_ID, d.DEPARTMENT_NAME
                    ORDER BY d.DEPARTMENT_ID DESC;
                    """;
        }
    }
}
