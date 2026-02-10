package com.isec.platform.modules.rating.dto;

import com.isec.platform.modules.rating.domain.RuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddonDto implements Serializable {
    private Long id;
    private String name;
    private String description;
    private String category;
}
