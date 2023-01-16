package com.bts.thoth.bank.account;

import java.util.UUID;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.Tuple0;
import io.vavr.control.Either;
import static io.vavr.API.*;

import fr.maif.eventsourcing.EventEnvelope;

public class AccountEventEnvelopeParser {

    public static Either<String, EventEnvelope<AccountEvent, Tuple0, Tuple0>> parseEnvelope(JsonNode json) {

        Either<String, AccountEvent> eventOrElse = AccountEventEnvelopeParser.getEventFromJson(json);

        if(eventOrElse.isLeft()){
            return Left(eventOrElse.getLeft());
        }
        AccountEvent event = eventOrElse.get();

        EventEnvelope.Builder<AccountEvent, Tuple0, Tuple0> builder = EventEnvelope.builder();
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

    private static Either<String, AccountEvent> getEventFromJson(JsonNode json){

        String eventType = json.has("eventType")? json.get("eventType").textValue(): "NA";

        if(eventType.equals("NA")){
            return Left("Not implemented: eventType not found");
        }

        return switch (eventType) {
            case "AccountOpened" -> Right(AccountEvent.AccountOpened.parse(json));
            case "MoneyDeposited" -> Right(AccountEvent.MoneyDeposited.parse(json));
            case "MoneyWithdrawn" -> Right(AccountEvent.MoneyWithdrawn.parse(json));
            case "AccountClosed" -> Right(AccountEvent.AccountClosed.parse(json));
            default -> Left("Not implemented: eventType " + eventType + " unknown");
        };
    }
}
