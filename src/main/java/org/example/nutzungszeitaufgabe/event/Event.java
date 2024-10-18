package org.example.nutzungszeitaufgabe.event;

public record Event(
        String customerId,
        String workloadId,
        long timestamp,
        String eventType
){
}
