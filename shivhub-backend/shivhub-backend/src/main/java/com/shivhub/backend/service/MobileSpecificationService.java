package com.shivhub.backend.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivhub.backend.dto.MobileSpecificationRequest;
import com.shivhub.backend.dto.MobileSpecificationResponse;
import com.shivhub.backend.entity.MobileSpecification;
import com.shivhub.backend.repository.MobileSpecificationRepository;

@Service
public class MobileSpecificationService {

    private final MobileSpecificationRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${mobile.spec.api.base-url:}")
    private String externalApiBaseUrl;

    @Value("${mobile.spec.api.key:}")
    private String externalApiKey;

    @Value("${mobile.spec.api.provider:EXTERNAL}")
    private String externalApiProvider;

    public MobileSpecificationService(MobileSpecificationRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public List<MobileSpecificationResponse> searchActive(String query) {
        String value = clean(query);
        List<MobileSpecification> specs = value.isBlank()
                ? repository.findByActiveTrueOrderByBrandAscModelNameAscVariantNameAsc()
                : repository.findByBrandContainingIgnoreCaseOrModelNameContainingIgnoreCaseOrderByBrandAscModelNameAscVariantNameAsc(value, value)
                        .stream()
                        .filter(spec -> Boolean.TRUE.equals(spec.getActive()))
                        .toList();

        return specs.stream().map(this::map).toList();
    }

    public List<MobileSpecificationResponse> searchForAdmin(String query) {
        String value = clean(query);
        List<MobileSpecification> specs = value.isBlank()
                ? repository.findAll()
                : repository.findByBrandContainingIgnoreCaseOrModelNameContainingIgnoreCaseOrderByBrandAscModelNameAscVariantNameAsc(value, value);

        return specs.stream().map(this::map).toList();
    }

    public MobileSpecificationResponse fetchExternalPreview(String brand, String model) {
        String cleanBrand = required(brand);
        String cleanModel = required(model);

        if (externalApiBaseUrl == null || externalApiBaseUrl.isBlank()) {
            throw new RuntimeException("Mobile specs API is not configured. Add mobile.spec.api.base-url and mobile.spec.api.key in application.properties.");
        }

        try {
            URI uri = URI.create(
                    externalApiBaseUrl
                            + (externalApiBaseUrl.contains("?") ? "&" : "?")
                            + "brand=" + encode(cleanBrand)
                            + "&model=" + encode(cleanModel)
            );

            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .GET()
                    .header("Accept", "application/json");

            if (externalApiKey != null && !externalApiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + externalApiKey);
                builder.header("X-API-Key", externalApiKey);
            }

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Mobile specs provider returned HTTP " + response.statusCode());
            }

            return mapExternalJson(cleanBrand, cleanModel, objectMapper.readTree(response.body()));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Mobile specs API request was interrupted", exception);
        } catch (Exception exception) {
            throw new RuntimeException("Could not fetch mobile specifications from external API", exception);
        }
    }

    @Transactional
    public MobileSpecificationResponse create(MobileSpecificationRequest request) {
        MobileSpecification spec = new MobileSpecification();
        apply(spec, request);
        return map(repository.save(spec));
    }

    @Transactional
    public MobileSpecificationResponse update(Long id, MobileSpecificationRequest request) {
        MobileSpecification spec = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mobile specification not found"));

        apply(spec, request);
        return map(repository.save(spec));
    }

    @Transactional
    public MobileSpecificationResponse changeStatus(Long id, boolean active) {
        MobileSpecification spec = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mobile specification not found"));

        spec.setActive(active);
        return map(repository.save(spec));
    }

    private void apply(MobileSpecification spec, MobileSpecificationRequest request) {
        spec.setBrand(required(request.getBrand()));
        spec.setModelName(required(request.getModelName()));
        spec.setVariantName(clean(request.getVariantName()));
        spec.setRam(clean(request.getRam()));
        spec.setStorage(clean(request.getStorage()));
        spec.setColorOptions(clean(request.getColorOptions()));
        spec.setHsnCode(defaultValue(request.getHsnCode(), "85171300"));
        spec.setDisplayDetails(clean(request.getDisplayDetails()));
        spec.setProcessor(clean(request.getProcessor()));
        spec.setCameraDetails(clean(request.getCameraDetails()));
        spec.setBatteryDetails(clean(request.getBatteryDetails()));
        spec.setOsDetails(clean(request.getOsDetails()));
        spec.setConnectivityDetails(clean(request.getConnectivityDetails()));
        spec.setOtherDetails(clean(request.getOtherDetails()));
        spec.setIndiaVariant(request.getIndiaVariant() == null ? true : request.getIndiaVariant());
        spec.setActive(request.getActive() == null ? true : request.getActive());
        spec.setSource(defaultValue(request.getSource(), "MANUAL"));
    }

    private MobileSpecificationResponse map(MobileSpecification spec) {
        MobileSpecificationResponse response = new MobileSpecificationResponse();
        response.setId(spec.getId());
        response.setBrand(spec.getBrand());
        response.setModelName(spec.getModelName());
        response.setVariantName(spec.getVariantName());
        response.setRam(spec.getRam());
        response.setStorage(spec.getStorage());
        response.setColorOptions(spec.getColorOptions());
        response.setHsnCode(spec.getHsnCode());
        response.setDisplayDetails(spec.getDisplayDetails());
        response.setProcessor(spec.getProcessor());
        response.setCameraDetails(spec.getCameraDetails());
        response.setBatteryDetails(spec.getBatteryDetails());
        response.setOsDetails(spec.getOsDetails());
        response.setConnectivityDetails(spec.getConnectivityDetails());
        response.setOtherDetails(spec.getOtherDetails());
        response.setIndiaVariant(spec.getIndiaVariant());
        response.setActive(spec.getActive());
        response.setSource(spec.getSource());
        response.setCreatedAt(spec.getCreatedAt());
        response.setUpdatedAt(spec.getUpdatedAt());
        return response;
    }

    private MobileSpecificationResponse mapExternalJson(String brand, String model, JsonNode root) {
        JsonNode data = root;
        if (root.has("data")) data = root.get("data");
        if (data.isArray() && !data.isEmpty()) data = data.get(0);

        MobileSpecificationResponse response = new MobileSpecificationResponse();
        response.setBrand(value(data, "brand", brand));
        response.setModelName(value(data, "modelName", value(data, "model", value(data, "name", model))));
        response.setVariantName(value(data, "variantName", value(data, "variant", "")));
        response.setRam(value(data, "ram", ""));
        response.setStorage(value(data, "storage", ""));
        response.setColorOptions(value(data, "colorOptions", value(data, "colors", "")));
        response.setHsnCode(value(data, "hsnCode", "85171300"));
        response.setDisplayDetails(value(data, "displayDetails", value(data, "display", "")));
        response.setProcessor(value(data, "processor", value(data, "chipset", "")));
        response.setCameraDetails(value(data, "cameraDetails", value(data, "camera", "")));
        response.setBatteryDetails(value(data, "batteryDetails", value(data, "battery", "")));
        response.setOsDetails(value(data, "osDetails", value(data, "os", "")));
        response.setConnectivityDetails(value(data, "connectivityDetails", value(data, "connectivity", "")));
        response.setOtherDetails(value(data, "otherDetails", compactJson(data)));
        response.setIndiaVariant(true);
        response.setActive(true);
        response.setSource(externalApiProvider + "_PREVIEW");
        return response;
    }

    private String value(JsonNode node, String field, String fallback) {
        if (node == null || !node.has(field) || node.get(field).isNull()) return fallback;
        JsonNode value = node.get(field);
        if (value.isArray()) {
            return String.join(", ",
                    java.util.stream.StreamSupport.stream(value.spliterator(), false)
                            .map(entry -> entry.isTextual() ? entry.asText() : entry.toString())
                            .toList());
        }
        return value.isTextual() ? value.asText() : value.toString();
    }

    private String compactJson(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return "";
        String value = node.toString();
        return value.length() > 4000 ? value.substring(0, 4000) : value;
    }

    private String required(String value) {
        String cleaned = clean(value);
        if (cleaned.isBlank()) {
            throw new RuntimeException("Brand and model name are required");
        }
        return cleaned;
    }

    private String defaultValue(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? fallback : cleaned;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
