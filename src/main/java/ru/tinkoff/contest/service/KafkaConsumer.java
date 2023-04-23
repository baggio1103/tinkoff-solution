package ru.tinkoff.contest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.tinkoff.contest.model.CurrencyEntity;
import ru.tinkoff.contest.repository.CurrencyRepository;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumer {

    private final CurrencyRepository currencyRepository;

    @KafkaListener(topics = "${topicName}", groupId = "group_id")
    public void processMessage(String currencyRelation) {
        log.info("Message received: " + currencyRelation);
        currencyRelation = currencyRelation
                .replace("EURUSD", String.format("\"%s\"", "EURUSD"))
                .replace("GBPUSD",  String.format("\"%s\"", "GBPUSD"))
                .replace("USDRUB",  String.format("\"%s\"", "USDRUB"))
                .replace("GBPEUR",  String.format("\"%s\"", "GBPEUR"))
                .replace("GBPRUB",  String.format("\"%s\"", "GBPRUB"))
                .replace("EURRUB",  String.format("\"%s\"", "EURRUB"));
        log.info("Message modified to {}", currencyRelation);
        var objectMapper = new ObjectMapper();
        try {
            var relations = objectMapper.readValue(currencyRelation, new TypeReference<Map<String, Double>>() {
            });
            // EURUSD, GBPUSD, USDRUB, GBPEUR, GBPRUB, EURRUB

            // EURUSD
            var eurusd = relations.get("EURUSD");
            currencyRepository.findById("EURUSD").ifPresentOrElse(currencyEntity -> {
                currencyEntity.setValue(eurusd);
                currencyRepository.save(currencyEntity);
            }, () -> currencyRepository.save(new CurrencyEntity("EURUSD", eurusd)));

            //
            var gbpusd = relations.get("GBPUSD");
            currencyRepository.findById("GBPUSD").ifPresentOrElse(currencyEntity -> {
                currencyEntity.setValue(gbpusd);
                currencyRepository.save(currencyEntity);
            }, () -> {
                currencyRepository.save(new CurrencyEntity("GBPUSD", gbpusd));
            });

            // USDRUB
            var usdRub = relations.get("USDRUB");
            currencyRepository.findById("USDRUB").ifPresentOrElse(currencyEntity -> {
                currencyEntity.setValue(usdRub);
                currencyRepository.save(currencyEntity);
            }, () -> {
                currencyRepository.save(new CurrencyEntity("USDRUB", usdRub));
            });

            // GBPEUR
            var gbpeur = relations.get("GBPEUR");
            currencyRepository.findById("GBPEUR").ifPresentOrElse(currencyEntity -> {
                currencyEntity.setValue(gbpeur);
                currencyRepository.save(currencyEntity);
            }, () -> {
                currencyRepository.save(new CurrencyEntity("GBPEUR", gbpeur));
            });

            //
            var gbprub = relations.get("GBPRUB");
            currencyRepository.findById("GBPRUB").ifPresentOrElse(currencyEntity -> {
                currencyEntity.setValue(gbprub);
                currencyRepository.save(currencyEntity);
            }, () -> {
                currencyRepository.save(new CurrencyEntity("GBPRUB", gbprub));
            });


            // EURRUB
            var eurrub = relations.get("EURRUB");
            currencyRepository.findById("EURRUB").ifPresentOrElse(currencyEntity -> {
                currencyEntity.setValue(eurrub);
                currencyRepository.save(currencyEntity);
            }, () -> {
                currencyRepository.save(new CurrencyEntity("EURRUB", eurrub));
            });

        } catch (JsonProcessingException e) {
            log.error("Error ", e);
            throw new RuntimeException(e);
        }
    }

}
