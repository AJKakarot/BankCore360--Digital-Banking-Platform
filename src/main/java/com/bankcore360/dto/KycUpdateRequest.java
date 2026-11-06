package com.bankcore360.dto;

import com.bankcore360.entity.KycStatus;
import jakarta.validation.constraints.NotNull;

public class KycUpdateRequest {

    @NotNull(message = "targetStatus is required")
    private KycStatus targetStatus;

    public KycUpdateRequest() {
    }

    public KycUpdateRequest(KycStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public KycStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(KycStatus targetStatus) {
        this.targetStatus = targetStatus;
    }
}
