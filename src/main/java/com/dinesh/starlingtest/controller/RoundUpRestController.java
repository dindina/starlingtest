package com.dinesh.starlingtest.controller;

import com.dinesh.starlingtest.service.RoundUpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;


@Schema(description = "Response object for the round-up and transfer operation")
record TransferResponseDto(
        @Schema(description = "Unique identifier for the transfer", nullable = true) String transferUid,
        @Schema(description = "Indicates if the operation was successful") boolean success
) {}
@RestController
@RequestMapping("/round-up/process")
@Tag(name = "Round-Up", description = "Operations related to calculating and transferring round-up amounts")

public class RoundUpRestController {

    private final RoundUpService roundUpService;


    record TransferResponseDto(String transferUid, boolean success) {}

    public RoundUpRestController(RoundUpService roundUpService) {
        this.roundUpService = roundUpService;
    }

    @Operation(
            summary = "Calculate and transfer round-up",
            description = "Calculates the total round-up for a specified period and initiates the transfer to the savings goal."
    )
    @ApiResponse(responseCode = "200", description = "Successfully processed round-up and initiated transfer", content = @Content(schema = @Schema(implementation = TransferResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Failed to initiate transfer", content = @Content(schema = @Schema(implementation = TransferResponseDto.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = TransferResponseDto.class)))
    @PostMapping()
    public ResponseEntity<TransferResponseDto> calculateAndTransfer(
            @Parameter(description = "Start date for the statement (YYYY-MM-DD)") @RequestParam("startDate") String startDateStr,
            @Parameter(description = "End date for the statement (YYYY-MM-DD)") @RequestParam("endDate") String endDateStr,
            @Parameter(description = "Starling account number") @RequestParam("accountNumber") String accountNumber,
            @Parameter(description = "Starling savings goal ID") @RequestParam("goalSavingsId") String goalSavingsId
    )
    {
        try {
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);

            Map<Integer, Double> weeklyRoundUps = roundUpService.downloadAndProcessStatement(startDate, endDate, accountNumber);
            double totalRoundUp = 0;

            // Calculate total round-up for the given period
            for (double roundUp : weeklyRoundUps.values()) {
                totalRoundUp += roundUp;
            }

            if (totalRoundUp > 0) {
                String transferUid = UUID.randomUUID().toString();
                boolean transferSuccess = roundUpService.transferToSavingsGoal(accountNumber, goalSavingsId, totalRoundUp, transferUid);
                TransferResponseDto response = new TransferResponseDto(transferUid, transferSuccess);
                return new ResponseEntity<>(response, transferSuccess ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
            } else {
                TransferResponseDto response = new TransferResponseDto(null, true); // No transfer needed, consider it a success
                return new ResponseEntity<>(response, HttpStatus.OK);
            }

        } catch (IOException e) {
            System.err.println("Error during round-up and transfer: " + e.getMessage());
            TransferResponseDto response = new TransferResponseDto(null, false);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


}