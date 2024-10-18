package org.example.nutzungszeitaufgabe.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@RestController
public class EventController {

    @Autowired
    private RestTemplate restTemplate;

    private static final String DATASET_API = "http://assessment-api:8080/v1/dataset";
    private static final String RESULT_API = "http://assessment-api:8080/v1/result";

    @PostMapping("/v1/result")
    public ResponseEntity<String> calculateAndPostUsageForAllCustomers() {
        List<Event> events = getDataset();

        Map<String, List<Event>> eventsByCustomer = events.stream()
                .collect(Collectors.groupingBy(Event::customerId));

        List<Result> resultList = new ArrayList<>();

        for (Map.Entry<String, List<Event>> entry : eventsByCustomer.entrySet()) {
            String customerId = entry.getKey();
            List<Event> customerEvents = entry.getValue();

            long totalUsage = calculateUsageForCustomer(customerId, customerEvents);

            resultList.add(new Result(customerId, totalUsage));
        }

        ResultWrapper resultWrapper = new ResultWrapper();
        resultWrapper.setResult(resultList);

        ResponseEntity<String> response = restTemplate.postForEntity(RESULT_API, resultWrapper, String.class);

        return ResponseEntity.ok("Results sent successfully");
    }


    @GetMapping("/v1/dataset")
    public List<Event> getDataset() {
        EventResponse response = restTemplate.getForObject(DATASET_API, EventResponse.class);
        return response.getEvents();
    }



    private long calculateUsageForCustomer(String customerId, List<Event> events) {
        List<Event> startEvents = events.stream()
                .filter(event -> "start".equals(event.eventType()))
                .collect(Collectors.toList());

        List<Event> stopEvents = events.stream()
                .filter(event -> "stop".equals(event.eventType()))
                .collect(Collectors.toList());

        Map<String, Long> workloadUsageMap = new HashMap<>();

        for (Event startEvent : startEvents) {
            Optional<Event> matchingStopEvent = stopEvents.stream()
                    .filter(stopEvent -> stopEvent.workloadId().equals(startEvent.workloadId()))
                    .findFirst();

            if (matchingStopEvent.isPresent()) {
                long usageTime = matchingStopEvent.get().timestamp() - startEvent.timestamp();

                workloadUsageMap.merge(startEvent.workloadId(), usageTime, Long::sum);

                stopEvents.remove(matchingStopEvent.get());
            }
        }

        long totalTime = workloadUsageMap.values().stream().mapToLong(Long::longValue).sum();

        if (totalTime <= 0) {
            System.err.println("Fehlerhafte Nutzungszeit für customerId: " + customerId);
        } else {
            System.out.println("Customerid: " + customerId + " | Consumption: " + totalTime + " Millisekunden");
        }

        return totalTime;
    }



}

