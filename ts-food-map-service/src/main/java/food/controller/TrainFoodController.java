package food.controller;

import food.service.FoodMapService;
import food.entity.TrainFood;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/api/v1/foodmapservice")
public class TrainFoodController {

    @Autowired
    FoodMapService foodMapService;

    private static final Logger LOGGER = LoggerFactory.getLogger(TrainFoodController.class);

    @GetMapping(path = "/trainfoods/welcome")
    public String home() {
        return "Welcome to [ Train Food Service ] !";
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/trainfoods")
    public HttpEntity getAllTrainFood(@RequestHeader HttpHeaders headers) {
        TrainFoodController.LOGGER.info("[Food Map Service][Get All TrainFoods]");
        return ok(foodMapService.listTrainFood(headers));
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/trainfoods/{tripId}")
    public HttpEntity getTrainFoodOfTrip(@PathVariable String tripId, @RequestHeader HttpHeaders headers) {
        TrainFoodController.LOGGER.info("[Food Map Service][Get TrainFoods By TripId]");
        return ok(foodMapService.listTrainFoodByTripId(tripId, headers));
    }

    @CrossOrigin(origins = "*")
    @PostMapping(path = "/trainfoods/create")
    public HttpEntity create(@RequestBody TrainFood tf, @RequestHeader HttpHeaders headers) {
        TrainFoodController.LOGGER.info("[Food Map Service][Create TrainFood] Create TrainFood form {}", tf);
        return ok(foodMapService.createTrainFood(tf, headers));
    }

    @CrossOrigin(origins = "*")
    @DeleteMapping(path = "/trainfoods/delete/{trainFoodId}")
    public HttpEntity delete(@PathVariable String trainFoodId, @RequestHeader HttpHeaders headers) {
        TrainFoodController.LOGGER.info("[Food Map Service][Delete TrainFood] TrainStore Id: {}", trainFoodId);
        // Order
        return ok(foodMapService.deleteTrainFood(trainFoodId, headers));
    }
}
