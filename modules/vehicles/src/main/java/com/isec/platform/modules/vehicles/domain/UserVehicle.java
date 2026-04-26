package com.isec.platform.modules.vehicles.domain;

import com.isec.platform.common.domain.BaseEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Table("user_vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVehicle extends BaseEntity {

    @Id
    private UUID id;

    private String userId;

    private String registrationNumber;

    private String vehicleMake;

    private String vehicleModel;

    private Integer yearOfManufacture;

    private BigDecimal vehicleValue;

    private String chassisNumber;

    private String engineNumber;
}
