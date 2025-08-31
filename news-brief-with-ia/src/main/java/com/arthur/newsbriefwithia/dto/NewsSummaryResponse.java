package com.arthur.newsbriefwithia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsSummaryResponse {

    private String summary;
    private LocalDate createdAt;


}
