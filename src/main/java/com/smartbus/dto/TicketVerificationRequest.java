package com.smartbus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TicketVerificationRequest {
    @NotBlank
    @Size(max = 500)
    private String ticketNumber;
    private boolean markAsUsed;

    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }
    public boolean isMarkAsUsed() { return markAsUsed; }
    public void setMarkAsUsed(boolean markAsUsed) { this.markAsUsed = markAsUsed; }
}
