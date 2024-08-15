package com.luluroute.ms.service.util;

public interface ShipmentConstants {

    long SHIPMENT_STATUS_NEW = 1000L;

    int ADDR_TYPE_ORIGIN = 1;

    int ADDR_TYPE_DESTINATION = 2;

    int ADDR_TYPE_RETURN = 3;

    String FAILURE_RESPONSE = "9000";

    String SUCCESS_RESPONSE = "1000";

    String SFS_UK = "SFSUK";

    String SHIP_FROM_STORE_PREFIX = "SFS";

    String ROLE_TYPE_DC_PRIMARY = "DC Primary";

    String ROLE_TYPE_OMNI_SFS_PRIMARY = "OMNI SFS Primary";

    int STORE_NUMBER_LENGTH_WITH_ZEROS = 5;

    String INTEGRATION = "Integration";

    String X_CORRELATION_ID = "X-Correlation-Id";

    String X_SHIPMENT_CORRELATION_ID = "X-Shipment-Correlation-Id";

    String USER_ID = "User-Id";
    String USER_ZIP_CODE = "Zip-Code";
    String USER_DC_ENTITY = "Dc-Entity";

    String SOAP_CONTEXT = "Soap-Context";
    String CANCEL_CONTEXT = "Cancel-Context";

    String ENTERING_MESSAGE = "APP_MESSAGE=Order Received in ISL | Correlation Id -  {}";
    String ENTERING_MESSAGE_SOAP = "APP_MESSAGE=Request Received in ISL (SOAP/Web Services) | XML - {}";

    String EXITING_MESSAGE = "APP_MESSAGE=Order Response Sent from ISL | Correlation Id -  {} | Time - {} ms";
    String EXITING_MESSAGE_SOAP = "APP_MESSAGE=Response Sent from ISL (SOAP/Web Services) | XML - {}";
    String PROCESSING_TIME_SOAP = "APP_MESSAGE= Exiting ISL (SOAP/Web Services) | Time - {} ms";

    String STANDARD_MESSAGE = "APP_MESSAGE= {} | Correlation Id -  {} | Time {} ms";

    String STANDARD_INFO = "APP_MESSAGE= METHOD=\"{%s}\" | Identifier# \"{%s}\" | Value # \"{%s}\" ";

    String MESSAGE_REDIS_KEY_LOADING = "APP_MESSAGE=\"Loading %s for key %s from Redis";

    String CODE_PATH = "codePath";

    String SUCCESS_MESSAGE = "APP_MESSAGE=\"Successfully posted to Legacy shipment service\"";

    String SCOPE_PREFIX = "SCOPE_";

    String KEY_DELIMITER = "_";

    String US = "US";

    String CA = "CA";

    String APP_HEADER = "Application";

    String APP_VALUE = "LuluRoute_2.0";

    String X_TRANSACTION_REFERENCE = "X-Transaction-Reference";

    String STANDARD_ERROR = "APP_MESSAGE=Error Occurred | METHOD=\"{}\" | ERROR=\"{}\"";

    String LOCATION_ORIGIN = "ORIGIN";

    String LOCATION_DEST = "DEST";

    String MSG_NOT_PROCESSED = "Shipment message is not processed!";

    String RESPONSE_PERSISTED_SUCCESSFULLY = "Shipment response is persisted to DB";

    String REQUEST_TYPE = "RequestType";

    String REQUEST_TYPE_SHIPMENT = "2000";

    String TS_CREATED = "tsCreated";

    String MSG_ENTITY_CODE = "MessageEntityCode";

    String ORIGIN_ENTITY_CODE = "OriginEntityCode";

    String INVALID_DATE = "is invalid or before today's date";

    String DEFAULT_PHONE = "0000000000";
    
    String CARRIER_CODE = "CarrierCode";

    String LABEL_TYPE = "Legacy";

    String LEGACY_LABEL_ARTIFACT_TYPE_7400 = "7400";

    String SHIPMENT_CREATED_TEXT = "SHIPMENT_CREATED";
    String SHIPMENT_NOT_VALID_TEXT = "SHIPMENT_NOT_VALID";
    String SHIPMENT_NOT_CREATED_TEXT = "SHIPMENT_NOT_CREATED";
    String SHIPMENT_CANCELED_TEXT = "SHIPMENT_CANCELED";
    String SHIPMENT_CANCELED_TEXT_SOAP = "Shipment Cancelled";
    String SHIPMENT_INITIATED_TEXT = "SHIPMENT_INITIATED";
    String SHIPMENT_NOT_FOUND = "SHIPMENT_NOT_FOUND";
    String UPDATED_BY = "System User";
    long CANCEL_SHIPMENT_SUCCESS_REQUEST_TYPE = 9989;
    String CANCEL_SHIPMENT_SUCCESS_RESPONSE_CODE = "1000";
    String CANCEL_RESPONSE_ALREADY_CANCELLED = "Failed: Order Already Cancelled";
    String CANCEL_RESPONSE_SHIPMENT_NOT_FOUND = "Failed: Shipment Not Found";
    String CANCEL_RESPONSE_TIMEOUT = "Failed: Timeout occurred while waiting for cancel shipment response.";
    String US_TERRITORY_DEST_ERROR_MSG = "Destination Address is a US-Territory | EXPECTED: Country=%s | ACTUAL: Country=%s, State=%s";
    String SOAP_INBOUND_NAME_SPACE = "http://ws.enroutecorp.com/";
    String SOAP_INBOUND_PORT_TYPE = "Enroute_Web_ServiceSoap";
    String SOAP_INBOUND_URI = "/v1/soap-ws";
    String SOAP_INBOUND_XSD = "XmlShipmentCreateAndExecuteInbound.xsd";
    String SOAP_INBOUND_ACTION_SHIPMENT_CREATE_AND_EXECUTE = "xml_shipment_create_and_execute";
    String SOAP_INBOUND_ACTION_SHIPMENT_CANCEL = "shipment_cancel";
    String SOAP_INBOUND_ACTION_AUTHENTICATE_GET_TOKEN = "authentication_get_token";
    String SOAP_INBOUND_ACTION_AUTHENTICATE_VALIDATE_TOKEN = "authentication_validate_token";
    String SOAP_LEGACY_PACKAGE = "com.enroutecorp.ws.outbound";
    String SOAP_LEGACY_ACTION_SHIPMENT_CREATE_AND_EXECUTE = "http://ws.enroutecorp.com/xml_shipment_create_and_execute";
    String SOAP_LEGACY_ACTION_SHIPMENT_CANCEL = "http://ws.enroutecorp.com/shipment_cancel";
    String SOAP_LEGACY_ACTION_AUTHENTICATION_VALIDATE_TOKEN = "http://ws.enroutecorp.com/authentication_validate_token";
    String SOAP_LEGACY_ACTION_AUTHENTICATION_GET_TOKEN = "http://ws.enroutecorp.com/authentication_get_token";
    String LEGACY_AUTHENTICATION_FAILED = "Failed: Legacy Enroute authentication failure";
    String LEGACY_AUTHENTICATION_SUCCESS = "APP_MESSAGE=\"Successfully authenticated user\"";
    String SOAP_UNEXPECTED_ERROR = "Failed: Unexpected error while handling SOAP request";
    String SOAP_NO_RECOMMENDED_SERVICE_ERROR = "No recommended service found!";
    String SOAP_UNKNOWN_ERROR = "Shipment failure with unknown cause";
    String PR_COUNTRY = "PR";

    String SFS_CA = "SFSCA";
    String SFS_US = "SFSUSA";

}
