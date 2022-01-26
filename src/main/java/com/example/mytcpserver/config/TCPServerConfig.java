package com.example.mytcpserver.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.*;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.TcpInboundGateway;
import org.springframework.integration.ip.tcp.TcpOutboundGateway;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayLfSerializer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.Header;


@Configuration
@EnableIntegration
@IntegrationComponentScan
@Slf4j
public class TCPServerConfig {

    @Value("${socket.port}")
    private int port;

    /**
     * Reply messages are routed to the connection only if the reply contains the ip_connectionId header
     * that was inserted into the original message by the connection factory.
     */
    @MessagingGateway(defaultRequestChannel = "toTcp")
    public interface Gateway {
        void send(String message, @Header(IpHeaders.CONNECTION_ID) String connectionId);
    }

    //    Server and client ConnectionFactories
    @Bean
    public AbstractServerConnectionFactory serverConnectionFactory() {

                var clientCF = new TcpNetServerConnectionFactory(port);
                clientCF.setSerializer(new ByteArrayLfSerializer());
                clientCF.setDeserializer(new ByteArrayLfSerializer());
                return clientCF;
    }


    @Bean
    public AbstractClientConnectionFactory clientConnectionFactory() {
        return new TcpNetClientConnectionFactory("localhost", this.port);
    }

    @Bean
    @ServiceActivator(inputChannel = "toTcp")
    public MessageHandler tcpOutGate(AbstractClientConnectionFactory connectionFactory) {
        TcpOutboundGateway gate = new TcpOutboundGateway();
        gate.setConnectionFactory(connectionFactory);
        gate.setOutputChannelName("resultToString");
        return gate;
    }

    @Bean
    public TcpInboundGateway tcpInGate(AbstractServerConnectionFactory connectionFactory) {
        TcpInboundGateway inGate = new TcpInboundGateway();
        inGate.setConnectionFactory(connectionFactory);
        inGate.setRequestChannel(fromTcp());
        return inGate;
    }

    @Bean
    public MessageChannel fromTcp() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel toTcp() {
        return new DirectChannel();
    }

    @MessageEndpoint
    public static class Echo {

        @Transformer(inputChannel = "fromTcp", outputChannel = "toEcho")
        public String convert(byte[] bytes) {
            return new String(bytes);
        }

        @ServiceActivator(inputChannel = "toEcho")
        public String upCase(String in) {
            log.info(in);
            return in.toUpperCase();
        }

        @Transformer(inputChannel = "resultToString")
        public String convertResult(byte[] bytes) {
            return new String(bytes);
        }

    }


}
