package fdse.microservice.service;

import edu.fudan.common.util.JsonUtils;
import edu.fudan.common.util.Response;
import edu.fudan.common.util.ConsistencyCheckedCache;
import fdse.microservice.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.AbstractMap.SimpleImmutableEntry;

/**
 * @author fdse
 */
@Service
public class BasicServiceImpl implements BasicService {

    @Autowired
    private RestTemplate restTemplate;

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicServiceImpl.class);

    public BiFunction<String, HttpHeaders, Boolean> checkStationExists = (stationName, headers) -> {
        BasicServiceImpl.LOGGER.info("[Basic Information Service][Check Station Exists] Station Name: {}", stationName);
        HttpEntity requestEntity = new HttpEntity(headers);
        ResponseEntity<Response> re = restTemplate.exchange(
                "http://ts-station-service:12345/api/v1/stationservice/stations/id/" + stationName,
                HttpMethod.GET,
                requestEntity,
                Response.class);
        Response exist = re.getBody();

        return exist.getStatus() == 1;
    };

    public BiFunction<String, HttpHeaders, Response> stationIdQuery = (stationName, headers) -> {
        BasicServiceImpl.LOGGER.info("[Basic Information Service][Query For Station Id] Station Id: {}", stationName);
        HttpEntity requestEntity = new HttpEntity(headers);
        ResponseEntity<Response> re = restTemplate.exchange(
                "http://ts-station-service:12345/api/v1/stationservice/stations/id/" + stationName,
                HttpMethod.GET,
                requestEntity,
                Response.class);
        return re.getBody();
    };

    public BiFunction<String, HttpHeaders, TrainType> queryTrainType = (trainTypeId, headers) -> {
        BasicServiceImpl.LOGGER.info("[Basic Information Service][Query Train Type] Train Type: {}", trainTypeId);
        HttpEntity requestEntity = new HttpEntity(headers);
        ResponseEntity<Response> re = restTemplate.exchange(
                "http://ts-train-service:14567/api/v1/trainservice/trains/" + trainTypeId,
                HttpMethod.GET,
                requestEntity,
                Response.class);
        Response response = re.getBody();

        return JsonUtils.conveterObject(response.getData(), TrainType.class);
    };

    private BiFunction<String, HttpHeaders, Route> getRouteByRouteId = (routeId, headers) -> {
        BasicServiceImpl.LOGGER.info("[Basic Information Service][Get Route By Id] Route ID：{}", routeId);
        HttpEntity requestEntity = new HttpEntity(headers);
        ResponseEntity<Response> re = restTemplate.exchange(
                "http://ts-route-service:11178/api/v1/routeservice/routes/" + routeId,
                HttpMethod.GET,
                requestEntity,
                Response.class);
        Response result = re.getBody();
        if (result.getStatus() == 0) {
            BasicServiceImpl.LOGGER.info("[Basic Information Service][Get Route By Id] Fail. {}", result.getMsg());
            return null;
        } else {
            BasicServiceImpl.LOGGER.info("[Basic Information Service][Get Route By Id] Success.");
            return JsonUtils.conveterObject(result.getData(), Route.class);
        }
    };

    private BiFunction<SimpleImmutableEntry<String, String>, HttpHeaders, PriceConfig> queryPriceConfigByRouteIdAndTrainType = (
            pair,
            headers) -> {
        BasicServiceImpl.LOGGER.info("[Basic Information Service][Query For Price Config] RouteId: {} ,TrainType: {}",
                pair.getKey(), pair.getValue());
        HttpEntity requestEntity = new HttpEntity(null, headers);
        ResponseEntity<Response> re = restTemplate.exchange(
                "http://ts-price-service:16579/api/v1/priceservice/prices/" + pair.getKey() + "/" + pair.getValue(),
                HttpMethod.GET,
                requestEntity,
                Response.class);
        Response result = re.getBody();

        BasicServiceImpl.LOGGER.info("Response Resutl to String {}", result.toString());
        return JsonUtils.conveterObject(result.getData(), PriceConfig.class);
    };

    private final ConsistencyCheckedCache<String, HttpHeaders, Boolean> stationExistsCache = new ConsistencyCheckedCache<String, HttpHeaders, Boolean>(
            "stationExistsCache", 100, checkStationExists);

    private final ConsistencyCheckedCache<String, HttpHeaders, Response> stationIdCache = new ConsistencyCheckedCache<String, HttpHeaders, Response>(
            "stationIdCache", 100, stationIdQuery);

    private final ConsistencyCheckedCache<String, HttpHeaders, TrainType> trainTypeCache = new ConsistencyCheckedCache<String, HttpHeaders, TrainType>(
            "trainTypeCache", 100, queryTrainType);

    private final ConsistencyCheckedCache<String, HttpHeaders, Route> routeIdCache = new ConsistencyCheckedCache<String, HttpHeaders, Route>(
            "routeIdCache", 100, getRouteByRouteId);

    private final ConsistencyCheckedCache<SimpleImmutableEntry<String, String>, HttpHeaders, PriceConfig> priceCache = new ConsistencyCheckedCache<SimpleImmutableEntry<String, String>, HttpHeaders, PriceConfig>(
            "priceCache", 100, queryPriceConfigByRouteIdAndTrainType);

    @Override
    public Response queryForTravel(Travel info, HttpHeaders headers) {

        Response response = new Response<>();
        TravelResult result = new TravelResult();
        result.setStatus(true);
        response.setStatus(1);
        response.setMsg("Success");
        boolean startingPlaceExist = stationExistsCache.getOrInsert(info.getStartingPlace(), headers);
        boolean endPlaceExist = stationExistsCache.getOrInsert(info.getEndPlace(), headers);
        if (!startingPlaceExist || !endPlaceExist) {
            result.setStatus(false);
            response.setStatus(0);
            response.setMsg("Start place or end place not exist!");
        }

        TrainType trainType = trainTypeCache.getOrInsert(info.getTrip().getTrainTypeId(), headers);
        if (trainType == null) {
            BasicServiceImpl.LOGGER.info("traintype doesn't exist");
            result.setStatus(false);
            response.setStatus(0);
            response.setMsg("Train type doesn't exist");
        } else {
            result.setTrainType(trainType);
        }

        String routeId = info.getTrip().getRouteId();
        String trainTypeString = "";
        if (trainType != null) {
            trainTypeString = trainType.getId();
        }
        Route route = routeIdCache.getOrInsert(routeId, headers);
        PriceConfig priceConfig = priceCache
                .getOrInsert(new SimpleImmutableEntry<String, String>(routeId, trainTypeString), headers);

        String startingPlaceId = (String) queryForStationId(info.getStartingPlace(), headers).getData();
        String endPlaceId = (String) queryForStationId(info.getEndPlace(), headers).getData();

        LOGGER.info("startingPlaceId : " + startingPlaceId + "endPlaceId : " + endPlaceId);

        int indexStart = 0;
        int indexEnd = 0;
        if (route != null) {
            indexStart = route.getStations().indexOf(startingPlaceId);
            indexEnd = route.getStations().indexOf(endPlaceId);
        }

        LOGGER.info("indexStart : " + indexStart + " __ " + "indexEnd : " + indexEnd);
        if (route != null) {
            LOGGER.info("route.getDistances().size : " + route.getDistances().size());
        }
        HashMap<String, String> prices = new HashMap<>();
        try {
            int distance = 0;
            if (route != null) {
                distance = route.getDistances().get(indexEnd) - route.getDistances().get(indexStart);
            }

            /**
             * We need the price Rate and distance (starting station).
             */
            double priceForEconomyClass = distance * priceConfig.getBasicPriceRate();
            double priceForConfortClass = distance * priceConfig.getFirstClassPriceRate();
            prices.put("economyClass", "" + priceForEconomyClass);
            prices.put("confortClass", "" + priceForConfortClass);
        } catch (Exception e) {
            prices.put("economyClass", "95.0");
            prices.put("confortClass", "120.0");
        }
        result.setPrices(prices);
        result.setPercent(1.0);
        response.setData(result);
        return response;
    }

    @Override
    public Response queryForStationId(String stationName, HttpHeaders headers) {
        return stationIdCache.getOrInsert(stationName, headers);
    }

}
