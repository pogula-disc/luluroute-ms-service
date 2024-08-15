package com.luluroute.ms.service.helper;

import com.logistics.luluroute.avro.artifact.message.ArtifactBody;
import com.logistics.luluroute.avro.artifact.message.ArtifactHeader;
import com.logistics.luluroute.avro.artifact.message.Extended;
import com.logistics.luluroute.avro.artifact.message.LabelExtended;
import com.logistics.luluroute.avro.artifact.message.LabelInfo;
import com.logistics.luluroute.avro.artifact.message.Processes;
import com.logistics.luluroute.avro.artifact.message.ShipmentArtifact;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentLabelInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.luluroute.ms.service.util.LabelNamingConstants.FWD_SLASH;
import static com.luluroute.ms.service.util.LabelNamingConstants.UNDERSCORE;
import static com.luluroute.ms.service.util.ShipmentConstants.*;


@Component
@Slf4j
public class ShipmentArtifactHelper {

	public static final String ARTIFACT_TYPE_7100 = "7100";

	public static final Long ARTIFACT_STATUS_200 = 200l;

	public static final String LEGACY_LABEL_DATA_PUBLISHED = "Legacy Label Artifact Message Published";

	public ShipmentArtifact composeShipmentArtifact(ShipmentInfo shipmentInfo, ShipmentLabelInfo shipmentLabel, String shipmentCorrelationId, long startTime, String srcEntityCode) {
		return new ShipmentArtifact(this.prepareArtifactHeader(shipmentCorrelationId, startTime),
				this.prepareArtifactBody(shipmentInfo, shipmentLabel, srcEntityCode));
	}

	/**
	 * Prepare artifact header.
	 * @param shipmentCorrelationId the shipment correlation id
	 * @return the artifact header
	 */
	private ArtifactHeader prepareArtifactHeader(String shipmentCorrelationId, long startTime) {
		return new ArtifactHeader(Instant.now().getEpochSecond(), LEGACY_LABEL_ARTIFACT_TYPE_7400, ARTIFACT_STATUS_200,
				getRandomUuid(), getRandomUuid(), shipmentCorrelationId,
				this.prepareProcessData(startTime));
	}

	/**
	 * Prepare process data.
	 * @return the list
	 */
	private List<Processes> prepareProcessData(long startTime) {
		List<Processes> processes = new ArrayList<>();
		Extended data = new Extended("", "", "");
		List<Extended> extendedList = new ArrayList<>();
		extendedList.add(data);
		processes.add(new Processes(LEGACY_LABEL_DATA_PUBLISHED, ARTIFACT_STATUS_200, startTime,
				Instant.now().getEpochSecond(), getRandomUuid(), ARTIFACT_STATUS_200,
				null, extendedList));
		return processes;
	}

	private ArtifactBody prepareArtifactBody(ShipmentInfo shipmentInfo, ShipmentLabelInfo shipmentLabelInfo, String srcEntityCd) {

		ArtifactBody artifactBody = new ArtifactBody();
		try {
			String labelFileName = buildLabelS3FileName(shipmentInfo, srcEntityCd);
			LabelInfo labelInfo = prepareLabelInfo(shipmentLabelInfo, labelFileName);
			artifactBody.setLabelInfo(labelInfo);
		} catch (Exception e) {
			log.error(STANDARD_ERROR, "ShipmentArtifactHelper.prepareArtifactBody", "ShipmentArtifactHelper.prepareArtifactBody failed to prepare and set label info");
		}
		return artifactBody;
	}

	private String buildLabelS3FileName(ShipmentInfo shipmentInfo, String srcEntityCd) {
		try {
			String legacyCarrierCd = shipmentInfo.getShipmentHeader().getCarrier().getCarrierName();
			String correlationId = shipmentInfo.getShipmentHeader().getShipmentCorrelationId();
			String date = getDateString();
			StringBuilder filePath = new StringBuilder(srcEntityCd).append(FWD_SLASH).append(legacyCarrierCd).append(FWD_SLASH)
					.append(date).append(legacyCarrierCd).append(UNDERSCORE).append(srcEntityCd).append(UNDERSCORE).append(correlationId);
			return filePath.toString();
		} catch (Exception e) {
			log.debug("Error in constructing complete legacy label file name. Setting label name without source entity code or carrier code.");
			return null;
		}

	}

	private String getDateString() {
		LocalDate dateObj = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
		StringBuilder dateStr = new StringBuilder(dateObj.format(formatter)).append(UNDERSCORE);
		return dateStr.toString();
	}

	private LabelInfo prepareLabelInfo(ShipmentLabelInfo shipmentLabelInfo, String labelFileName) {
		LabelExtended labelExtended = new LabelExtended("", "", "");
		List<LabelExtended> labelExtendeds = new ArrayList<>();
		labelExtendeds.add(labelExtended);
		LabelInfo labelInfo = new LabelInfo();
		// Construct S3 File Path and Name to save in MS-LABEL service
		labelInfo.setLabelType(LABEL_TYPE);
		labelInfo.setLabelName(labelFileName);
		labelInfo.setLabelId(UUID.randomUUID().toString());
		labelInfo.setContentFromCarrier(shipmentLabelInfo.getLabel());
		labelInfo.setFormatFromCarrier(shipmentLabelInfo.getFormat());
		labelInfo.setFormatRendered(shipmentLabelInfo.getFormat());
		labelInfo.setGenerationDate(Instant.now().getEpochSecond());
		labelInfo.setLabelExtended(labelExtendeds);
		return labelInfo;
	}

	public static String getRandomUuid() {
		return UUID.randomUUID().toString();
	}

}

