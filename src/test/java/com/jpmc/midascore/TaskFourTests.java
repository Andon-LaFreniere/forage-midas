package com.jpmc.midascore;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext
// @Disabled("Manual task verifier; run individually for debugging")
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
public class TaskFourTests {
    static final Logger logger = LoggerFactory.getLogger(TaskFourTests.class);

    @Autowired
    private KafkaProducer kafkaProducer;

    @Autowired
    private UserPopulator userPopulator;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileLoader fileLoader;

    @Test
    void task_four_verifier() throws InterruptedException {
        userPopulator.populate();
        String[] transactionLines = fileLoader.loadStrings("/test_data/alskdjfh.fhdjsk");
        for (String transactionLine : transactionLines) {
            kafkaProducer.send(transactionLine);
        }
        Thread.sleep(2000);

        logger.info("----------------------------------------------------------");
        logger.info("----------------------------------------------------------");
        logger.info("----------------------------------------------------------");

        // Dynamically find Wilbur out of the populated users
        for (long i = 1; i <= 20; i++) {
            userRepository.findById(i).ifPresent(user -> {
                if ("wilbur".equalsIgnoreCase(user.getName())) {
                    logger.info(">>> FOUND WILBUR'S BALANCE: " + user.getBalance());
                }
            });
        }
        logger.info("use your debugger to find out what wilbur's balance is after all transactions are processed");
        logger.info("kill this test once you find the answer");
        while (true) {
            Thread.sleep(20000);
            logger.info("...");
        }
    }
}
