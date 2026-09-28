package com.shivhub.backend.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** Map secrets stay server-side. Provider failures return an explicit estimated fallback. */
@Component
public class DeliveryMapGateway {
    private final String key; private final ObjectMapper mapper;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    public record Coordinates(double latitude,double longitude){}
    public DeliveryMapGateway(@Value("${shivhub.maps.api-key:${SHIVHUB_MAPS_API_KEY:}}")String key,ObjectMapper mapper){this.key=key;this.mapper=mapper;}
    public Optional<Coordinates> geocode(String address){
        if(key.isBlank()||address==null||address.isBlank())return Optional.empty();
        try{
            URI uri=URI.create("https://maps.googleapis.com/maps/api/geocode/json?address="+URLEncoder.encode(address,java.nio.charset.StandardCharsets.UTF_8)+"&region=in&key="+URLEncoder.encode(key,java.nio.charset.StandardCharsets.UTF_8));
            var json=request(HttpRequest.newBuilder(uri).GET().timeout(Duration.ofSeconds(4)).build());
            if(!"OK".equals(json.path("status").asText()))return Optional.empty();
            var result=json.path("results").path(0);
            if(result.path("partial_match").asBoolean(false))return Optional.empty();
            var geometry=result.path("geometry");String locationType=geometry.path("location_type").asText();
            if(!Set.of("ROOFTOP","RANGE_INTERPOLATED","GEOMETRIC_CENTER").contains(locationType))return Optional.empty();
            var location=geometry.path("location");if(!location.path("lat").isNumber()||!location.path("lng").isNumber())return Optional.empty();
            return Optional.of(new Coordinates(location.path("lat").asDouble(),location.path("lng").asDouble()));
        }catch(InterruptedException e){Thread.currentThread().interrupt();return Optional.empty();}catch(Exception e){return Optional.empty();}
    }
    public OptionalDouble roadDistance(Coordinates origin,Coordinates destination){
        if(key.isBlank())return OptionalDouble.empty();
        try{
            String body=mapper.writeValueAsString(Map.of("origin",waypoint(origin),"destination",waypoint(destination),"travelMode","DRIVE","routingPreference","TRAFFIC_UNAWARE"));
            var request=HttpRequest.newBuilder(URI.create("https://routes.googleapis.com/directions/v2:computeRoutes")).timeout(Duration.ofSeconds(4)).header("Content-Type","application/json").header("X-Goog-Api-Key",key).header("X-Goog-FieldMask","routes.distanceMeters").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var distance=request(request).path("routes").path(0).path("distanceMeters");
            return distance.isNumber()&&distance.asDouble()>=0?OptionalDouble.of(distance.asDouble()/1000d):OptionalDouble.empty();
        }catch(InterruptedException e){Thread.currentThread().interrupt();return OptionalDouble.empty();}catch(Exception e){return OptionalDouble.empty();}
    }
    private Map<String,Object> waypoint(Coordinates c){return Map.of("location",Map.of("latLng",Map.of("latitude",c.latitude(),"longitude",c.longitude())));}
    private com.fasterxml.jackson.databind.JsonNode request(HttpRequest request)throws Exception{var response=http.send(request,HttpResponse.BodyHandlers.ofString());if(response.statusCode()!=200)throw new IllegalStateException("Map provider unavailable");return mapper.readTree(response.body());}
    public static double straightLineKm(Coordinates a,Coordinates b){double dlat=Math.toRadians(b.latitude()-a.latitude()),dlon=Math.toRadians(b.longitude()-a.longitude());double h=Math.pow(Math.sin(dlat/2),2)+Math.cos(Math.toRadians(a.latitude()))*Math.cos(Math.toRadians(b.latitude()))*Math.pow(Math.sin(dlon/2),2);return 6371.0088*2*Math.atan2(Math.sqrt(h),Math.sqrt(Math.max(0,1-h)));}
}
