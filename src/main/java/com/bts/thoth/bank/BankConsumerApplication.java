package com.bts.thoth.bank;

import com.bts.thoth.bank.core.AsyncWithdrawByMonthProjection;
import com.bts.thoth.bank.core.BankEvent;
import com.bts.thoth.bank.core.BankEvent.BankEventJsonFormat;
import com.bts.thoth.bank.core.BankEventEnvelopeParser;
import com.bts.thoth.bank.core.WithdrawByMonthProjection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import fr.maif.eventsourcing.EventEnvelope;
import fr.maif.eventsourcing.EventProcessorImpl;
import fr.maif.eventsourcing.format.JacksonEventFormat;
import fr.maif.jooq.reactor.PgAsyncPool;
import fr.maif.jooq.reactor.PgAsyncTransaction;
import fr.maif.kafka.reactor.consumer.ResilientKafkaConsumer;
import fr.maif.kafka.reactor.consumer.Status;
import io.github.cdimascio.dotenv.Dotenv;
import io.vavr.Tuple0;
import io.vavr.control.Either;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.kafka.receiver.ReceiverOptions;

import java.util.HashMap;
import io.vavr.collection.List;
import java.util.Map;

import static io.vavr.API.print;


@SpringBootApplication
public class BankConsumerApplication {

	private static final Dotenv dotenv = Dotenv.load();

	private static final TimeBasedGenerator UUIDgenerator = Generators.timeBasedGenerator();

	private static PgPool pgPool;
	private static Vertx vertx = Vertx.vertx();
	private static PgAsyncPool pgProjectionPool;

	private static WithdrawByMonthProjection withdrawByMonthProjection;

	public static void main(String[] args) {

		Logger.getRootLogger().setLevel(Level.INFO);

		DefaultConfiguration jooqConfig = new DefaultConfiguration();
		jooqConfig.setSQLDialect(SQLDialect.POSTGRES);

		PgConnectOptions options = new PgConnectOptions()
				.setHost(dotenv.get("PPG_HOST"))
				.setPort(Integer.parseInt(dotenv.get("PPG_PORT")))
				.setUser(dotenv.get("PPG_USER"))
				.setPassword(dotenv.get("PPG_PWD"))
				.setDatabase(dotenv.get("PPG_DB"));
		PoolOptions poolOptions = new PoolOptions().setMaxSize(50);
		pgPool = PgPool.pool(vertx, options, poolOptions);
		pgProjectionPool = PgAsyncPool.create(pgPool, jooqConfig);

		withdrawByMonthProjection = new AsyncWithdrawByMonthProjection(pgProjectionPool);

		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, dotenv.get("KAFKA_HOST"));
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		ReceiverOptions<String, String> receiverOptions = ReceiverOptions.<String, String>create(props);

		ObjectMapper map = new ObjectMapper();

		BankEvent.BankEventJsonParser bankEventParser = new BankEvent.BankEventJsonParser();

		ResilientKafkaConsumer<String, String> resilientKafkaConsumer = ResilientKafkaConsumer.create(
				"ThothBankConsumer-" + UUIDgenerator.generate(),
				ResilientKafkaConsumer.Config.create(
						java.util.List.of(dotenv.get("KAFKA_TOPIC")),
						"ThothBankConsumer-lol8",
						receiverOptions
				),
				event -> {
					System.out.println("Event %s".formatted(event.value()));

					try {
						JsonNode json = map.readTree(event.value());

						Either<String, EventEnvelope<BankEvent, Tuple0, Tuple0>> test = BankEventEnvelopeParser.parseEnvelope(json);
						System.out.println(test);

						if (test.isRight()){
							EventEnvelope<BankEvent, Tuple0, Tuple0> bankEventEnvelope = test.get();

							withdrawByMonthProjection.storeProjection((PgAsyncTransaction) pgProjectionPool, List.of(bankEventEnvelope));

						}
					} catch (JsonProcessingException e) {
						System.out.println(e);
					}

					// ConsumerRecord

				}
//				.doOnNext(event -> {
//					withdrawByMonthProjection.storeProjection((PgAsyncTransaction) pgProjectionPool,
//							(io.vavr.collection.List<EventEnvelope<BankEvent, Tuple0, Tuple0>>) io.vavr.collection.List.of(event)));
//
//				})
		);

		print("test");
	}

	static void projectEvent(BankEvent event){
		/*withdrawByMonthProjection.storeProjection((PgAsyncTransaction) pgProjectionPool,
				List.of(EventEnvelope.builder(event)));*/
	}
}
