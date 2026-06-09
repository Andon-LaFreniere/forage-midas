package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DatabaseConduit {
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public DatabaseConduit(UserRepository userRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    public void save(UserRecord userRecord) {
        userRepository.save(userRecord);
    }

    public void save(TransactionRecord transactionRecord) {
        transactionRepository.save(transactionRecord);
    }

    public void processTransaction(Transaction transaction) {
        if (transaction == null) {
            return;
        }

        Optional<UserRecord> sender = userRepository.findById(transaction.getSenderId());
        Optional<UserRecord> recipient = userRepository.findById(transaction.getRecipientId());

        if (sender.isEmpty() || recipient.isEmpty()) {
            return;
        }

        UserRecord senderRecord = sender.get();
        UserRecord recipientRecord = recipient.get();

        // Check if sender has enough balance (incentive doesn't affect sender checking)
        if (senderRecord.getBalance() < transaction.getAmount()) {
            return;
        }

        // Deduct ONLY amount from sender
        senderRecord.setBalance(senderRecord.getBalance() - transaction.getAmount());
        
        // Add BOTH amount and incentive to recipient
        recipientRecord.setBalance(recipientRecord.getBalance() + transaction.getAmount() + transaction.getIncentive());

        userRepository.save(senderRecord);
        userRepository.save(recipientRecord);
        
        // Pass the incentive value into your persistent database record structure
        // Note: You may need to add a matching 'incentive' field and constructor parameter to TransactionRecord.java
        transactionRepository.save(new TransactionRecord(senderRecord, recipientRecord, transaction.getAmount(), transaction.getIncentive()));
    }
}
