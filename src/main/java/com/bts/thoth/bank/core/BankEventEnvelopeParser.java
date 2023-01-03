package com.bts.thoth.bank.core;

import com.fasterxml.jackson.databind.JsonNode;
import fr.maif.eventsourcing.EventEnvelope;
import fr.maif.json.Json;
import io.vavr.Tuple0;
import io.vavr.control.Either;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.bts.thoth.bank.core.BankEvent.MoneyWithdrawnV1;
import static io.vavr.API.*;

public class BankEventEnvelopeParser {

    public static Either<String, EventEnvelope<BankEvent, Tuple0, Tuple0>> parseEnvelope(JsonNode json) {

        Either<String, BankEvent> eventOrElse = BankEventEnvelopeParser.getEventFromJson(json);

        if(eventOrElse.isLeft()){
            return Left(eventOrElse.getLeft());
        }
        BankEvent event = (BankEvent)eventOrElse.get();

        if(!event.type().name().equals(MoneyWithdrawnV1.name())){
            return Left("TODO : EventType to implement.");
        }

        EventEnvelope.Builder<BankEvent, Tuple0, Tuple0> builder = EventEnvelope.builder();

        return Right(builder
                .withEventType(MoneyWithdrawnV1.name())
                .withEvent(event)
                .withEmissionDate(LocalDateTime.parse(json.get("emissionDate").asText()))
                .withId(UUID.fromString(json.get("id").asText()))
                .withEntityId(json.get("entityId").asText())
                .withSequenceNum(json.get("sequenceNum").asLong())
                .withTransactionId(json.get("transactionId").asText())
                .build());
    }

    private static Either<String, BankEvent> getEventFromJson(JsonNode json){

        String eventType = json.has("eventType")? json.get("eventType").textValue(): "NA";

        if(eventType.equals("NA")){
            return Left("Not implemented: eventType not found");
        }

        return switch (eventType) {
            case "AccountOpened" -> Right((BankEvent) Json.fromJson(json, BankEvent.AccountOpened.class).get());
            case "MoneyDeposited" -> Right((BankEvent) Json.fromJson(json, BankEvent.MoneyDeposited.class).get());
            case "MoneyWithdrawn" -> Right(BankEvent.MoneyWithdrawn.parse(json));
            case "AccountClosed" -> Right((BankEvent) Json.fromJson(json, BankEvent.AccountClosed.class).get());
            default -> Left("Not implemented: eventType " + eventType + " unknown");
        };
    }



}
