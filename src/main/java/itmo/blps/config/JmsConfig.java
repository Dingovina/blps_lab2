package itmo.blps.config;

import com.rabbitmq.jms.admin.RMQConnectionFactory;
import jakarta.jms.ConnectionFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.JacksonJsonMessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

@EnableJms
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class JmsConfig {

    @Bean
    public ConnectionFactory connectionFactory(MessagingProperties properties) {
        MessagingProperties.Rabbitmq rabbitmq = properties.getRabbitmq();
        RMQConnectionFactory factory = new RMQConnectionFactory();
        factory.setHost(rabbitmq.getHost());
        factory.setPort(rabbitmq.getPort());
        factory.setUsername(rabbitmq.getUsername());
        factory.setPassword(rabbitmq.getPassword());
        factory.setVirtualHost(rabbitmq.getVirtualHost());
        return factory;
    }

    @Bean
    public MessageConverter jmsMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory, MessageConverter jmsMessageConverter) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setMessageConverter(jmsMessageConverter);
        return template;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jmsMessageConverter,
            MessagingProperties properties) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jmsMessageConverter);
        factory.setConcurrency(properties.getListenerConcurrency());
        return factory;
    }
}
