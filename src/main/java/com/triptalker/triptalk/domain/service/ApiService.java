package com.triptalker.triptalk.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.triptalker.triptalk.domain.entity.Location;
import com.triptalker.triptalk.domain.entity.LocationDetail;
import com.triptalker.triptalk.domain.entity.Vertex;
import com.triptalker.triptalk.domain.repository.LocationDetailRepository;
import com.triptalker.triptalk.domain.repository.LocationRepository;
import com.triptalker.triptalk.domain.repository.VertexRepository;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class ApiService {
    private final LocationRepository locationRepository;
    private final LocationDetailRepository locationDetailRepository;
    private final VertexRepository vertexRepository;

    @Value("${openapi.servicekey}")
    private String serviceKey;

    @Value("${kakao.apiKey}")
    private String KakaoApiKey;

    @Transactional
    public String getThemeLocationBasedList() {

        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        String url = "https://apis.data.go.kr/B551011/Odii/themeLocationBasedList"
                + "?numOfRows=200&pageNo=1"
                + "&MobileOS=AND"
                + "&MobileApp=triptalk"
                + "&serviceKey=" + serviceKey
                + "&_type=json"
                + "&mapX=129.0105504845115"
                + "&mapY=35.0974835023459"
                + "&radius=1000"
                + "&langCode=ko";

        return WebClient.builder()
                .uriBuilderFactory(factory)
                .build()
                .get()
                .uri(url)              // 전체 URL 사용
                .retrieve()            // 응답을 받아옴
                .bodyToMono(String.class)
                .block();  // 응답 바디를 String으로 변환
    }

    @Transactional
    public String getLocation(String pageNum) {

        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);


        String url = "https://apis.data.go.kr/B551011/Odii/themeBasedList"
                + "?numOfRows=100&pageNo=" + pageNum
                + "&MobileOS=AND"
                + "&MobileApp=triptalk"
                + "&serviceKey=" + serviceKey
                + "&_type=json"
                + "&langCode=ko";

//            System.out.println(url);

        String json = WebClient.builder()
                .uriBuilderFactory(factory)
                .build()
                .get()
                .uri(url)              // 전체 URL 사용
                .retrieve()            // 응답을 받아옴
                .bodyToMono(String.class)  // 응답 바디를 String으로 변환
                .block();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(json); // JSON 문자열을 JsonNode로 파싱
            JsonNode itemNode = rootNode
                    .path("response") // "response" 노드로 이동
                    .path("body")     // "body" 노드로 이동
                    .path("items")    // "items" 노드로 이동
                    .path("item");

            if (itemNode.isArray()) {
                for (JsonNode item : itemNode) {
                    Location location = new Location();
                    location.setTid(item.path("tid").asText());
                    location.setTlid(item.path("tlid").asText());
                    location.setThemeCategory(item.path("themeCategory").asText());
                    location.setLocationName(item.path("title").asText());
                    location.setAddr1(item.path("addr1").asText());
                    location.setAddr2(item.path("addr2").asText());
                    location.setMapX(item.path("mapX").asDouble());
                    location.setMapY(item.path("mapY").asDouble());
                    location.setImageUrl(item.path("imageUrl").asText());
                    location.setCreatedAt(StringToLocalDateTime(item.path("createdtime").asText()));
                    location.setCreatedAt(StringToLocalDateTime(item.path("modifiedtime").asText()));

                    locationRepository.save(location);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


        return WebClient.builder()
                .uriBuilderFactory(factory)
                .build()
                .get()
                .uri(url)              // 전체 URL 사용
                .retrieve()            // 응답을 받아옴
                .bodyToMono(String.class)  // 응답 바디를 String으로 변환
                .block();

    }

    @Transactional
    public boolean getLocationAudio(String tid, String tlid) {

        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);


        String url = "https://apis.data.go.kr/B551011/Odii/storyBasedList"
                + "?numOfRows=100&pageNo=1"
                + "&MobileOS=AND"
                + "&MobileApp=triptalk"
                + "&serviceKey=" + serviceKey
                + "&_type=json"
                + "&langCode=ko"
                + "&tid=" + tid
                + "&tlid=" + tlid;

//            System.out.println(url);

        String json = WebClient.builder()
                .uriBuilderFactory(factory)
                .build()
                .get()
                .uri(url)              // 전체 URL 사용
                .retrieve()            // 응답을 받아옴
                .bodyToMono(String.class)  // 응답 바디를 String으로 변환
                .block();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(json); // JSON 문자열을 JsonNode로 파싱
            JsonNode itemNode = rootNode
                    .path("response") // "response" 노드로 이동
                    .path("body")     // "body" 노드로 이동
                    .path("items")    // "items" 노드로 이동
                    .path("item");

            if (itemNode.isArray()) {
                Optional<Location> locationOpt = locationRepository.findByTidAndTlid(tid, tlid);

                Location location;
                if (locationOpt.isPresent()) {
                    // If location exists, use it
                    location = locationOpt.get();
                    for (JsonNode item : itemNode) {
                        LocationDetail locationDetail = new LocationDetail();
                        locationDetail.setLocation(location); // Location과 연결
                        locationDetail.setStid(item.path("stid").asText());
                        locationDetail.setStlid(item.path("stlid").asText());
                        locationDetail.setTitle(item.path("title").asText());
                        locationDetail.setMapX(item.path("mapX").asText());
                        locationDetail.setMapY(item.path("mapY").asText());
                        locationDetail.setImageUrl(item.path("imageUrl").asText());
                        locationDetail.setAudioTitle(item.path("audioTitle").asText());
                        locationDetail.setScript(item.path("script").asText());
                        locationDetail.setPlayTime(item.path("playTime").asText());
                        locationDetail.setAudioUrl(item.path("audioUrl").asText());
                        locationDetail.setLangCode(item.path("langCode").asText());
                        locationDetail.setCreatedAt(StringToLocalDateTime(item.path("createdtime").asText()));
                        locationDetail.setCreatedAt(StringToLocalDateTime(item.path("modifiedtime").asText()));

                        locationDetailRepository.save(locationDetail);
                    }
                }

            }
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Transactional
    public String sendLocationToKakao(String tid, String tlid) throws JsonProcessingException {

        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);


        String url = "https://apis-navi.kakaomobility.com/v1/waypoints/directions";

        // tid, tlid에 해당하는 Location 검색
        Optional<Location> locationOpt = locationRepository.findByTidAndTlid(tid, tlid);

        if (!locationOpt.isPresent()) {
            throw new RuntimeException("Location not found for tid: " + tid + " and tlid: " + tlid);
        }
        Location location = locationOpt.get();

        // Location에 해당하는 LocationDetails 가져오기
        List<LocationDetail> locationDetails = locationDetailRepository.findByLocation(location);

        if (locationDetails.isEmpty()) {
            throw new RuntimeException("LocationDetails not found for location id: " + location.getId());
        }

        // 출발지(origin): LocationDetails의 첫 번째
        LocationDetail originDetail = locationDetails.get(0);
        // 도착지(destination): LocationDetails의 마지막
        LocationDetail destinationDetail = locationDetails.get(locationDetails.size() - 1);

        // 경유지(waypoints): 첫 번째와 마지막을 제외한 나머지
        List<Map<String, Object>> waypoints = new ArrayList<>();
        for (int i = 1; i < locationDetails.size() - 1; i++) {
            LocationDetail waypointDetail = locationDetails.get(i);
            Map<String, Object> waypointMap = new HashMap<>();
            waypointMap.put("name", waypointDetail.getTitle());
            waypointMap.put("x", waypointDetail.getMapX());
            waypointMap.put("y", waypointDetail.getMapY());
            waypoints.add(waypointMap);
        }

        // 요청 본문(body) 생성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("origin", createLocationMap(originDetail));
        requestBody.put("destination", createLocationMap(destinationDetail));
        requestBody.put("waypoints", waypoints);
        requestBody.put("priority", "RECOMMEND");
        requestBody.put("car_fuel", "GASOLINE");
        requestBody.put("car_hipass", false);
        requestBody.put("alternatives", false);
        requestBody.put("road_details", false);

        // JSON 변환
        ObjectMapper mapper = new ObjectMapper();
        String requestBodyJson = mapper.writeValueAsString(requestBody);
        log.info("requestBodyJson: " + requestBodyJson);

        // 카카오 API에 POST 요청 보내기
        String response = WebClient.builder()
                .build()
                .post()
                .uri("https://apis-navi.kakaomobility.com/v1/waypoints/directions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "KakaoAK " + KakaoApiKey)
                .bodyValue(requestBodyJson)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return response;
    }

    private Map<String, Object> createLocationMap(LocationDetail locationDetail) {
        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("name", locationDetail.getTitle());
        locationMap.put("x", locationDetail.getMapX());
        locationMap.put("y", locationDetail.getMapY());
        return locationMap;
    }

    @Transactional
    public boolean getLocationVertex(String tid, String tlid) throws JsonProcessingException {

        // tid, tlid에 해당하는 Location 검색
        Optional<Location> locationOpt = locationRepository.findByTidAndTlid(tid, tlid);

        if (!locationOpt.isPresent()) {
            throw new RuntimeException("Location not found for tid: " + tid + " and tlid: " + tlid);
        }

        Location location = locationOpt.get();

        // Location에 해당하는 Vertex가 있는지 확인
        List<Vertex> existingVertices = vertexRepository.findByLocation(location);

        // Vertex가 존재하면 더 이상 작업하지 않음
        if (!existingVertices.isEmpty()) {
            return true; // 이미 Vertex가 존재하므로 추가 작업 불필요
        }

        // Vertex가 비어 있으면 카카오 API 호출
        String json = sendLocationToKakao(tid, tlid);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(json);
            JsonNode routesNode = rootNode.path("routes");

            // 중복된 x, y 값을 저장하지 않기 위한 Set
            Set<String> uniqueVertices = new HashSet<>();

            if (routesNode.isArray()) {
                for (JsonNode route : routesNode) {
                    JsonNode sectionsNode = route.path("sections");
                    if (sectionsNode.isArray()) {
                        for (JsonNode section : sectionsNode) {
                            JsonNode roadsNode = section.path("roads");
                            if (roadsNode.isArray()) {
                                for (JsonNode road : roadsNode) {
                                    JsonNode vertexesNode = road.path("vertexes");

                                    // vertexes 안에 있는 x, y 좌표를 순회하면서 중복을 체크한 후 저장
                                    for (int i = 0; i < vertexesNode.size(); i += 2) {
                                        double x = vertexesNode.get(i).asDouble();
                                        double y = vertexesNode.get(i + 1).asDouble();

                                        String vertexKey = x + "," + y;  // 좌표를 String으로 변환하여 중복 체크

                                        // 중복된 좌표가 없을 경우에만 저장
                                        if (!uniqueVertices.contains(vertexKey)) {
                                            uniqueVertices.add(vertexKey);

                                            // Vertex 객체 생성 및 저장
                                            Vertex vertex = new Vertex();
                                            vertex.setLocation(location);
                                            vertex.setX(x);
                                            vertex.setY(y);
                                            vertexRepository.save(vertex);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private LocalDateTime StringToLocalDateTime(String time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return LocalDateTime.parse(time, formatter);
    }
}
