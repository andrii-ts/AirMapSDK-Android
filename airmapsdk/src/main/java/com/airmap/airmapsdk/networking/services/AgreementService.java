package com.airmap.airmapsdk.networking.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.airmap.airmapsdk.models.agreements.Agreement;
import com.airmap.airmapsdk.models.agreements.AgreementStatus;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.networking.callbacks.EmptyCallback;
import com.airmap.airmapsdk.networking.callbacks.GenericListOkHttpCallback;
import com.airmap.airmapsdk.networking.callbacks.GenericOkHttpCallback;
import com.airmap.airmapsdk.networking.callbacks.VoidCallback;

import java.util.List;

import okhttp3.Call;

public class AgreementService extends BaseService {

    /**
     * Get a list of agreements for an authority by ID
     * @param authorityId The ID of the authority to get agreements for
     * @param listener The callback that is invoked on success or error
     * @return
     */
    public static Call getAgreements(@NonNull String authorityId,
                                     @Nullable AirMapCallback<List<Agreement>> listener){
        return AirMap.getClient().get(String.format(listAgreementsUrl, authorityId),
                new GenericListOkHttpCallback(listener, Agreement.class));
    }

    /**
     * Get the text for an agreement by ID
     * @param agreementId The ID of an agreement to get the text for
     * @param listener The callback that is invoked on success or error
     * @return
     */
    public static Call getAgreement(@NonNull String agreementId,
                                    @Nullable AirMapCallback<Agreement> listener){
        return AirMap.getClient().get(String.format(getAgreementUrl, agreementId),
                new GenericOkHttpCallback(listener, Agreement.class));
    }

    /**
     * Check if a user has accepted an agreement
     * @param agreementId The ID of agreement to check the status for
     * @param listener The callback that is invoked on success or error
     * @return
     */
    public static Call getAgreementStatus(@NonNull String agreementId,
                                          @Nullable AirMapCallback<AgreementStatus> listener){
        return AirMap.getClient().get(String.format(getAgreementStatusUrl, agreementId),
                new GenericOkHttpCallback(listener, AgreementStatus.class));
    }

    /**
     * Agree to an agreement by ID
     * @param agreementId The ID of the agreement to agree to
     * @param listener The callback that is invoked on success or error
     * @return
     */
    public static Call agreeToAgreement(@NonNull String agreementId,
                                        @Nullable AirMapCallback<Void> listener){
        return AirMap.getClient().post(String.format(acceptAgreementUrl, agreementId), new EmptyCallback(listener));
    }

}
