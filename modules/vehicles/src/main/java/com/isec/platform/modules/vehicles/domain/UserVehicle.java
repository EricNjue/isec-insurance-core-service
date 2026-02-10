package com.isec.platform.modules.vehicles.domain;

import com.isec.platform.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "user_vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVehicle extends BaseEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String registrationNumber;

    @Column(nullable = false)
    private String vehicleMake;

    @Column(nullable = false)
    private String vehicleModel;

    @Column(nullable = false)
    private Integer yearOfManufacture;

    @Column(nullable = false)
    private BigDecimal vehicleValue;

    @Column(name = "chassis_number")
    private String chassisNumber;

    @Column(name = "engine_number")
    private String engineNumber;

    @Override
    public void onCreate() {
        super.onCreate();
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
