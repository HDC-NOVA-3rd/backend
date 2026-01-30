package com.backend.nova.apartment.dto;

import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.apartment.entity.Dong;
import com.backend.nova.apartment.entity.Ho;

public class ApartmentStructure {
    public record ApartmentResponse(Long id, String name) {
        public static ApartmentResponse from(Apartment apartment) {
            return new ApartmentResponse(apartment.getId(), apartment.getName());
        }
    }

    public record DongResponse(Long id, String dongNo) {
        public static DongResponse from(Dong dong) {
            return new DongResponse(dong.getId(), dong.getDongNo());
        }
    }

    public record HoResponse(Long id, String hoNo) {
        public static HoResponse from(Ho ho) {
            return new HoResponse(ho.getId(), ho.getHoNo());
        }
    }
}