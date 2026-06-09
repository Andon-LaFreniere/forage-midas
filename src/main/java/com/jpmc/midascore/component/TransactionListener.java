package com.jpmc.midascore.component;

import com.jpmc.midascore.foundation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class TransactionListener {
    private static final Logger logger = LoggerFactory.getLogger(TransactionListener.class);
    private final DatabaseConduit databaseConduit;
    private final RestTemplate restTemplate;

    // Spring can autowire RestTemplate if a bean is defined, or you can instantiate it directly here
    public TransactionListener(DatabaseConduit databaseConduit) {
        this.databaseConduit = databaseConduit;
        this.restTemplate = new RestTemplate();
    }

    @KafkaListener(topics = "${general.kafka-topic}", containerFactory = "kafkaListenerContainerFactory")
    public void onTransaction(Transaction transaction) {
        logger.info("Received transaction: {}", transaction);

        try {
            String incentiveUrl = "http://localhost:8080/incentive";
            
            // Post the transaction object. Spring auto-serializes it to JSON.
            // Using a generic record or a mini-DTO to easily extract the "amount" field
            IncentiveResponse response = restTemplate.postForObject(incentiveUrl, transaction, IncentiveResponse.class);
            
            if (response != null) {
                // Ensure you have added this setter to your Transaction.java file
                transaction.setIncentive(response.getAmount());
                logger.info("Applied incentive of {} to transaction", response.getAmount());
            }
        } catch (Exception e) {
            logger.error("Failed to fetch incentive from API", e);
            // Default to 0 if the API fails or is unreachable
            transaction.setIncentive(0.0f); 
        }

        // Pass the enriched transaction down to be validated and saved
        databaseConduit.processTransaction(transaction);
    }

    // Static inner DTO to map the target API response structure
    private static class IncentiveResponse {
        private float amount;

        public float getAmount() {
            return amount;
        }

        public void setAmount(float amount) {
            this.amount = amount;
        }
    }
}