package com.venn.LoadManager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoadRequestDTO {
    private String id;

    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("load_amount")
    private String loadAmount;

    private String time; // yyyy-MM-dd'T'HH:mm:ss'Z'
}