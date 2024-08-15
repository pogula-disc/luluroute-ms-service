package com.luluroute.ms.service.config;

import com.logistics.luluroute.avro.artifact.message.ShipmentArtifact;
import com.logistics.luluroute.avro.shipment.message.ShipmentMessage;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import java.util.Map;
import java.util.Properties;

@Configuration
@EnableKafka
public class KafkaConfig {

    public static final String SECURITY_PROTOCOL_KEY = "security.protocol";
    public static final String SASL_MECHANISM_KEY = "sasl.mechanism";
    public static final String SASL_JAAS_CONFIG_KEY = "sasl.jaas.config";
    public static final String AUTH_CREDENTIAL_SOURCE = "basic.auth.credentials.source";
    public static final String AUTH_REG_USER_INFO = "schema.registry.basic.auth.user.info";
    public static final String USER_INFO = "USER_INFO";

    @Value("${config.kafka.schema.registry.url}")
    private String schemaRegistryUrl;

    @Value("${config.kafka.auto.register.schemas}")
    private String autoRegisterSchemas;

    @Value("${config.kafka.basic.auth.user.info}")
    private String schemaRegistryUserInfo;

    @Value("${config.kafka.basic.auth.credentials.source}")
    private String basicAuthCredentialsSource;

    @Value("${config.shipment.group.id}")
    private String consumerGroupId;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServer;

    @Value("${spring.kafka.properties.sasl.jaas.config}")
    private String saslJaasConfig;

    @Value("${spring.kafka.properties.security.protocol}")
    private String securityProtocol;

    @Value("${spring.kafka.properties.sasl.mechanism}")
    private String saslMechanism;

    @Autowired
    private KafkaProperties kafkaProperties;

    @Bean
    public ProducerFactory<String, ShipmentMessage> messageProducerFactory() {
        Map<String, Object> configProps = this.kafkaProperties.buildProducerProperties();
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        configProps.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        configProps.put(AbstractKafkaAvroSerDeConfig.AUTO_REGISTER_SCHEMAS, autoRegisterSchemas);
        configProps.put(AbstractKafkaAvroSerDeConfig.USER_INFO_CONFIG, schemaRegistryUserInfo);
        configProps.put(AbstractKafkaAvroSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, basicAuthCredentialsSource);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public ProducerFactory<String, ShipmentArtifact> artifactProducerFactory() {
        Map<String, Object> configProps = this.kafkaProperties.buildProducerProperties();
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        configProps.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        configProps.put(AbstractKafkaAvroSerDeConfig.AUTO_REGISTER_SCHEMAS, autoRegisterSchemas);
        configProps.put(AbstractKafkaAvroSerDeConfig.USER_INFO_CONFIG, schemaRegistryUserInfo);
        configProps.put(AbstractKafkaAvroSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, basicAuthCredentialsSource);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean("kafkaTemplate")
    public KafkaTemplate<String, ShipmentMessage> messageKafkaTemplate() {
        return new KafkaTemplate<>(messageProducerFactory());
    }

    @Bean("artifactKafkaTemplate")
    public KafkaTemplate<String, ShipmentArtifact> artifactKafkaTemplate() {
        return new KafkaTemplate<>(artifactProducerFactory());
    }

    @Bean
    @Primary
    public ConsumerFactory<Object, Object> consumerFactory() {
        Map<String, Object> config = this.kafkaProperties.buildConsumerProperties();
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        config.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        config.put(AbstractKafkaAvroSerDeConfig.AUTO_REGISTER_SCHEMAS, autoRegisterSchemas);
        config.put(AbstractKafkaAvroSerDeConfig.USER_INFO_CONFIG, schemaRegistryUserInfo);
        config.put(AbstractKafkaAvroSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, basicAuthCredentialsSource);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public <T> ConcurrentKafkaListenerContainerFactory<String, T> containerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    public Properties getConsumerProperties() {
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.setProperty(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        properties.setProperty(SASL_JAAS_CONFIG_KEY, saslJaasConfig);
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId+ UUID.randomUUID());
        properties.setProperty(SECURITY_PROTOCOL_KEY, securityProtocol);
        properties.setProperty(SASL_MECHANISM_KEY, saslMechanism);
        properties.setProperty(AUTH_REG_USER_INFO, schemaRegistryUserInfo);
        properties.setProperty(AUTH_CREDENTIAL_SOURCE, USER_INFO);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        properties.setProperty(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, Boolean.TRUE.toString());
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, Boolean.TRUE.toString());

        return properties;
    }
}
