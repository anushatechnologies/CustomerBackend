package com.hinchexpress.trip.dto;

import com.hinchexpress.common.enums.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckpointDto {

    private Long id;
    private Long tripId;
    private TripStatus checkpointStatus;
    private String title;
    private String description;
    private String locationName;
    private Double latitude;
    private Double longitude;
    private LocalDateTime recordedAt;
}

