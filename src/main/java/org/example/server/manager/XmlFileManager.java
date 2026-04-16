package org.example.server.manager;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.example.common.models.Organization;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class XmlFileManager {
    private final XmlMapper xmlMapper;
    private final Path filePath;

    public XmlFileManager(String fileName) {
        this.xmlMapper = XmlMapper.builder()
                .defaultUseWrapper(false)
                .build();
        this.xmlMapper.registerModule(new JavaTimeModule());

        this.xmlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);


        this.xmlMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        this.filePath = Path.of(fileName);
    }
    /**
     * Внутренний класс-обёртка для корректной сериализации списка организаций в XML
     * Представляет корневой элемент {@code <organizations>} и элементы списка {@code <organization>}
     */

    @JacksonXmlRootElement(localName = "organizations")
    public static class OrganizationsWrapper {
        /**
         * Список организаций, сериализуемых как повторяющиеся элементы {@code <organization>}.
         */
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "organization")
        public List<Organization> organizations;

        /**
         * Пустой конструктор, используется для десериализации Jackson
         */

        public OrganizationsWrapper() {}

        /**
         * Конструктор с параметрами.
         *
         * @param orgs список организаций для обёртки
         */
        public OrganizationsWrapper(List<Organization> orgs) {
            this.organizations = new ArrayList<>(orgs);
        }
    }

    /**
     * Загружает коллекцию организаций из XML-файла.
     * Если файл не существует, возвращает пустой список.
     * Использует BufferedReader для чтения, как того требует задание.
     *
     * @return список организаций; никогда не {@code null}
     * @throws IOException если произошла ошибка при чтении файла
     */

    public List<Organization> readCollection() throws IOException {
        if (!filePath.toFile().exists()){
            return new ArrayList<>();
        }
        try (
                FileInputStream fis = new FileInputStream(filePath.toFile());
                InputStreamReader isr = new InputStreamReader(fis, java.nio.charset.StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(isr)
        )
        {
            OrganizationsWrapper wrapper = xmlMapper.readValue(reader, OrganizationsWrapper.class);
            return wrapper.organizations != null ? wrapper.organizations : new ArrayList<>();
        }
    }

    /**
     * Сохраняет коллекцию организаций в XML-файл
     * Файл создаётся автоматически, если не существует
     * Использует OutputStreamWriter для записи, как того требует задание
     *
     * @param collection коллекция организаций для сохранения
     * @throws IOException если произошла ошибка при записи файла
     */

    public void writeCollection(List<Organization> collection) throws IOException {
        OrganizationsWrapper wrapper = new OrganizationsWrapper(collection);

        try (
                FileOutputStream fos = new FileOutputStream(filePath.toFile());
                OutputStreamWriter osw = new OutputStreamWriter(fos, java.nio.charset.StandardCharsets.UTF_8)
        ) {
            xmlMapper.writeValue(osw, wrapper);
        }
    }
}
