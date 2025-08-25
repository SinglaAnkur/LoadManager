package com.venn.LoadManager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({ "id", "customer_id", "accepted" })
public class LoadResponseDTO {
    private String id;

    @JsonProperty("customer_id")
    private String customerId;

    private boolean accepted;

    public LoadResponseDTO() {}

    public LoadResponseDTO(String id, String customerId, boolean accepted) {
        this.id = id;
        this.customerId = customerId;
        this.accepted = accepted;
    }
}
