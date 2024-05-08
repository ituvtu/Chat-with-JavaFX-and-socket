package ituvtu.chat;

import jakarta.xml.bind.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class XMLUtil {
    private static final ConcurrentHashMap<Class<?>, JAXBContext> contextMap = new ConcurrentHashMap<>();

    private static JAXBContext getContext(Class<?> clazz) {
        return contextMap.computeIfAbsent(clazz, c -> {
            try {
                return JAXBContext.newInstance(c);
            } catch (JAXBException e) {
                throw new RuntimeException("Failed to create JAXBContext for class: " + c.getName(), e);
            }
        });
    }

    public static <T> String toXML(T obj) throws JAXBException {
        JAXBContext context = getContext(obj.getClass());
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter writer = new StringWriter();
        marshaller.marshal(obj, writer);
        return writer.toString();
    }

    public static <T> T fromXML(String xml, Class<T> clazz) throws JAXBException {
        JAXBContext context = getContext(clazz);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return clazz.cast(unmarshaller.unmarshal(new StringReader(xml)));
    }

}
