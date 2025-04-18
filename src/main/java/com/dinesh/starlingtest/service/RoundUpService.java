package com.dinesh.starlingtest.service;

import okhttp3.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class RoundUpService {


    @Value("${starling.api.base-url}")
    private String starlingApiBaseUrl;

    @Value("${starling.api.key}")
    private String apiKey;

    @Value("${retry.max-retries}")
    private int maxRetries;

    @Value("${retry.delay-seconds}")
    private long retryDelaySeconds;
    private final OkHttpClient client = new OkHttpClient();



    public boolean transferToSavingsGoal(String accountId, String savingsGoalId , double amount, String transferId) throws IOException, InterruptedException {
        String idempotencyKey  = transferId;
        String transferUrl = String.format("%s/account/%s/savings-goals/%s/add-money/%s",
                starlingApiBaseUrl, accountId, savingsGoalId,idempotencyKey);

        System.out.println("transferUrl " + transferUrl);

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(String.format("{\"amount\":{\"currency\":\"GBP\",\"minorUnits\":%d}}", (int) (amount * 100)), JSON);

        Request request = new Request.Builder()
                .url(transferUrl)
                .put(requestBody)
                .header("Authorization", "Bearer " + apiKey)
                .header("Idempotency-Key", idempotencyKey) // Include idempotency key
                .build();

        for (int i = 0; i <= maxRetries; i++) {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() || response.code() == 409) { // 409 Conflict might indicate a previous successful request with the same idempotency key
                    System.out.println("Successfully transferred £" + String.format("%.2f", amount) + " (attempt " + (i + 1) + ", idempotency key: " + idempotencyKey + ")");
                    return true;
                } else {
                    System.err.println("Failed to transfer (attempt " + (i + 1) + "): " + response.code() + " - " + response.body().string());
                    if (i < maxRetries && shouldRetry(response.code())) {
                        Thread.sleep(Duration.ofSeconds(retryDelaySeconds).toMillis());
                    } else {
                        throw new IOException("Failed to transfer after " + (i + 1) + " attempts.");
                    }
                }
            } catch (IOException e) {
                System.err.println("Error during transfer call (attempt " + (i + 1) + "): " + e.getMessage());
                if (i < maxRetries) {
                    Thread.sleep(Duration.ofSeconds(retryDelaySeconds).toMillis());
                } else {
                    throw e;
                }
            }
        }
        return true;
    }

    private boolean shouldRetry(int responseCode) {
        return responseCode >= 500 || responseCode == 429; // Retry on server errors and rate limiting
    }

    public Map<Integer, Double> downloadAndProcessStatement(LocalDate startDate, LocalDate endDate, String accountNumber) throws IOException, InterruptedException {
        String statementUrl = String.format("%s/accounts/%s/statement/downloadForDateRange?start=%s&end=%s",
                starlingApiBaseUrl,
                accountNumber,
                startDate.format(DateTimeFormatter.ISO_DATE),
                endDate.format(DateTimeFormatter.ISO_DATE));

        Request httpRequest = new Request.Builder()
                .url(statementUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "text/csv")
                .build();

        for (int i = 0; i <= maxRetries; i++) {
            try (Response response = client.newCall(httpRequest).execute()) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        Reader reader = response.body().charStream();
                        return processCsvStatement(reader);
                    } else {
                        System.out.println("No content received for the statement.");
                        return new HashMap<>();
                    }
                } else {
                    System.err.println("Failed to download statement (attempt " + (i + 1) + "): " + response.code() + " - " + (response.body() != null ? response.body().string() : "No body"));
                    if (i < maxRetries && shouldRetry(response.code())) {
                        Thread.sleep(Duration.ofSeconds(retryDelaySeconds).toMillis());
                    } else {
                        throw new IOException("Failed to download statement after " + (i + 1) + " attempts.");
                    }
                }
            } catch (IOException e) {
                System.err.println("Error during download call (attempt " + (i + 1) + "): " + e.getMessage());
                if (i < maxRetries) {
                    Thread.sleep(Duration.ofSeconds(retryDelaySeconds).toMillis());
                } else {
                    throw e;
                }
            }
        }
        return new HashMap<>();
    }

    private Map<Integer, Double> processCsvStatement(Reader reader) throws IOException {
        Map<Integer, Double> weeklyRoundUps = new HashMap<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        try (CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
            System.out.println("CSV Headers: " + parser.getHeaderNames());

            for (CSVRecord record : parser) {
                try {
                    String dateString = record.get("Date");
                    String amountString = record.get("Amount (GBP)");
                    //System.out.println("amountString " + amountString);

                    LocalDate transactionDate = LocalDate.parse(dateString, dateFormatter);
                    double amount = Double.parseDouble(amountString);

                    if (amount < 0) { // debit transactions
                        System.out.println("debit amount " + amount);
                        double spendingAmount = Math.abs(amount);
                        double roundedUp = Math.ceil(spendingAmount);
                        System.out.println("roundedUp amount " + roundedUp);
                        double roundUpContribution = roundedUp - spendingAmount;
                        System.out.println("roundUpContribution amount " + roundUpContribution);

                        int weekNumber = transactionDate.get(weekFields.weekOfWeekBasedYear());
                        int year = transactionDate.getYear();
                        int weekKey = year * 100 + weekNumber;

                        weeklyRoundUps.put(weekKey, weeklyRoundUps.getOrDefault(weekKey, 0.0) + roundUpContribution);
                    }

                } catch (NumberFormatException e) {
                    System.err.println("Could not parse amount in record: " + record.getRecordNumber() + " - " + e.getMessage());
                } catch (java.time.format.DateTimeParseException e) {
                    System.err.println("Could not parse date in record: " + record.getRecordNumber() + " - " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Error processing record: " + record.getRecordNumber() + " - " + e.getMessage());
                }
            }
            System.out.println("weeklyRoundUps  " + weeklyRoundUps);
            return weeklyRoundUps;

        }
    }


}