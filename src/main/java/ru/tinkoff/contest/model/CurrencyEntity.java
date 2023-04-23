package ru.tinkoff.contest.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "currency_value")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyEntity {

    @Id
    private String id;

    @Column(name = "value", columnDefinition = "numeric(20, 2)")
    private Double value;

}
