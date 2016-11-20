package translator;

import com.google.gson.Gson;
import com.rabbitmq.client.AMQP.*;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import connector.RabbitMQConnector;
import models.Data;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public class JSONTranslator {

    private final RabbitMQConnector connector = new RabbitMQConnector();

    private Channel channel;
    private String queueName;
    private final String EXCHANGENAME = "whatTranslator.json";
    private final String BANKEXCHANGENAME = "cphbusiness.bankJSON";
    private final String REPLYTOQUENAME = "whatNormalizerQueue";

    public void init() throws IOException {
        channel = connector.getChannel();
        channel.exchangeDeclare(EXCHANGENAME, "direct");
        queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGENAME, "");
        receive();
    }

    private boolean receive() throws IOException {

        System.out.println(" [*] Waiting for messages.");
        final Consumer consumer = new DefaultConsumer(channel) {

            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) throws IOException {

                System.out.println(" [x] Received ");
                try {
                    send(properties, body);
                }
                catch (JAXBException ex) {
                    Logger.getLogger(JSONTranslator.class.getName()).log(Level.SEVERE, null, ex);
                }
                finally {
                    System.out.println(" [x] Done");
                    channel.basicAck(envelope.getDeliveryTag(), false);
                }
            }
        };
        channel.basicConsume(queueName, false, consumer);
        return true;
    }

    private Data unmarchal(String bodyString) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(Data.class);
        Unmarshaller unmarchaller = jc.createUnmarshaller();
        StringReader reader = new StringReader(bodyString);
        return (Data) unmarchaller.unmarshal(reader);
    }

    private String removeBom(String xmlString) {
        String res = xmlString.trim();
        return res.substring(res.indexOf("<?xml"));
    }

    private BasicProperties propBuilder(String corrId,Map<String,Object> headers) {
        BasicProperties.Builder builder = new BasicProperties.Builder();
        builder.replyTo(REPLYTOQUENAME);
        System.out.println(headers.get("bankName"));
        builder.headers(headers);
        builder.correlationId(corrId);
        BasicProperties prop = builder.build();
        return prop;
    }

    public boolean send(BasicProperties prop, byte[] body) throws JAXBException {

        try {String corrId= prop.getCorrelationId();
            BasicProperties newProp = propBuilder(corrId, prop.getHeaders());
            String bodyString = removeBom(new String(body));
            Data data = unmarchal(bodyString);
            int months = data.getLoanDuration() * 12;
            data.setLoanDuration(months);
            Gson gson = new Gson();
            String jsonString = gson.toJson(data);
            System.out.println("sending to "+BANKEXCHANGENAME+" "+jsonString);
            channel.basicPublish(BANKEXCHANGENAME, "", newProp, jsonString.getBytes());
            return true;
        }
        catch (IOException ex) {
            Logger.getLogger(JSONTranslator.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
}
