package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import com.hello.suripu.core.util.DataUtils;
import com.hello.suripu.core.util.calibration.SenseOneFiveDataConversion;

public class CalibratedDeviceData {

    private final DeviceData deviceData;
    private final Device.Color color;
    private final Optional<Calibration> calibration;

    public CalibratedDeviceData(final DeviceData deviceData, final Device.Color color, final Optional<Calibration> calibration) {
        this.deviceData = deviceData;
        this.color = color;
        this.calibration = calibration;
    }

    public float temperature() {
        // Sense one
        if(!deviceData.hasExtra()) {
            return DataUtils.calibrateTemperature(deviceData.ambientTemperature);
        }

        return SenseOneFiveDataConversion.convertRawToCelsius(
                deviceData.ambientTemperature,
                Optional.absent()
        );
    }

    public float humidity() {
        // Sense one
        if(!deviceData.hasExtra()) {
            return DataUtils.calibrateHumidity(deviceData.ambientTemperature, deviceData.ambientHumidity);
        }
        return SenseOneFiveDataConversion.convertRawToHumidity(deviceData.ambientHumidity);
    }

    //TODO: please refactor me - why is raw=>lux conversion done in DeviceDataDAODynamoDB?
    public float lux() {
        // Sense one
        if(!deviceData.hasExtra()) {
            return DataUtils.calibrateLight(deviceData.ambientLightFloat,color);
        }
        return SenseOneFiveDataConversion.convertLuxCountToLux(deviceData.extra().luxCount(), color);
    }

    public float particulates() {
        return DataUtils.convertRawDustCountsToDensity(deviceData.ambientAirQualityRaw, calibration);
    }

    public float sound(boolean useAudioPeakEnergy) {
        final Integer audioPeakDB;
        if (useAudioPeakEnergy && deviceData.audioPeakEnergyDB != 0) {
            audioPeakDB = deviceData.audioPeakEnergyDB;
        } else {
            audioPeakDB = deviceData.audioPeakDisturbancesDB;
        }
        return DataUtils.calibrateAudio(DataUtils.dbIntToFloatAudioDecibels(deviceData.audioPeakDisturbancesDB), DataUtils.dbIntToFloatAudioDecibels(audioPeakDB), deviceData.firmwareVersion);
    }

    public float soundPeakEnergy() {
        return DataUtils.dbIntToFloatAudioDecibels(deviceData.audioPeakDisturbancesDB);
    }

    public float soundPeakDisturbance() {
        return DataUtils.dbIntToFloatAudioDecibels(deviceData.audioPeakDisturbancesDB);
    }

    public float audioNumDisturbances() {
        return deviceData.audioNumDisturbances;
    }

    public float pressure() {
        if(deviceData.hasExtra()) {
            return SenseOneFiveDataConversion.convertRawToMilliBar(deviceData.extra().pressure());
        }
        return 0f;
    }

    public float tvoc() {
        if(deviceData.hasExtra()) {
            return SenseOneFiveDataConversion.convertRawToVOC(deviceData.extra().tvoc());
        }
        return 0f;
    }

    public float co2() {
        if(deviceData.hasExtra()) {
            return SenseOneFiveDataConversion.convertRawToCO2(deviceData.extra().co2());
        }
        return 0f;
    }
}
