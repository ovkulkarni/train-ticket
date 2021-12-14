package ticketinfo.service;

import edu.fudan.common.util.Response;
import edu.fudan.common.util.ConsistencyCheckedCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ticketinfo.entity.Travel;

import java.util.function.BiFunction;

/**
 * Created by Chenjie Xu on 2017/6/6.
 */
@Service
public class TicketInfoServiceImpl implements TicketInfoService {

    @Autowired
    private RestTemplate restTemplate;

    private BiFunction<Travel, HttpHeaders, Response> travelQuery = (info, headers) -> {
        HttpEntity requestEntity = new HttpEntity(info, headers);
        ResponseEntity<Response> re = restTemplate.exchange(
                "http://ts-basic-service:15680/api/v1/basicservice/basic/travel",
                HttpMethod.POST,
                requestEntity,
                Response.class);
        return re.getBody();
    };

    private BiFunction<String, HttpHeaders, Response> stationQuery = (name, headers) -> {
        HttpEntity requestEntity = new HttpEntity(headers);
        ResponseEntity<Response> re = restTemplate.exchange(
                "http://ts-basic-service:15680/api/v1/basicservice/basic/" + name,
                HttpMethod.GET,
                requestEntity,
                Response.class);

        return re.getBody();
    };

    private ConsistencyCheckedCache<Travel, HttpHeaders, Response> travelCache = new ConsistencyCheckedCache<Travel, HttpHeaders, Response>(
            "travelCache", 100, travelQuery);

    private ConsistencyCheckedCache<String, HttpHeaders, Response> stationCache = new ConsistencyCheckedCache<String, HttpHeaders, Response>(
            "stationCache", 100, stationQuery);

    @Override
    public Response queryForTravel(Travel info, HttpHeaders headers) {
        return travelCache.getOrInsert(info, headers);
    }

    @Override
    public Response queryForStationId(String name, HttpHeaders headers) {
        return stationCache.getOrInsert(name, headers);
    }
}
