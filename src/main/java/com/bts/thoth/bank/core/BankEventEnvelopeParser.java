package com.bts.thoth.bank.core;

import java.util.UUID;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.Tuple0;
import io.vavr.control.Either;
import static io.vavr.API.*;

import fr.maif.eventsourcing.EventEnvelope;

public class BankEventEnvelopeParser {

    public static Either<String, EventEnvelope<BankEvent, Tuple0, Tuple0>> parseEnvelope(JsonNode json) {

        Either<String, BankEvent> eventOrElse = BankEventEnvelopeParser.getEventFromJson(json);

        if(eventOrElse.isLeft()){
            return Left(eventOrElse.getLeft());
        }
        BankEvent event = eventOrElse.get();

        EventEnvelope.Builder<BankEvent, Tuple0, Tuple0> builder = EventEnvelope.builder();
        builder
                .withEvent(event)
                .withEmissionDate(LocalDateTime.parse(json.get("emissionDate").asText()))
                .withId(UUID.fromString(json.get("id").asText()))
                .withEntityId(json.get("entityId").asText())
                .withSequenceNum(json.get("sequenceNum").asLong())
                .withTransactionId(json.get("transactionId").asText())
                .withEventType(event.type().name());

        return Right(builder.build());
    }

    private static Either<String, BankEvent> getEventFromJson(JsonNode json){

        String eventType = json.has("eventType")? json.get("eventType").textValue(): "NA";

        if(eventType.equals("NA")){
            return Left("Not implemented: eventType not found");
        }

        return switch (eventType) {
            case "AccountOpened" -> Right(BankEvent.AccountOpened.parse(json));
            case "MoneyDeposited" -> Right(BankEvent.MoneyDeposited.parse(json));
            case "MoneyWithdrawn" -> Right(BankEvent.MoneyWithdrawn.parse(json));
            case "AccountClosed" -> Right(BankEvent.AccountClosed.parse(json));
            default -> Left("Not implemented: eventType " + eventType + " unknown");
        };
    }
}
